package com.example.standby.data.repository

import android.content.Context
import com.example.standby.data.models.CalendarEvent
import android.util.Log
import com.example.standby.data.storage.PreferencesManager
import com.example.standby.domain.utils.DateTimeUtils
import com.example.standby.util.Constants
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.Scope
import okhttp3.ConnectionPool
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.*
import java.util.concurrent.TimeUnit

class CalendarRepository(
    private val context: Context,
    private val preferencesManager: PreferencesManager
) {
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
        .build()
    private val calendarScope = Scope(Constants.CALENDAR_SCOPE)
    
    fun getLastSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }
    
    suspend fun fetchAndSaveNextEvent(): CalendarEvent? {
        val account = getLastSignedInAccount() ?: return null
        val token = GoogleAuthUtil.getToken(
            context,
            account.email ?: "",
            "oauth2:${calendarScope.scopeUri}"
        )
        
        val event = fetchNextEventFromApi(token)
        preferencesManager.saveEvent(event)
        return event
    }
    
    fun getCachedEvent(): CalendarEvent? {
        return preferencesManager.loadEvent()?.takeIf { it.isInFuture() }
    }
    
    fun clearEvent() {
        preferencesManager.clearEvent()
    }
    
    private fun fetchNextEventFromApi(accessToken: String): CalendarEvent? {
        // Try today first
        val todayUrl = buildCalendarUrl(
            Calendar.getInstance().timeInMillis,
            DateTimeUtils.getTodayEnd().timeInMillis
        )
        
        val todayEvent = executeCalendarRequest(accessToken, todayUrl)
        if (todayEvent != null) return todayEvent
        
        // Fallback to tomorrow
        val tomorrowUrl = buildCalendarUrl(
            DateTimeUtils.getTomorrowStart().timeInMillis,
            DateTimeUtils.getTomorrowEnd().timeInMillis
        )
        
        return executeCalendarRequest(accessToken, tomorrowUrl)
    }
    
    private fun buildCalendarUrl(timeMin: Long, timeMax: Long): String {
        return HttpUrl.Builder()
            .scheme("https")
            .host(Constants.CALENDAR_API_BASE)
            .addPathSegment("calendar")
            .addPathSegment("v3")
            .addPathSegment("calendars")
            .addPathSegment("primary")
            .addPathSegment("events")
            .addQueryParameter("singleEvents", "true")
            .addQueryParameter("orderBy", "startTime")
            .addQueryParameter("maxResults", "1")
            .addQueryParameter("timeMin", DateTimeUtils.toISO8601(timeMin))
            .addQueryParameter("timeMax", DateTimeUtils.toISO8601(timeMax))
            .build()
            .toString()
    }
    
    private fun executeCalendarRequest(accessToken: String, url: String): CalendarEvent? {
        val req = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .build()
            
        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val body = resp.body?.string() ?: return null
            val json = JSONObject(body)
            val items = json.optJSONArray("items") ?: return null
            if (items.length() == 0) return null
            
            val first = items.getJSONObject(0)
            val start = first.optJSONObject("start") ?: return null
            val dateTime = start.optString("dateTime", null)
            val date = start.optString("date", null)
            val timeZoneId = start.optString("timeZone", null)
            
            val millis = when {
                // RFC3339 datetime with or without explicit offset
                dateTime != null -> DateTimeUtils.parseRFC3339(dateTime, timeZoneId)
                // All-day event: date-only; interpret as midnight in event/calendar TZ
                date != null -> DateTimeUtils.parseAllDayStartMillis(date, timeZoneId)
                else -> null
            } ?: return null
            
            val title = first.optString("summary", "Untitled Event")
            if (Constants.ENABLE_CALENDAR_LOGS) {
                try {
                    val end = first.optJSONObject("end")
                    val endDateTime = end?.optString("dateTime", null)
                    val endDate = end?.optString("date", null)
                    Log.i(
                        "CalendarFetch",
                        "Event parsed -> title='${title}', start=${millis}, startSrc=${if (dateTime!=null) "dateTime" else if (date!=null) "date" else "unknown"}, tz=${timeZoneId ?: "n/a"}, endDateTime=${endDateTime ?: "n/a"}, endDate=${endDate ?: "n/a"}"
                    )
                } catch (_: Exception) {}
            }
            
            return CalendarEvent(millis, title)
        }
    }
}

