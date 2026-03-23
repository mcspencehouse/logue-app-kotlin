package com.spencehouse.logue.ui.model

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spencehouse.logue.service.AuthService
import com.spencehouse.logue.service.VehicleService
import com.spencehouse.logue.service.mqtt.AwsMqttClient
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authService: AuthService,
    private val vehicleService: VehicleService
) : ViewModel() {

    private val tag = "DashboardViewModel"
    var uiState by mutableStateOf(DashboardUiState())
        private set

    private var mqttClient: AwsMqttClient? = null
    private var refreshJob: Job? = null
    private var carFinderPollingJob: Job? = null

    var isRefreshing by mutableStateOf(false)
        private set

    init {
        Log.d(tag, "Initializing DashboardViewModel")

        viewModelScope.launch {
            if (authService.vehicles.isEmpty()) {
                Log.d(tag, "Vehicles empty, attempting to fetch vehicles")
                val result = authService.fetchVehicles()
                if (result.isFailure) {
                    Log.e(tag, "Failed to fetch vehicles during initialization", result.exceptionOrNull())
                }
            }

            val mappedVehicles = authService.vehicles.map {
                VehicleUiModel(
                    vin = it.vin,
                    modelYear = it.modelYear,
                    divisionName = it.divisionName,
                    modelCode = it.modelCode,
                    aliasName = it.aliasName,
                    asset34FrontPath = it.asset34FrontPath
                )
            }
            Log.d(tag, "Mapped vehicles in init: $mappedVehicles")

            val isEv = checkIfEv(authService.getVehicleName())
            uiState = uiState.copy(
                vehicleName = authService.getVehicleName(),
                vehicles = mappedVehicles,
                selectedVin = authService.selectedVin,
                isEv = isEv,
                useCelsius = authService.sessionManager.useCelsius,
                useKilometers = authService.sessionManager.useKilometers,
                useKpa = authService.sessionManager.useKpa,
                savedPin = authService.sessionManager.pin
            )

            if (isEv) {
                connectMqtt()
            } else {
                updateStatus("Not an EV")
            }
        }
        startAutoRefresh()
    }

    private fun checkIfEv(name: String): Boolean {
        val evModels = listOf("ZDX", "PROLOGUE", "EV")
        return evModels.any { name.uppercase().contains(it) }
    }

    private fun connectMqtt() {
        val vin = authService.selectedVin
        if (vin == null || !uiState.isEv) return

        viewModelScope.launch {
            try {
                Log.d(tag, "Connecting MQTT for VIN: $vin")
                updateStatus("Authenticating MQTT...")
                val credsResult = vehicleService.getCigToken(vin)
                val creds = credsResult.getOrElse {
                    Log.e(tag, "Failed to get CIG token", it)
                    val errorMsg = it.message ?: ""
                    if (errorMsg.contains("Unable to resolve host")) {
                        updateStatus("Network Error. Check connection.")
                    } else if (errorMsg.contains("scope is invalid")) {
                        updateStatus("Not an EV")
                    } else {
                        updateStatus("Auth Error: $errorMsg")
                    }
                    return@launch
                }

                Log.d(tag, "CIG Token received, initializing AwsMqttClient")
                updateStatus("Connecting to AWS IoT...")
                mqttClient?.disconnect()
                mqttClient = AwsMqttClient(
                    vin = vin,
                    cigToken = creds.token,
                    cigSignature = creds.tokenSignature,
                    onMessageCallback = { topic, payload ->
                        Log.d(tag, "MQTT Message received on $topic")
                        onMqttMessage(topic, payload)
                    },
                    onConnected = {
                        Log.i(tag, "MQTT Connected successfully")
                        updateStatus("Connected")
                        refreshData()
                    },
                    onError = { error ->
                        Log.e(tag, "MQTT Client Error: $error")
                        updateStatus("Connection Error: $error")
                    }
                )
                mqttClient?.connect()
            } catch (e: Exception) {
                Log.e(tag, "connectMqtt exception", e)
                updateStatus("Connection Error: ${e.message}")
            }
        }
    }

    private fun onMqttMessage(topic: String, payload: String) {
        try {
            val json = JSONObject(payload)
            Log.v(tag, "Processing MQTT payload: $payload")
            if (topic.contains("DASHBOARD_ASYNC")) {
                updateDashboardUi(json)
            } else if (topic.contains("ENGINE_START_STOP_ASYNC")) {
                Log.d(tag, "Engine start/stop update received")
            } else if (topic.contains("CARFINDER_HORN_LIGHT_ASYNC")) {
                updateCarFinderUi(json)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error parsing MQTT message", e)
        }
    }

    private fun updateCarFinderUi(data: JSONObject) {
        val reported = data.optJSONObject("state")?.optJSONObject("reported") ?: return
        val rb = reported.optJSONObject("responseBody") ?: return

        Log.d(tag, "Updating UI with reported car finder data")
        val lightStatus = rb.optString("lightStatus", "OFF")
        val hornStatus = rb.optString("hornStatus", "OFF")

        uiState = uiState.copy(
            isFlashing = lightStatus == "ON",
            isHonking = hornStatus == "ON"
        )
    }

    private fun updateDashboardUi(data: JSONObject) {
        val reported = data.optJSONObject("state")?.optJSONObject("reported") ?: return
        val rb = reported.optJSONObject("responseBody") ?: return

        Log.d(tag, "Updating UI with reported dashboard data")
        val evStatus = rb.optJSONObject("evStatus")
        val odometerData = rb.optJSONObject("odometer")
        val tireStatus = rb.optJSONObject("tireStatus")
        val chargeMode = rb.optJSONObject("getChargeMode")
        val chargeTime = rb.optJSONObject("hvBatteryChargeCompleteTime")

        val battery = evStatus?.optInt("soc")
        val rangeVal = evStatus?.optInt("evRange")
        val chargeStatus = evStatus?.optString("chargeStatus")
        val plugStatus = evStatus?.optString("plugStatus")
        val chargeModeValue = evStatus?.optString("chargeMode")

        val targetLevel = chargeMode?.optJSONObject("generalAwayTargetChargeLevel")?.optInt("value") ?: 80

        val isPluggedIn = plugStatus?.lowercase() == "plugged" || chargeStatus?.lowercase() == "charging"

        val (mainStatus, voltage) = formatChargeStatus(chargeStatus, plugStatus, chargeModeValue)

        authService.sessionManager.cachedBatteryPercentage = battery ?: -1
        authService.sessionManager.cachedRange = rangeVal ?: -1
        authService.sessionManager.cachedChargeStatus = mainStatus
        authService.sessionManager.cachedIsPluggedIn = isPluggedIn
        authService.sessionManager.targetChargeLevel = targetLevel

        uiState = uiState.copy(
            batteryPercentage = battery,
            range = rangeVal,
            chargeStatus = mainStatus,
            chargeVoltage = voltage,
            chargeCompletionTime = formatTime(chargeTime),
            isPluggedIn = isPluggedIn,
            targetChargeLevel = targetLevel,
            odometer = odometerData?.optInt("value"),
            tirePressures = parseTires(tireStatus),
            lastUpdated = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date()),
            statusText = "Data Received"
        )
    }

    private fun formatTime(chargeTime: JSONObject?): String? {
        if (chargeTime == null) return null

        val day = chargeTime.optJSONObject("hvBatteryChargeCompleteDay")?.optString("value")
        val hourStr = chargeTime.optJSONObject("hvBatteryChargeCompleteHour")?.optString("value")
        val minuteStr = chargeTime.optJSONObject("hvBatteryChargeCompleteMinute")?.optString("value")

        if (day.isNullOrEmpty() || hourStr.isNullOrEmpty() || minuteStr.isNullOrEmpty()) {
            return null
        }

        val hour = hourStr.toIntOrNull()
        val minute = minuteStr.toIntOrNull()

        if (hour == null || minute == null) return null

        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_WEEK)

        val dayOfWeekMap = mapOf(
            "Sunday" to 1, "Monday" to 2, "Tuesday" to 3, "Wednesday" to 4,
            "Thursday" to 5, "Friday" to 6, "Saturday" to 7
        )

        val targetDay = dayOfWeekMap[day] ?: return null
        val daysToAdd = (targetDay - currentDay + 7) % 7

        calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)

        // If the calculated time is in the past (and it's the same day), assume it's for the next week
        if (daysToAdd == 0 && calendar.timeInMillis < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 7)
        }

        val dateFormat = SimpleDateFormat("EEE, h:mm a", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    private fun formatChargeStatus(chargeStatus: String?, plugStatus: String?, chargeMode: String?): Pair<String, String?> {
        val status = chargeStatus?.lowercase()
        val pStatus = plugStatus?.lowercase()

        val isPluggedIn = pStatus == "plugged" || status == "charging"

        if (!isPluggedIn) {
            return "Unplugged" to null
        }

        var mainStatus = "Plugged In"
        when (status) {
            "charging" -> mainStatus = "Charging"
            "complete" -> mainStatus = "Complete"
        }

        val chargeModeInt = chargeMode?.toIntOrNull()
        val voltage = if (chargeModeInt != null && chargeModeInt > 0) {
            "${chargeModeInt}V"
        } else {
            null
        }

        return mainStatus to voltage
    }

    private fun parseTires(tireStatus: JSONObject?): Map<String, Double?> {
        val tires = mutableMapOf<String, Double?>()
        val positions = listOf("frontLeft", "frontRight", "rearLeft", "rearRight")
        positions.forEach { pos ->
            tires[pos] = tireStatus?.optJSONObject(pos)?.optJSONObject("pressureData")?.optDouble("value")
        }
        return tires
    }

    fun refreshData(): Job {
        val vin = authService.selectedVin ?: return viewModelScope.launch {}

        if (!uiState.isEv) return viewModelScope.launch {}

        return viewModelScope.launch {
            isRefreshing = true
            Log.d(tag, "Refreshing data and checking connection for VIN: $vin")

            if (uiState.isEv) {
                // Reconnect if status indicates an error or disconnect
                if (uiState.statusText.contains("Error") || uiState.statusText.contains("lost")) {
                    connectMqtt()
                }

                updateStatus("Requesting update...")
                val result = vehicleService.requestDashboard(vin)
                result.onFailure {
                    Log.e(tag, "Manual dashboard request failed", it)
                    val errorMsg = it.message ?: ""
                    if (errorMsg.contains("Unable to resolve host")) {
                        updateStatus("Network Error. Check connection.")
                    } else if (errorMsg.contains("scope is invalid")) {
                        updateStatus("Not an EV")
                        uiState = uiState.copy(isEv = false)
                    } else {
                        updateStatus("Refresh failed: $errorMsg")
                    }
                }
            } else {
                updateStatus("Not an EV")
            }

            val climateResult = vehicleService.getClimateStatus(vin)
            climateResult.onSuccess {
                val status = it.jsonObject["climateStatus"]?.jsonPrimitive?.content ?: "OFF"
                Log.d(tag, "Climate status received: $status")
                authService.sessionManager.cachedClimateStatus = status.uppercase()
                val mappedVehicles = authService.vehicles.map { vehicle ->
                    VehicleUiModel(
                        vin = vehicle.vin,
                        modelYear = vehicle.modelYear,
                        divisionName = vehicle.divisionName,
                        modelCode = vehicle.modelCode,
                        aliasName = vehicle.aliasName,
                        asset34FrontPath = vehicle.asset34FrontPath
                    )
                }
                Log.d(tag, "Mapped vehicles in refreshData: $mappedVehicles")
                uiState = uiState.copy(
                    climateStatus = status.uppercase(),
                    vehicles = mappedVehicles
                )
            }.onFailure {
                Log.e(tag, "Climate status request failed", it)
                val errorMsg = it.message ?: ""
                if (errorMsg.contains("Unable to resolve host")) {
                    updateStatus("Network Error. Check connection.")
                }
            }

            isRefreshing = false
        }
    }

    private fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (isActive) {
                delay(60000)
                Log.d(tag, "Auto-refreshing data")
                refreshData()
            }
        }
    }

    fun toggleCelsius(value: Boolean) {
        authService.sessionManager.useCelsius = value
        uiState = uiState.copy(useCelsius = value)
    }

    fun toggleKilometers(value: Boolean) {
        authService.sessionManager.useKilometers = value
        uiState = uiState.copy(useKilometers = value)
    }

    fun toggleKpa(value: Boolean) {
        authService.sessionManager.useKpa = value
        uiState = uiState.copy(useKpa = value)
    }

    private fun updateStatus(text: String) {
        uiState = uiState.copy(statusText = text)
    }

    fun onVehicleChange(vin: String) {
        if (vin == authService.selectedVin) return

        Log.i(tag, "Changing vehicle to VIN: $vin")
        mqttClient?.disconnect()

        // This was the missing link - update persistent storage!
        authService.updateSelectedVin(vin)

        val mappedVehicles = authService.vehicles.map {
            VehicleUiModel(
                vin = it.vin,
                modelYear = it.modelYear,
                divisionName = it.divisionName,
                modelCode = it.modelCode,
                aliasName = it.aliasName,
                asset34FrontPath = it.asset34FrontPath
            )
        }
        Log.d(tag, "Mapped vehicles in onVehicleChange: $mappedVehicles")

        val isEv = checkIfEv(authService.getVehicleName())
        uiState = uiState.copy(
            selectedVin = vin,
            vehicleName = authService.getVehicleName(),
            isEv = isEv,
            batteryPercentage = null,
            range = null,
            statusText = if (isEv) "Switching vehicles..." else "Not an EV",
            vehicles = mappedVehicles
        )

        if (isEv) {
            connectMqtt()
        }
    }

    fun logout() {
        Log.i(tag, "User logged out from Dashboard")
        mqttClient?.disconnect()
        authService.logout()
    }

    fun setTargetChargeLevel(level: Int) {
        val vin = authService.selectedVin ?: return
        if (!uiState.isEv) return

        viewModelScope.launch {
            Log.d(tag, "Setting target charge level to $level%")
            vehicleService.setTargetChargeLevel(vin, level)
            refreshData()
        }
    }

    fun setPin(pin: String?) {
        authService.sessionManager.pin = pin
        uiState = uiState.copy(savedPin = pin)
    }

    private suspend fun sendCommand(name: String, action: suspend (String) -> Result<String?>, pin: String): Result<String?> {
        Log.i(tag, "Sending command: $name")
        updateStatus("Sending $name command...")
        val result = action(pin)
        result.onSuccess {
            Log.i(tag, "Command $name sent successfully")
            updateStatus("$name command sent!")
        }.onFailure {
            Log.e(tag, "Command $name failed", it)
            updateStatus("$name failed: ${it.message}")
        }
        return result
    }

    private fun sendCarFinderCommand(name: String, action: suspend (String) -> Result<String?>, pin: String, targetIsOff: Boolean = false) {
        viewModelScope.launch {
            val result = sendCommand(name, action, pin)
            result.onSuccess {
                startCarFinderPolling(targetIsOff)
            }
        }
    }

    private fun startCarFinderPolling(targetIsOff: Boolean) {
        carFinderPollingJob?.cancel()
        carFinderPollingJob = viewModelScope.launch {
            Log.d(tag, "Starting car finder polling for targetIsOff: $targetIsOff")
            for (i in 1..12) { // Increased polling attempts
                if (!isActive) return@launch
                updateStatus("Checking status... (attempt $i)")

                val conditionMet = if (targetIsOff) {
                    !uiState.isFlashing && !uiState.isHonking
                } else {
                    uiState.isFlashing || uiState.isHonking
                }

                if (conditionMet) {
                    if (targetIsOff) {
                        Log.i(tag, "Target status (OFF) reached after $i polls")
                        updateStatus("Lights and Horn are off.")
                    } else {
                        Log.i(tag, "Target status (ON) reached after $i polls")
                        when {
                            uiState.isFlashing && uiState.isHonking -> updateStatus("Lights and Horn are on.")
                            uiState.isFlashing -> updateStatus("Lights are turning on.")
                            else -> updateStatus("Horn is turning on.")
                        }
                    }
                    cancel()
                    return@launch
                }
                delay(5000) // Increased delay
                refreshData() // Actively refresh data
            }
            Log.w(tag, "Car finder polling timed out")
            updateStatus("Failed to get status.")
        }
    }

    private fun sendCommandWithPolling(name: String, action: suspend (String) -> Result<String?>, pin: String, targetStatus: String) {
        viewModelScope.launch {
            Log.i(tag, "Sending command: $name")
            updateStatus("Sending $name command...")
            val result = action(pin)
            result.onSuccess {
                Log.i(tag, "Command $name sent successfully")
                updateStatus("$name command sent!")
                
                if (name == "Start Climate") {
                    val intent = Intent(context, com.spencehouse.logue.service.ClimateControlService::class.java)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                }
                
                startAggressivePolling(targetStatus)
            }.onFailure {
                Log.e(tag, "Command $name failed", it)
                updateStatus("$name failed: ${it.message}")
            }
        }
    }

    fun startClimate(pin: String, temp: Int) = sendCommandWithPolling("Start Climate", { p ->
        vehicleService.startClimate(authService.selectedVin!!, p, temp)
    }, pin, "ON")

    fun stopClimate(pin: String) = sendCommandWithPolling("Stop Climate", { p ->
        vehicleService.stopClimate(authService.selectedVin!!, p)
    }, pin, "OFF")

    fun toggleFlashLights(pin: String) {
        if (uiState.isFlashing) {
            stopFlashAndHorn(pin)
        } else {
            flashLights(pin)
        }
    }

    fun toggleSoundHorn(pin: String) {
        if (uiState.isHonking) {
            stopFlashAndHorn(pin)
        } else {
            soundHorn(pin)
        }
    }

    fun flashLights(pin: String) {
        uiState = uiState.copy(isFlashing = true)
        sendCarFinderCommand("Flash Lights", { p ->
            vehicleService.requestLightHorn(authService.selectedVin!!, p, "lgt")
        }, pin)
    }

    fun soundHorn(pin: String) {
        uiState = uiState.copy(isHonking = true)
        sendCarFinderCommand("Sound Horn", { p ->
            vehicleService.requestLightHorn(authService.selectedVin!!, p, "hrn")
        }, pin)
    }

    fun stopFlashAndHorn(pin: String) {
        uiState = uiState.copy(isFlashing = false, isHonking = false)
        sendCarFinderCommand("Stop Flash and Horn", { p ->
            vehicleService.requestStopLightHorn(authService.selectedVin!!, p)
        }, pin, true)
    }

    fun lockDoors(pin: String) {
        viewModelScope.launch {
            sendCommand("Lock Doors", { p ->
                vehicleService.requestDoorLock(authService.selectedVin!!, p, "alk")
            }, pin)
        }
    }

    fun unlockDoors(pin: String) {
        viewModelScope.launch {
            sendCommand("Unlock Doors", { p ->
                vehicleService.requestDoorLock(authService.selectedVin!!, p, "dulk")
            }, pin)
        }
    }

    override fun onCleared() {
        Log.d(tag, "DashboardViewModel cleared, disconnecting MQTT")
        mqttClient?.disconnect()
        refreshJob?.cancel()
        carFinderPollingJob?.cancel()
        super.onCleared()
    }

    private fun startAggressivePolling(targetStatus: String) {
        viewModelScope.launch {
            Log.d(tag, "Starting aggressive polling for target status: $targetStatus")
            for (i in 1..12) {
                if (!isActive) return@launch
                if (uiState.climateStatus == targetStatus) {
                    Log.i(tag, "Target status reached after $i polls")
                    updateStatus("Climate is ${targetStatus.lowercase()}.")
                    cancel()
                }
                delay(5000)
                refreshData()
            }
        }
    }
}
