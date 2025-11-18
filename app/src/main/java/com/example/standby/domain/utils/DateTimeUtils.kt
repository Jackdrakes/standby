package com.example.standby.domain.utils

import java.text.SimpleDateFormat
import java.util.*

object DateTimeUtils {
    
    fun getCurrentTime(): String {
        val formatter = SimpleDateFormat("h:mm", Locale.getDefault())
        return formatter.format(Date())
    }
    
    fun getCurrentDate(): String {
        val formatter = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
        return formatter.format(Date())
    }
    
    fun formatCountdown(now: Long, target: Long): String {
        val remaining = (target - now).coerceAtLeast(0L)
        val totalSeconds = remaining / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return when {
            hours > 0 -> "in ${hours}h ${minutes}m ${seconds}s"
            minutes > 0 -> "in ${minutes}m ${seconds}s"
            else -> "in ${seconds}s"
        }
    }
    
    fun formatCountdownNoSeconds(now: Long, target: Long): String {
        val remaining = (target - now).coerceAtLeast(0L)
        val totalMinutesCeil = kotlin.math.ceil(remaining / 60_000.0).toLong()
        val hours = totalMinutesCeil / 60
        val minutes = totalMinutesCeil % 60
        return if (hours > 0) "in ${hours}h ${minutes}m" else "in ${minutes}m"
    }
    
    fun toISO8601(millis: Long): String {
        val tz = TimeZone.getTimeZone("UTC")
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = tz
        return sdf.format(Date(millis))
    }
    
    fun parseISO8601(text: String): Long? {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.US)
            sdf.parse(text)?.time
        } catch (e: Exception) {
            null
        }
    }
    
    // Robust RFC3339 parsing with optional milliseconds and explicit timezone support.
    // If the input does not include an offset (e.g., separate timeZone field from API),
    // we apply the provided timeZoneId or fall back to the device default.
    fun parseRFC3339(text: String, timeZoneId: String? = null): Long? {
        // Try multiple ISO-8601 timezone patterns to correctly parse offsets with and without colon
        // Examples handled: +05:30 (XXX), +0530 (XX), Z (XXX/XX/X)
        val patternsWithOffset = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXX",
            "yyyy-MM-dd'T'HH:mm:ssXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ssX"
        )
        val patternsWithoutOffset = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        val hasOffset = text.endsWith("Z", ignoreCase = true) ||
            Regex("[+-]\\d{2}:?\\d{2}$").containsMatchIn(text)
        
        // Try patterns that include an explicit offset first
        if (hasOffset) {
            for (pattern in patternsWithOffset) {
                try {
                    val sdf = SimpleDateFormat(pattern, Locale.US)
                    // When an explicit offset exists in text, SimpleDateFormat will honor it
                    return sdf.parse(text)?.time
                } catch (_: Exception) {}
            }
        }
        
        // Fall back to patterns without offset and apply the supplied or device timezone
        val tz: TimeZone = try {
            TimeZone.getTimeZone(timeZoneId ?: TimeZone.getDefault().id)
        } catch (_: Exception) {
            TimeZone.getDefault()
        }
        for (pattern in patternsWithoutOffset) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                sdf.timeZone = tz
                return sdf.parse(text)?.time
            } catch (_: Exception) {}
        }
        return null
    }
    
    // Parse an all-day event start (date-only) at local/calendar midnight.
    // Google Calendar provides date-only for all-day events; interpret midnight in the
    // calendar's timezone (if provided) or device default.
    fun parseAllDayStartMillis(date: String, timeZoneId: String? = null): Long? {
        return try {
            val tz: TimeZone = try {
                TimeZone.getTimeZone(timeZoneId ?: TimeZone.getDefault().id)
            } catch (_: Exception) {
                TimeZone.getDefault()
            }
            val cal = Calendar.getInstance(tz)
            val dateOnly = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = tz }.parse(date)
            if (dateOnly != null) {
                cal.time = dateOnly
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            } else null
        } catch (_: Exception) {
            null
        }
    }
    
    fun Calendar.setToDayBoundary(hour: Int, minute: Int = 0, second: Int = 0, millisecond: Int = 0): Calendar = apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, second)
        set(Calendar.MILLISECOND, millisecond)
    }
    
    fun getTodayEnd(): Calendar {
        return Calendar.getInstance().setToDayBoundary(23, 59, 59, 999)
    }
    
    fun getTomorrowStart(): Calendar {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 1)
            setToDayBoundary(0, 0, 0, 0)
        }
    }
    
    fun getTomorrowEnd(): Calendar {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 1)
            setToDayBoundary(23, 59, 59, 999)
        }
    }
}

