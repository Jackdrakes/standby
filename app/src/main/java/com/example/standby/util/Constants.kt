package com.example.standby.util

import androidx.compose.ui.graphics.Color

object Constants {
    // Time intervals
    const val TIME_UPDATE_INTERVAL_MS = 1000L // Deprecated - now using calculated minute-based updates
    const val CALENDAR_REFRESH_INTERVAL_MS = 60_000L // 1 minute - keeps events current and accurate
    // Debug flags
    const val ENABLE_CALENDAR_LOGS = false
    const val SETTINGS_AUTO_HIDE_MS = 2000L
    
    // API & Scopes
    const val CALENDAR_SCOPE = "https://www.googleapis.com/auth/calendar.readonly"
    const val CALENDAR_API_BASE = "www.googleapis.com"
    
    // Storage
    const val PREFS_NAME = "standby_prefs"
    const val PREF_KEY_EVENT_MILLIS = "next_event_millis"
    const val PREF_KEY_EVENT_TITLE = "next_event_title"
    
    // UI Colors
    val EVENT_COUNTDOWN_COLOR = Color(0xFFD3D3D3)
    val BATTERY_CHARGING_COLOR = Color(0xFFFFD700)
    val SETTINGS_FAB_COLOR = Color(0xFF222222)
    val COUNTDOWN_BG_COLOR = Color(0xFF121212)
}

