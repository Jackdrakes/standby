package com.example.standby.data.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.standby.data.models.CalendarEvent
import com.example.standby.util.Constants

class PreferencesManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        Constants.PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    fun saveEvent(event: CalendarEvent?) {
        prefs.edit {
            if (event == null) {
                remove(Constants.PREF_KEY_EVENT_MILLIS)
                remove(Constants.PREF_KEY_EVENT_TITLE)
            } else {
                putLong(Constants.PREF_KEY_EVENT_MILLIS, event.startTimeMillis)
                putString(Constants.PREF_KEY_EVENT_TITLE, event.title)
            }
        }
    }
    
    fun loadEvent(): CalendarEvent? {
        if (!prefs.contains(Constants.PREF_KEY_EVENT_MILLIS)) {
            return null
        }
        
        val millis = prefs.getLong(Constants.PREF_KEY_EVENT_MILLIS, 0L)
        val title = prefs.getString(Constants.PREF_KEY_EVENT_TITLE, null) ?: return null
        
        return CalendarEvent(millis, title)
    }
    
    fun clearEvent() {
        prefs.edit {
            remove(Constants.PREF_KEY_EVENT_MILLIS)
            remove(Constants.PREF_KEY_EVENT_TITLE)
        }
    }
}

