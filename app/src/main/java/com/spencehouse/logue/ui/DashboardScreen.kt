package com.spencehouse.logue.ui

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.spencehouse.logue.BuildConfig
import com.spencehouse.logue.R
import com.spencehouse.logue.di.ImageLoaderEntryPoint
import com.spencehouse.logue.ui.model.DashboardViewModel
import com.spencehouse.logue.ui.model.VehicleLocation
import dagger.hilt.android.EntryPointAccessors
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    onLogout: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState
    var showPinDialog by remember { mutableStateOf<Pair<String, (String) -> Unit>?>(null) }
    var showChargeDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val imageLoader = EntryPointAccessors.fromApplication(context, ImageLoaderEntryPoint::class.java).imageLoader()

    val executeOrShowPin = { actionName: String, onConfirm: (String) -> Unit ->
        if (uiState.savedPin?.isNotEmpty() == true) {
            onConfirm(uiState.savedPin)
        } else {
            showPinDialog = actionName to onConfirm
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { expanded = true }
                        ) {
                            val selectedVehicle = uiState.vehicles.find { it.vin == uiState.selectedVin }
                            Log.d("DashboardScreen", "Selected vehicle image URL: ${selectedVehicle?.asset34FrontPath}")
                            if (selectedVehicle?.asset34FrontPath != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(selectedVehicle.asset34FrontPath)
                                        .error(R.drawable.ic_launcher_foreground)
                                        .listener(onError = { _, result ->
                                            Log.e("DashboardScreen", "Coil error: ${result.throwable}")
                                        })
                                        .build(),
                                    contentDescription = "Vehicle Image",
                                    imageLoader = imageLoader,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Text(
                                text = uiState.vehicleName,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Select Vehicle",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            uiState.vehicles.forEach { vehicle ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (vehicle.asset34FrontPath != null) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(LocalContext.current)
                                                        .data(vehicle.asset34FrontPath)
                                                        .build(),
                                                    contentDescription = "Vehicle Image",
                                                    imageLoader = imageLoader,
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape),
                                                    contentScale = ContentScale.Crop,
                                                    placeholder = painterResource(id = R.drawable.ic_launcher_foreground),
                                                    onError = { error ->
                                                        Log.e("DashboardScreen", "Coil error: ${error.result.throwable}")
                                                    }
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                            }
                                            Text(
                                                vehicle.aliasName ?: "${vehicle.modelYear} ${vehicle.divisionName} ${vehicle.modelCode}",
                                                color = if (vehicle.vin == uiState.selectedVin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.onVehicleChange(vehicle.vin)
                                        expanded = false
                                    }
                                 )
                            }
                        }
                    }

                    Row {
                        IconButton(onClick = { viewModel.refreshData() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(onClick = { showSettingsDialog = true }) {
                            Icon(Icons.Default.Info, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = viewModel.isRefreshing,
            onRefresh = { viewModel.refreshData() },
            modifier = Modifier.padding(innerPadding)
        ) {
            if (!uiState.isEv) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Selected vehicle is not an EV",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Please select an electric vehicle to view detailed dashboard information.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Note: OnStar must also be activated for vehicle integration.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Status Row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                uiState.statusText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Last Updated: ${uiState.lastUpdated}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Vehicle Status Section
                    item {
                        VehicleStatusCard(
                            percentage = uiState.batteryPercentage ?: 0,
                            range = uiState.range ?: 0,
                            odometer = uiState.odometer ?: 0,
                            targetLimit = uiState.targetChargeLevel,
                            useKilometers = uiState.useKilometers,
                            chargeStatus = uiState.chargeStatus,
                            chargeVoltage = uiState.chargeVoltage,
                            chargeCompletionTime = uiState.chargeCompletionTime,
                            isPluggedIn = uiState.isPluggedIn,
                            onSettingsClick = { showChargeDialog = true }
                        )
                    }

                    // Remote Commands
                    item {
                        RemoteCommands(
                            isFlashing = uiState.isFlashing,
                            isHonking = uiState.isHonking,
                            onLock = { executeOrShowPin("Lock Doors") { pin -> viewModel.lockDoors(pin) } },
                            onUnlock = { executeOrShowPin("Unlock Doors") { pin -> viewModel.unlockDoors(pin) } },
                            onLights = {
                                val action = if (uiState.isFlashing) "Stop Lights" else "Flash Lights"
                                executeOrShowPin(action) { pin -> viewModel.toggleFlashLights(pin) }
                            },
                            onHorn = {
                                val action = if (uiState.isHonking) "Stop Horn" else "Sound Horn"
                                executeOrShowPin(action) { pin -> viewModel.toggleSoundHorn(pin) }
                            }
                        )
                    }

                    // Climate Section
                    item {
                        ClimateControl(
                            status = uiState.climateStatus,
                            useCelsius = uiState.useCelsius,
                            onStart = { temp ->
                                executeOrShowPin("Start Climate") { pin -> viewModel.startClimate(pin, temp) }
                            },
                            onStop = {
                                executeOrShowPin("Stop Climate") { pin -> viewModel.stopClimate(pin) }
                            }
                        )
                    }

                    // Vehicle Location
                    item {
                        VehicleLocationCard(
                            vehicleLocation = uiState.vehicleLocation,
                            vehicleLocationError = uiState.vehicleLocationError,
                            onFindVehicle = {
                                executeOrShowPin("Find Vehicle") { pin ->
                                    viewModel.requestVehicleLocation(
                                        pin
                                    )
                                }
                            }
                        )
                    }

                    // Tire Pressure
                    item {
                        TirePressureSection(uiState.tirePressures, uiState.useKpa)
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // Dialogs
    showPinDialog?.let { (name, onConfirm) ->
        PinDialog(
            title = "Confirm $name",
            savedPin = uiState.savedPin,
            onDismiss = { showPinDialog = null },
            onConfirm = { pin, savePin ->
                viewModel.setPin(if (savePin) pin else null)
                onConfirm(pin)
                showPinDialog = null
            }
        )
    }

    if (showChargeDialog) {
        ChargeLimitDialog(
            currentLimit = uiState.targetChargeLevel,
            onDismiss = { showChargeDialog = false },
            onConfirm = { limit ->
                viewModel.setTargetChargeLevel(limit)
                showChargeDialog = false
            }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            useCelsius = uiState.useCelsius,
            useKilometers = uiState.useKilometers,
            useKpa = uiState.useKpa,
            isPinSaved = !uiState.savedPin.isNullOrEmpty(),
            onUseCelsiusChange = { viewModel.toggleCelsius(it) },
            onUseKilometersChange = { viewModel.toggleKilometers(it) },
            onUseKpaChange = { viewModel.toggleKpa(it) },
            onDeletePin = { viewModel.setPin(null) },
            onLogout = {
                viewModel.logout()
                onLogout()
            },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VehicleStatusCard(
    percentage: Int,
    range: Int,
    odometer: Int,
    targetLimit: Int,
    useKilometers: Boolean,
    chargeStatus: String,
    chargeVoltage: String?,
    chargeCompletionTime: String?,
    isPluggedIn: Boolean,
    onSettingsClick: () -> Unit
) {
    val batteryColor = when {
        percentage > 70 -> MaterialTheme.colorScheme.primary
        percentage > 30 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    val chargeColor = if (isPluggedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { percentage / 100f },
                    modifier = Modifier.size(150.dp),
                    strokeWidth = 8.dp,
                    color = batteryColor,
                    trackColor = MaterialTheme.colorScheme.surfaceContainer,
                )
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )

                val angle = (targetLimit / 100f) * 360f
                val radians = Math.toRadians(angle.toDouble() - 90)
                val radius = 71
                val x = (radius * cos(radians)).toFloat()
                val y = (radius * sin(radians)).toFloat()

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .size(24.dp)
                        .offset(x.dp, y.dp)
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.onSurface,
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Bolt,
                        contentDescription = "Charge Target",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "${if (useKilometers) "${(range * 1.609).toInt()} km" else "$range miles"} range",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Odometer", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (useKilometers) "${(odometer * 1.609).toInt()} km" else "$odometer miles",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Target", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "$targetLimit%",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(onClick = onSettingsClick, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Settings, contentDescription = "Charge Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Power, contentDescription = null, tint = chargeColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text(chargeStatus, style = MaterialTheme.typography.bodyMedium, color = chargeColor, fontWeight = FontWeight.SemiBold)
                if (isPluggedIn) {
                    if (chargeVoltage != null) {
                        Text(" · $chargeVoltage", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (chargeStatus == "Charging" && chargeCompletionTime != null) {
                        Text(" · $chargeCompletionTime", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun RemoteCommands(
    isFlashing: Boolean,
    isHonking: Boolean,
    onLock: () -> Unit,
    onUnlock: () -> Unit,
    onLights: () -> Unit,
    onHorn: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("REMOTE COMMANDS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CommandButton("Lock", Icons.Default.Lock, Modifier.weight(1f)) { onLock() }
                CommandButton("Unlock", Icons.Default.LockOpen, Modifier.weight(1f)) { onUnlock() }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val lightsButtonText = if (isFlashing) "Stop Lights" else "Lights"
                val lightsButtonColors = if (isFlashing) {
                    ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
                } else {
                    ButtonDefaults.filledTonalButtonColors()
                }
                CommandButton(lightsButtonText, Icons.Default.Highlight, Modifier.weight(1f), colors = lightsButtonColors) { onLights() }

                val hornButtonText = if (isHonking) "Stop Horn" else "Horn"
                val hornButtonColors = if (isHonking) {
                    ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
                } else {
                    ButtonDefaults.filledTonalButtonColors()
                }
                CommandButton(hornButtonText, Icons.Default.Campaign, Modifier.weight(1f), colors = hornButtonColors) { onHorn() }
            }
        }
    }
}

@Composable
fun CommandButton(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 56.dp),
        shape = MaterialTheme.shapes.medium,
        colors = colors
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Icon(icon, contentDescription = null)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
fun ClimateControl(status: String, useCelsius: Boolean, onStart: (Int) -> Unit, onStop: () -> Unit) {
    var temp by remember(useCelsius) { mutableIntStateOf(if (useCelsius) 22 else 72) }
    val isOn = status != "OFF"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Thermostat, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("CLIMATE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { temp-- }) { Icon(Icons.Default.Remove, contentDescription = "Decrease temperature") }
                Text("$temp${if (useCelsius) "°C" else "°F"}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                IconButton(onClick = { temp++ }) { Icon(Icons.Default.Add, contentDescription = "Increase temperature") }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { if (isOn) onStop() else onStart(temp) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isOn) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.PowerSettingsNew, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isOn) "STOP CLIMATE" else "START CLIMATE")
            }
        }
    }
}

@Composable
fun VehicleLocationCard(
    vehicleLocation: VehicleLocation?,
    vehicleLocationError: String?,
    onFindVehicle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("VEHICLE LOCATION", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (vehicleLocation != null) {
                val vehicleLatLng = LatLng(vehicleLocation.latitude, vehicleLocation.longitude)
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(vehicleLatLng, 15f)
                }

                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(MaterialTheme.shapes.medium),
                    cameraPositionState = cameraPositionState
                ) {
                    Marker(
                        state = remember { MarkerState(position = vehicleLatLng) },
                        title = "Vehicle Location"
                    )
                }
            } else {
                if (vehicleLocationError != null) {
                    Text(
                        text = vehicleLocationError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    Button(
                        onClick = onFindVehicle,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("FIND VEHICLE")
                    }
                }
            }
        }
    }
}

@Composable
fun TirePressureSection(pressures: Map<String, Double?>, useKpa: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("TIRE PRESSURE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(10.dp))

            val labels = mapOf(
                "frontLeft" to "Front Left",
                "frontRight" to "Front Right",
                "rearLeft" to "Rear Left",
                "rearRight" to "Rear Right"
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("front" to listOf("frontLeft", "frontRight"), "rear" to listOf("rearLeft", "rearRight")).forEach { (_, positions) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        positions.forEach { pos ->
                            val pressure = pressures[pos]
                            OutlinedCard(
                                modifier = Modifier.weight(1f),
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(labels[pos] ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val displayPressure = if (pressure != null) {
                                        if (useKpa) "${pressure.toInt()} kPa" else "${(pressure * 0.145038).toInt()} PSI"
                                    } else "--"
                                    Text(displayPressure, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                 }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PinDialog(
    title: String,
    savedPin: String?,
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean) -> Unit
) {
    var pin by remember { mutableStateOf(savedPin ?: "") }
    var savePin by remember { mutableStateOf(savedPin != null) }
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                            pin = it
                        }
                    },
                    label = { Text("Enter PIN") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    keyboardActions = KeyboardActions(onDone = { onConfirm(pin, savePin) }),
                    singleLine = true
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = savePin,
                        onCheckedChange = { savePin = it }
                    )
                    Text("Save PIN", modifier = Modifier.padding(start = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pin, savePin) }) { Text("Execute") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun ChargeLimitDialog(currentLimit: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var limit by remember { mutableFloatStateOf(currentLimit.toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Charge Limit", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column {
                Text("${limit.toInt()}%", style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.CenterHorizontally))
                Slider(
                    value = limit,
                    onValueChange = { limit = it },
                    valueRange = 50f..100f,
                    steps = 4
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(limit.toInt()) }) { Text("Update") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SettingsDialog(
    useCelsius: Boolean,
    useKilometers: Boolean,
    useKpa: Boolean,
    isPinSaved: Boolean,
    onUseCelsiusChange: (Boolean) -> Unit,
    onUseKilometersChange: (Boolean) -> Unit,
    onUseKpaChange: (Boolean) -> Unit,
    onDeletePin: () -> Unit,
    onLogout: () -> Unit,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings & Info") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Use Celsius")
                    Switch(checked = useCelsius, onCheckedChange = onUseCelsiusChange)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Use Kilometers")
                    Switch(checked = useKilometers, onCheckedChange = onUseKilometersChange)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Use kPa for Tire Pressure")
                    Switch(checked = useKpa, onCheckedChange = onUseKpaChange)
                }

                if (isPinSaved) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    Button(
                        onClick = onDeletePin,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Saved PIN")
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                
                Text("Logue App", style = MaterialTheme.typography.titleMedium)
                Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Logue is an open-source alternative client for Honda and Acura connected vehicles.")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Please note that some commands and statuses may take up to 30 seconds to update, as the vehicle checks for new commands periodically.", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Developed by mcspencehouse",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { 
                        uriHandler.openUri("https://github.com/mcspencehouse/logue-app-kotlin")
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
