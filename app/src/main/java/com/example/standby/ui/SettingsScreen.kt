package com.example.standby.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.standby.data.repository.CalendarRepository
import com.example.standby.data.storage.PreferencesManager
import com.example.standby.util.Constants
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import java.util.Date

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onSignInChanged: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { CalendarRepository(context, PreferencesManager(context)) }
    
    var accountEmail by remember { mutableStateOf<String?>(null) }
    var isFetching by remember { mutableStateOf(false) }
    var fetchStatus by remember { mutableStateOf<String?>(null) }
    
    val scope = remember { Scope(Constants.CALENDAR_SCOPE) }
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(scope)
            .build()
    }

    LaunchedEffect(Unit) {
        accountEmail = repository.getLastSignedInAccount()?.email
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Settings",
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Text(
                text = accountEmail ?: "Not signed in",
                fontSize = 16.sp,
                color = Color.Gray
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    val client = GoogleSignIn.getClient(context, gso)
                    context.startActivity(client.signInIntent)
                    // Note: Sign-in happens in separate activity, check on resume
                }) {
                    Text("Sign in")
                }
                Button(onClick = {
                    GoogleSignIn.getClient(
                        context,
                        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                    ).signOut()
                    accountEmail = null
                    repository.clearEvent()
                    onSignInChanged()
                }) {
                    Text("Sign out")
                }
            }
            // Manual fetch for debugging/verification
            val coroutineScope = rememberCoroutineScope()
            Button(onClick = {
                if (!isFetching) {
                    coroutineScope.launch {
                        isFetching = true
                        fetchStatus = null
                        try {
                            val event = withContext(Dispatchers.IO) { repository.fetchAndSaveNextEvent() }
                            if (event != null) {
                                val msg = "Fetched: ${event.title} at ${Date(event.startTimeMillis)}"
                                fetchStatus = msg
                                if (Constants.ENABLE_CALENDAR_LOGS) Log.i("CalendarFetch", msg)
                            } else {
                                fetchStatus = "No upcoming events"
                                if (Constants.ENABLE_CALENDAR_LOGS) Log.i("CalendarFetch", "No upcoming events")
                            }
                        } catch (e: Exception) {
                            val err = "Fetch failed: ${e.message}"
                            fetchStatus = err
                            if (Constants.ENABLE_CALENDAR_LOGS) Log.e("CalendarFetch", err, e)
                        } finally {
                            isFetching = false
                        }
                    }
                }
            }) {
                Text(if (isFetching) "Fetching..." else "Fetch next event")
            }
            if (fetchStatus != null) {
                Text(text = fetchStatus!!, fontSize = 14.sp, color = Color.Gray)
            }
            
            // Check for sign-in changes when resuming
            LaunchedEffect(Unit) {
                while (true) {
                    delay(500) // Check every 500ms
                    val currentAccount = repository.getLastSignedInAccount()?.email
                    if (currentAccount != accountEmail) {
                        accountEmail = currentAccount
                        if (currentAccount != null) {
                            onSignInChanged()
                        }
                    }
                }
            }
            
            Button(onClick = onBack) {
                Text(text = "Back")
            }
        }
    }
}

