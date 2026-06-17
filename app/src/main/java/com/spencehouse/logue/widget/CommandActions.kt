package com.spencehouse.logue.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.spencehouse.logue.service.SessionManager
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class BaseCommandAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val sessionManager = SessionManager(context)
        val pin = sessionManager.pin
        val vin = sessionManager.vin

        if (pin.isNullOrEmpty() || vin.isNullOrEmpty()) {
            val intent = android.content.Intent(context, com.spencehouse.logue.MainActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
            return
        }

        val entryPoint = EntryPointAccessors.fromApplication(context, com.spencehouse.logue.di.WidgetModule.WidgetEntryPoint::class.java)
        val vehicleService = entryPoint.vehicleService()

        CoroutineScope(Dispatchers.IO).launch {
            executeCommand(vehicleService, vin, pin)
        }
    }

    abstract suspend fun executeCommand(vehicleService: com.spencehouse.logue.service.VehicleService, vin: String, pin: String)
}

class LockAction : BaseCommandAction() {
    override suspend fun executeCommand(vehicleService: com.spencehouse.logue.service.VehicleService, vin: String, pin: String) {
        vehicleService.requestDoorLock(vin, pin, "alk")
    }
}

class UnlockAction : BaseCommandAction() {
    override suspend fun executeCommand(vehicleService: com.spencehouse.logue.service.VehicleService, vin: String, pin: String) {
        vehicleService.requestDoorLock(vin, pin, "dulk")
    }
}

class LightsAction : BaseCommandAction() {
    override suspend fun executeCommand(vehicleService: com.spencehouse.logue.service.VehicleService, vin: String, pin: String) {
        vehicleService.requestLightHorn(vin, pin, "lgt")
    }
}

class HornAction : BaseCommandAction() {
    override suspend fun executeCommand(vehicleService: com.spencehouse.logue.service.VehicleService, vin: String, pin: String) {
        vehicleService.requestLightHorn(vin, pin, "hrn")
    }
}
