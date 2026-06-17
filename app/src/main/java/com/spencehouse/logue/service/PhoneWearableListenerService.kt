package com.spencehouse.logue.service

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PhoneWearableListenerService : WearableListenerService() {
    private val tag = "PhoneWearListener"
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface PhoneWearableEntryPoint {
        fun vehicleService(): VehicleService
        fun authService(): AuthService
        fun wearableSyncManager(): WearableSyncManager
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(tag, "Message received from Wear OS: ${messageEvent.path}")
        
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            PhoneWearableEntryPoint::class.java
        )
        val vehicleService = entryPoint.vehicleService()
        val authService = entryPoint.authService()

        val path = messageEvent.path
        if (path == "/request/telemetry") {
            serviceScope.launch {
                try {
                    val wearableSyncManager = entryPoint.wearableSyncManager()
                    val sessionManager = authService.sessionManager
                    val cachedVin = authService.selectedVin ?: sessionManager.vin
                    val cachedBattery = sessionManager.cachedBatteryPercentage
                    val cachedRange = sessionManager.cachedRange
                    val cachedStatus = sessionManager.cachedChargeStatus ?: "Unknown"
                    val targetLimit = sessionManager.targetChargeLevel
                    val isPluggedIn = sessionManager.cachedIsPluggedIn
                    
                    if (!cachedVin.isNullOrEmpty() && cachedBattery >= 0 && cachedRange >= 0) {
                        Log.i(tag, "Sending cached telemetry on request: Battery $cachedBattery%, Range $cachedRange, Target $targetLimit, Plugged $isPluggedIn")
                        wearableSyncManager.syncVehicleTelemetry(
                            cachedVin,
                            cachedBattery,
                            cachedRange,
                            cachedStatus,
                            targetLimit,
                            isPluggedIn,
                            sessionManager.useCelsius,
                            sessionManager.useKilometers
                        )
                    } else {
                        Log.w(tag, "No cached telemetry available to sync")
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error handling Wear OS telemetry request", e)
                }
            }
            return
        }

        val vin = authService.selectedVin
        if (vin.isNullOrEmpty()) {
            Log.e(tag, "No vehicle selected, cannot execute command")
            return
        }

        // Get saved PIN
        val pin = authService.sessionManager.pin
        if (pin.isNullOrEmpty()) {
            Log.e(tag, "No PIN saved on phone, cannot execute remote command from watch")
            return
        }

        serviceScope.launch {
            try {
                val path = messageEvent.path
                when {
                    path == "/command/lock" -> {
                        Log.i(tag, "Executing Lock from Wear OS for VIN $vin")
                        vehicleService.requestDoorLock(vin, pin, "alk")
                    }
                    path == "/command/unlock" -> {
                        Log.i(tag, "Executing Unlock from Wear OS for VIN $vin")
                        vehicleService.requestDoorLock(vin, pin, "dulk")
                    }
                    path == "/command/lights" -> {
                        Log.i(tag, "Executing Flash Lights from Wear OS for VIN $vin")
                        vehicleService.requestLightHorn(vin, pin, "lgt")
                    }
                    path == "/command/horn" -> {
                        Log.i(tag, "Executing Sound Horn from Wear OS for VIN $vin")
                        vehicleService.requestLightHorn(vin, pin, "hrn")
                    }
                    path.startsWith("/command/climate_start") -> {
                        val parts = path.split("/")
                        val temp = parts.getOrNull(3)?.toIntOrNull() ?: 72
                        Log.i(tag, "Executing Start Climate from Wear OS for VIN $vin with temp $temp")
                        vehicleService.startClimate(vin, pin, temp)
                    }
                    path == "/command/climate_stop" -> {
                        Log.i(tag, "Executing Stop Climate from Wear OS for VIN $vin")
                        vehicleService.stopClimate(vin, pin)
                    }
                    else -> {
                        Log.w(tag, "Unknown path from Wear OS: $path")
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error executing Wear OS command ${messageEvent.path}", e)
            }
        }
    }
}
