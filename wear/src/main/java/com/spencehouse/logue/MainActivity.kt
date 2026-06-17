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

    // Register a listener so the watch UI updates in real-time when the phone syncs data
    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            when (key) {
                "batteryPercentage" -> batteryPercentage = prefs.getInt(key, -1)
                "range" -> range = prefs.getInt(key, -1)
                "statusText" -> statusText = prefs.getString(key, "No Sync") ?: "No Sync"
                "timestamp" -> timestamp = prefs.getLong(key, 0)
            }
        }
        sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    MaterialTheme {
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

                // Battery Telemetry
                item {
                    val batteryStr = if (batteryPercentage in 0..100) "$batteryPercentage%" else "--%"
                    val rangeStr = if (range >= 0) "$range mi" else "-- mi"
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "$batteryStr ($rangeStr)",
                            style = MaterialTheme.typography.display3,
                            textAlign = TextAlign.Center
                        )
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
                                color = MaterialTheme.colors.onSurfaceVariant,
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
