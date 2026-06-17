package com.spencehouse.logue.wear

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import com.google.android.gms.wearable.Wearable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.cos
import kotlin.math.sin
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val tag = "MainActivityWatch"
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        sharedPrefs = getSharedPreferences("wear_logue_prefs", Context.MODE_PRIVATE)

        setContent {
            WearApp(
                sharedPrefs = sharedPrefs,
                onSendCommand = { path -> sendCommandToPhone(path) }
            )
        }
    }

    private fun sendCommandToPhone(path: String) {
        Log.i(tag, "Attempting to send command: $path")
        Wearable.getNodeClient(this).connectedNodes
            .addOnSuccessListener { nodes ->
                if (nodes.isEmpty()) {
                    Log.w(tag, "No connected phone nodes found to send command")
                    return@addOnSuccessListener
                }
                val messageClient = Wearable.getMessageClient(this)
                for (node in nodes) {
                    messageClient.sendMessage(node.id, path, byteArrayOf())
                        .addOnSuccessListener {
                            Log.d(tag, "Command $path sent successfully to node: ${node.displayName}")
                        }
                        .addOnFailureListener { e ->
                            Log.e(tag, "Failed to send command $path to node: ${node.displayName}", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Failed to retrieve connected nodes", e)
            }
    }
}

@Composable
fun WearApp(
    sharedPrefs: SharedPreferences,
    onSendCommand: (String) -> Unit
) {
    // Reactive state observing changes in preferences
    var batteryPercentage by remember { mutableStateOf(sharedPrefs.getInt("batteryPercentage", -1)) }
    var range by remember { mutableStateOf(sharedPrefs.getInt("range", -1)) }
    var statusText by remember { mutableStateOf(sharedPrefs.getString("statusText", "No Sync Yet") ?: "No Sync") }
    var timestamp by remember { mutableStateOf(sharedPrefs.getLong("timestamp", 0)) }
    var targetLimit by remember { mutableStateOf(sharedPrefs.getInt("targetLimit", 80)) }
    var isPluggedIn by remember { mutableStateOf(sharedPrefs.getBoolean("isPluggedIn", false)) }
    var useCelsius by remember { mutableStateOf(sharedPrefs.getBoolean("useCelsius", false)) }
    var useKilometers by remember { mutableStateOf(sharedPrefs.getBoolean("useKilometers", false)) }

    // Selected AC target temp on the watch
    var acTemp by remember { mutableStateOf(if (useCelsius) 22 else 72) }

    LaunchedEffect(useCelsius) {
        acTemp = if (useCelsius) 22 else 72
    }

    // Request fresh telemetry data on launch
    LaunchedEffect(Unit) {
        onSendCommand("/request/telemetry")
    }

    // Register a listener so the watch UI updates in real-time when the phone syncs data
    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            when (key) {
                "batteryPercentage" -> batteryPercentage = prefs.getInt(key, -1)
                "range" -> range = prefs.getInt(key, -1)
                "statusText" -> statusText = prefs.getString(key, "No Sync") ?: "No Sync"
                "timestamp" -> timestamp = prefs.getLong(key, 0)
                "targetLimit" -> targetLimit = prefs.getInt(key, 80)
                "isPluggedIn" -> isPluggedIn = prefs.getBoolean(key, false)
                "useCelsius" -> useCelsius = prefs.getBoolean(key, false)
                "useKilometers" -> useKilometers = prefs.getBoolean(key, false)
            }
        }
        sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val wearColors = Colors(
        primary = Color(0xFFa8d48b),
        primaryVariant = Color(0xFF2c5116),
        secondary = Color(0xFFa0cfcf),
        secondaryVariant = Color(0xFF1e4e4e),
        background = Color(0xFF121410),
        surface = Color(0xFF1c1e1a),
        error = Color(0xFFffb4ab),
        onPrimary = Color(0xFF173800),
        onSecondary = Color(0xFF003737),
        onBackground = Color(0xFFe2e3dd),
        onSurface = Color(0xFFe2e3dd),
        onError = Color(0xFF690005)
    )

    MaterialTheme(colors = wearColors) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val hasData = batteryPercentage in 0..100
            val batteryProgress = if (hasData) batteryPercentage / 100f else 0f
            val batteryColor = when {
                batteryPercentage > 70 -> Color(0xFFa8d48b)
                batteryPercentage > 30 -> Color(0xFFa0cfcf)
                else -> Color(0xFFffb4ab)
            }

            BoxWithConstraints(
                modifier = Modifier.fillMaxSize().padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = batteryProgress,
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 6.dp,
                    indicatorColor = batteryColor,
                    trackColor = MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
                )

                if (hasData) {
                    val angle = (targetLimit / 100f) * 360f
                    val radians = Math.toRadians(angle.toDouble() - 90)
                    val radius = (maxWidth - 6.dp) / 2
                    val x = radius * cos(radians).toFloat()
                    val y = radius * sin(radians).toFloat()

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(16.dp)
                            .offset(x = x, y = y)
                            .background(MaterialTheme.colors.background, CircleShape)
                            .border(
                                1.5.dp,
                                MaterialTheme.colors.onSurface,
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Bolt,
                            contentDescription = "Charge Target",
                            tint = MaterialTheme.colors.onSurface,
                            modifier = Modifier
                                .size(12.dp)
                                .padding(1.dp)
                        )
                    }
                }
            }

            val listState = rememberScalingLazyListState()
            
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp)
            ) {

                // Battery Telemetry Redesigned
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isPluggedIn) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "Plugged In",
                                    tint = Color(0xFFa8d48b),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            val batteryStr = if (hasData) "$batteryPercentage%" else "--%"
                            Text(
                                text = batteryStr,
                                style = MaterialTheme.typography.display2,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        val rangeStr = if (range >= 0) {
                            if (useKilometers) "${(range * 1.609).toInt()} km range" else "$range mi range"
                        } else {
                            "-- range"
                        }
                        Text(
                            text = rangeStr,
                            style = MaterialTheme.typography.body1,
                            color = MaterialTheme.colors.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.caption2,
                            color = MaterialTheme.colors.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (timestamp > 0) {
                            val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
                            Text(
                                text = "Synced: $timeStr",
                                style = MaterialTheme.typography.caption3,
                                color = MaterialTheme.colors.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }

                // Remote Command: Lock / Unlock
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onSendCommand("/command/lock") },
                            colors = ButtonDefaults.primaryButtonColors(),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock Doors",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = { onSendCommand("/command/unlock") },
                            colors = ButtonDefaults.primaryButtonColors(),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = "Unlock Doors",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }

                // Remote Command: AC Temperature Adjuster
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "AC Temperature",
                            style = MaterialTheme.typography.caption1,
                            color = MaterialTheme.colors.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Button(
                                onClick = {
                                    val minTemp = if (useCelsius) 15 else 60
                                    if (acTemp > minTemp) acTemp--
                                },
                                colors = ButtonDefaults.secondaryButtonColors(),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Decrease Temp",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            
                            Text(
                                text = if (useCelsius) "$acTemp°C" else "$acTemp°F",
                                style = MaterialTheme.typography.body1,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            
                            Button(
                                onClick = {
                                    val maxTemp = if (useCelsius) 29 else 84
                                    if (acTemp < maxTemp) acTemp++
                                },
                                colors = ButtonDefaults.secondaryButtonColors(),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Increase Temp",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }

                // Remote Command: Climate Control
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onSendCommand("/command/climate_start/$acTemp") },
                            colors = ButtonDefaults.secondaryButtonColors(),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AcUnit,
                                contentDescription = "Start AC",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = { onSendCommand("/command/climate_stop") },
                            colors = ButtonDefaults.primaryButtonColors(
                                backgroundColor = MaterialTheme.colors.error,
                                contentColor = MaterialTheme.colors.onError
                            ),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop AC",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }

                // Remote Command: Lights & Horn
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onSendCommand("/command/lights") },
                            colors = ButtonDefaults.secondaryButtonColors(),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = "Flash Lights",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = { onSendCommand("/command/horn") },
                            colors = ButtonDefaults.secondaryButtonColors(),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Sound Horn",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
