package com.example.standby.data.models

data class CalendarEvent(
    val startTimeMillis: Long,
    val title: String
) {
    fun isInFuture(now: Long = System.currentTimeMillis()): Boolean {
        return startTimeMillis > now
    }
}

