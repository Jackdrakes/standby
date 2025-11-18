package com.example.standby.ui.components

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.standby.util.Constants

@Composable
fun StatusBar(
    batteryLevel: Int,
    isCharging: Boolean,
    btConnected: Boolean,
    eventTitle: String? = null,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.padding(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Event title with marquee (10% width, min 120dp)
            if (eventTitle != null) {
                val screenWidthDp = LocalConfiguration.current.screenWidthDp
                val titleWidthDp = maxOf((screenWidthDp * 0.10f).dp, 120.dp)
                
                Text(
                    text = eventTitle,
                    modifier = Modifier
                        .width(titleWidthDp)
                        .basicMarquee(iterations = Int.MAX_VALUE),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    textAlign = TextAlign.Start
                )
            }
            
            if (btConnected) {
                Icon(
                    imageVector = Icons.Filled.BluetoothConnected,
                    contentDescription = "Bluetooth Connected",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = "${batteryLevel}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (isCharging) Constants.BATTERY_CHARGING_COLOR else Color.White,
                textAlign = TextAlign.End
            )
        }
    }
}

