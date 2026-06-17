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
import androidx.compose.material.icons.filled.Bolt
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
            val listState = rememberScalingLazyListState()
            
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                item {
                    ListHeader {
                        Text(
                            text = "Logue Control",
                            style = MaterialTheme.typography.title3,
                            color = MaterialTheme.colors.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Battery Telemetry Redesigned
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(110.dp)
                        ) {
                            val batteryProgress = if (batteryPercentage in 0..100) batteryPercentage / 100f else 0f
                            val batteryColor = when {
                                batteryPercentage > 70 -> Color(0xFFa8d48b)
                                batteryPercentage > 30 -> Color(0xFFa0cfcf)
                                else -> Color(0xFFffb4ab)
                            }
                            CircularProgressIndicator(
                                progress = batteryProgress,
                                modifier = Modifier.fillMaxSize(),
                                strokeWidth = 6.dp,
                                indicatorColor = batteryColor,
                                trackColor = MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
                            )
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isPluggedIn) {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = "Plugged In",
                                            tint = Color(0xFFa8d48b),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    val batteryStr = if (batteryPercentage in 0..100) "$batteryPercentage%" else "--%"
                                    Text(
                                        text = batteryStr,
                                        style = MaterialTheme.typography.title2,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                val rangeStr = if (range >= 0) "$range mi" else "-- mi"
                                Text(
                                    text = rangeStr,
                                    style = MaterialTheme.typography.body2,
                                    color = MaterialTheme.colors.onSurfaceVariant
                                )
                            }
                            
                            if (batteryPercentage in 0..100) {
                                val angle = (targetLimit / 100f) * 360f
                                val radians = Math.toRadians(angle.toDouble() - 90)
                                val radius = 52 // (110 - 6) / 2
                                val x = (radius * cos(radians)).toFloat()
                                val y = (radius * sin(radians)).toFloat()
                                
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .offset(x.dp, y.dp)
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
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
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
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Chip(
                            onClick = { onSendCommand("/command/lock") },
                            label = { Text("Lock") },
                            modifier = Modifier.weight(1f).padding(end = 4.dp),
                            colors = ChipDefaults.primaryChipColors()
                        )
                        Chip(
                            onClick = { onSendCommand("/command/unlock") },
                            label = { Text("Unlock") },
                            modifier = Modifier.weight(1f).padding(start = 4.dp),
                            colors = ChipDefaults.primaryChipColors()
                        )
                    }
                }

                // Remote Command: Climate Control
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Chip(
                            onClick = { onSendCommand("/command/climate_start") },
                            label = { Text("AC Start") },
                            modifier = Modifier.weight(1f).padding(end = 4.dp),
                            colors = ChipDefaults.secondaryChipColors()
                        )
                        Chip(
                            onClick = { onSendCommand("/command/climate_stop") },
                            label = { Text("AC Stop") },
                            modifier = Modifier.weight(1f).padding(start = 4.dp),
                            colors = ChipDefaults.secondaryChipColors()
                        )
                    }
                }

                // Remote Command: Lights & Horn
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Chip(
                            onClick = { onSendCommand("/command/lights") },
                            label = { Text("Lights") },
                            modifier = Modifier.weight(1f).padding(end = 4.dp),
                            colors = ChipDefaults.secondaryChipColors()
                        )
                        Chip(
                            onClick = { onSendCommand("/command/horn") },
                            label = { Text("Horn") },
                            modifier = Modifier.weight(1f).padding(start = 4.dp),
                            colors = ChipDefaults.secondaryChipColors()
                        )
                    }
                }
            }
        }
    }
}
