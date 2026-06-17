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
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(tag, "Message received from Wear OS: ${messageEvent.path}")
        
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            PhoneWearableEntryPoint::class.java
        )
        val vehicleService = entryPoint.vehicleService()
        val authService = entryPoint.authService()

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
                when (messageEvent.path) {
                    "/command/lock" -> {
                        Log.i(tag, "Executing Lock from Wear OS for VIN $vin")
                        vehicleService.requestDoorLock(vin, pin, "alk")
                    }
                    "/command/unlock" -> {
                        Log.i(tag, "Executing Unlock from Wear OS for VIN $vin")
                        vehicleService.requestDoorLock(vin, pin, "dulk")
                    }
                    "/command/lights" -> {
                        Log.i(tag, "Executing Flash Lights from Wear OS for VIN $vin")
                        vehicleService.requestLightHorn(vin, pin, "lgt")
                    }
                    "/command/horn" -> {
                        Log.i(tag, "Executing Sound Horn from Wear OS for VIN $vin")
                        vehicleService.requestLightHorn(vin, pin, "hrn")
                    }
                    "/command/climate_start" -> {
                        Log.i(tag, "Executing Start Climate from Wear OS for VIN $vin")
                        // Default to 72 degrees F (22C)
                        vehicleService.startClimate(vin, pin, 72)
                    }
                    "/command/climate_stop" -> {
                        Log.i(tag, "Executing Stop Climate from Wear OS for VIN $vin")
                        vehicleService.stopClimate(vin, pin)
                    }
                    else -> {
                        Log.w(tag, "Unknown path from Wear OS: ${messageEvent.path}")
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error executing Wear OS command ${messageEvent.path}", e)
            }
        }
    }
}
