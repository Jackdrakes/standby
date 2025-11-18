package com.example.standby.ui

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.standby.data.repository.CalendarRepository
import com.example.standby.data.storage.PreferencesManager
import com.example.standby.domain.utils.DateTimeUtils
import com.example.standby.domain.utils.DeviceUtils
import com.example.standby.ui.components.EventCountdown
import com.example.standby.ui.components.StatusBar
import com.example.standby.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun StandbyClockScreen() {
    val context = LocalContext.current
    val repository = remember { CalendarRepository(context, PreferencesManager(context)) }

    var currentTime by remember { mutableStateOf(DateTimeUtils.getCurrentTime()) }
    var currentDate by remember { mutableStateOf(DateTimeUtils.getCurrentDate()) }
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var batteryLevel by remember { mutableStateOf(DeviceUtils.getBatteryLevel(context)) }
    var isCharging by remember { mutableStateOf(DeviceUtils.getBatteryChargingStatus(context)) }
    var btConnected by remember { mutableStateOf(false) }
    var nextEvent by remember { mutableStateOf(repository.getCachedEvent()) }
    
    // State for triggering immediate calendar fetch
    var triggerFetch by remember { mutableStateOf(0) }
    var signedInAccount by remember { mutableStateOf(repository.getLastSignedInAccount()) }
    
    // Adaptive brightness feature removed
    
    // Update time and date when minute changes
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = DateTimeUtils.getCurrentTime()
            currentDate = DateTimeUtils.getCurrentDate()
            val now = System.currentTimeMillis()
            val millisInMinute = 60_000L
            val millisUntilNextMinute = millisInMinute - (now % millisInMinute)
            delay(millisUntilNextMinute)
        }
    }

    // Update event countdown every second for smooth transitions
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            // Refresh from cache; if missing, try to fetch next after now
            val cached = repository.getCachedEvent()
            if (cached == null) {
                try {
                    repository.fetchAndSaveNextEvent()
                } catch (_: Exception) {}
            } else {
                nextEvent = cached
            }
            delay(1000L)
        }
    }

    // Smart calendar refresh with exponential backoff and network monitoring
    LaunchedEffect(signedInAccount, triggerFetch) {
        // Only run if signed in
        if (signedInAccount == null) return@LaunchedEffect
        
        var retryDelayMs = Constants.CALENDAR_REFRESH_INTERVAL_MS
        val minRetryDelay = 30_000L // 30 seconds minimum retry
        val maxRetryDelay = Constants.CALENDAR_REFRESH_INTERVAL_MS // 1 minute maximum
        
        var isFirstFetch = true
        while (true) {
            if (!isFirstFetch) delay(retryDelayMs) else isFirstFetch = false
            try {
                withContext(Dispatchers.IO) { repository.fetchAndSaveNextEvent() }
                nextEvent = repository.getCachedEvent()
                retryDelayMs = maxRetryDelay
            } catch (_: Exception) {
                retryDelayMs = if (retryDelayMs == maxRetryDelay) minRetryDelay else (retryDelayMs * 2).coerceAtMost(maxRetryDelay)
            }
        }
    }
    
    // Monitor network connectivity changes
    DisposableEffect(signedInAccount) {
        if (signedInAccount == null) {
            onDispose { }
        } else {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    // Network connected - trigger immediate fetch
                    triggerFetch++
                }
            }
            
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
            
            onDispose {
                connectivityManager.unregisterNetworkCallback(networkCallback)
            }
        }
    }

    // Runtime permission request for BLUETOOTH_CONNECT (Android 12+)
    val needsBtConnectPermission = remember { Build.VERSION.SDK_INT >= Build.VERSION_CODES.S }
    val hasBtConnectPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            else true
        )
    }
    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        btConnected = DeviceUtils.isBluetoothConnected(context)
    }
    LaunchedEffect(needsBtConnectPermission, hasBtConnectPermission) {
        if (needsBtConnectPermission) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                btPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }
    }

    // Initial Bluetooth and battery status
    LaunchedEffect(Unit) {
        btConnected = DeviceUtils.isBluetoothConnected(context)
    }

    // Listen to system broadcasts for real-time updates
    DisposableEffect(Unit) {
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_BATTERY_CHANGED -> {
                        batteryLevel = DeviceUtils.getBatteryLevel(context)
                        isCharging = DeviceUtils.getBatteryChargingStatus(context)
                    }
                    Intent.ACTION_POWER_CONNECTED -> {
                        isCharging = true
                        batteryLevel = DeviceUtils.getBatteryLevel(context)
                    }
                    Intent.ACTION_POWER_DISCONNECTED -> {
                        isCharging = false
                        batteryLevel = DeviceUtils.getBatteryLevel(context)
                    }
                }
                btConnected = DeviceUtils.isBluetoothConnected(context)
            }
        }
        context.registerReceiver(receiver, filter)
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    var showSettings by remember { mutableStateOf(false) }
    var screen by remember { mutableStateOf("clock") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(screen) {
                detectTapGestures(onTap = {
                    if (screen == "clock") {
                        showSettings = !showSettings
                    }
                })
            },
        contentAlignment = Alignment.Center
    ) {
        // Top-right: Battery, Bluetooth, and Event title
        StatusBar(
            batteryLevel = batteryLevel,
            isCharging = isCharging,
            btConnected = btConnected,
            eventTitle = nextEvent?.title,
            modifier = Modifier.align(Alignment.TopEnd)
        )

        // Center: Clock display
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = currentTime,
                fontSize = 120.sp,
                fontWeight = FontWeight.Light,
                color = Color.White,
                textAlign = TextAlign.Center,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = currentDate,
                fontSize = 32.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                letterSpacing = 2.sp
            )
        }

        // Top-left: Event countdown
        EventCountdown(
            event = nextEvent,
            nowMillis = nowMillis,
            modifier = Modifier.align(Alignment.TopStart)
        )

        // Settings navigation
        if (screen == "settings") {
            SettingsScreen(
                onBack = { screen = "clock" },
                onSignInChanged = {
                    signedInAccount = repository.getLastSignedInAccount()
                    if (signedInAccount != null) {
                        // Trigger immediate fetch after sign-in
                        triggerFetch++
                    }
                }
            )
        }

        // Settings FAB with auto-hide
        if (screen == "clock" && showSettings) {
            LaunchedEffect(showSettings) {
                delay(Constants.SETTINGS_AUTO_HIDE_MS)
                showSettings = false
            }
            FloatingActionButton(
                onClick = { screen = "settings" },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                containerColor = Constants.SETTINGS_FAB_COLOR
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = Color.White
                )
            }
        }
    }
}

