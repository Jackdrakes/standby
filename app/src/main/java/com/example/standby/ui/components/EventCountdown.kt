package com.example.standby.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.standby.data.models.CalendarEvent
import com.example.standby.domain.utils.DateTimeUtils

@Composable
fun EventCountdown(
    event: CalendarEvent?,
    nowMillis: Long,
    modifier: Modifier = Modifier
) {
    if (event != null) {
        val countdownText = DateTimeUtils.formatCountdownNoSeconds(nowMillis, event.startTimeMillis)

        Box(modifier = modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Vertical bar separator
                Text(
                    text = "|",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.Gray.copy(alpha = 0.6f),
                    modifier = Modifier.width(8.dp)
                )
                // Countdown text
                Text(
                    text = countdownText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

