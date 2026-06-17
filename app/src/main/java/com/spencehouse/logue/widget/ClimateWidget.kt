package com.spencehouse.logue.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.spencehouse.logue.service.SessionManager
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

import androidx.glance.GlanceTheme
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.ColorFilter
import com.spencehouse.logue.R
import androidx.glance.text.TextAlign

class ClimateWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, com.spencehouse.logue.di.WidgetModule.WidgetEntryPoint::class.java)
            val sessionManager = entryPoint.sessionManager()
            val prefs = androidx.glance.currentState<androidx.datastore.preferences.core.Preferences>()
            val widgetStatus = prefs[ClimateStatusKey]
            val climateStatus = widgetStatus ?: sessionManager.cachedClimateStatus ?: "OFF"
            val isOn = climateStatus != "OFF"
            val savedTemp = androidx.glance.currentState(TemperatureKey) ?: if (sessionManager.useCelsius) 22 else 72
            GlanceTheme {
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                        .appWidgetBackground()
                        .background(GlanceTheme.colors.surfaceVariant)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_thermostat),
                            contentDescription = "Climate",
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                            modifier = GlanceModifier.size(16.dp)
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Text(
                            text = "EV CLIMATE",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    if (isOn) {
                        Text(
                            text = climateStatus,
                            style = TextStyle(
                                color = GlanceTheme.colors.error,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = GlanceModifier.size(40.dp).background(GlanceTheme.colors.surface).cornerRadius(20.dp).clickable(actionRunCallback(IncreaseTempAction::class.java)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+",
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onSurface,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                )
                            }
                            Spacer(modifier = GlanceModifier.height(8.dp))
                            Text(
                                text = "$savedTemp${if (sessionManager.useCelsius) "°C" else "°F"}",
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurface,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.height(8.dp))
                            Box(
                                modifier = GlanceModifier.size(40.dp).background(GlanceTheme.colors.surface).cornerRadius(20.dp).clickable(actionRunCallback(DecreaseTempAction::class.java)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "-",
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onSurface,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        val actionName = if (isOn) "STOP" else "START"
                        val actionClass = if (isOn) StopClimateAction::class.java else StartClimateAction::class.java

                        Box(
                            modifier = GlanceModifier
                                .background(if (isOn) GlanceTheme.colors.error else GlanceTheme.colors.primaryContainer)
                                .cornerRadius(100.dp) // Fully rounded
                                .padding(vertical = 8.dp, horizontal = 16.dp)
                                .clickable(actionRunCallback(actionClass)),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    provider = ImageProvider(R.drawable.ic_power),
                                    contentDescription = actionName,
                                    colorFilter = ColorFilter.tint(if (isOn) GlanceTheme.colors.onError else GlanceTheme.colors.onPrimaryContainer),
                                    modifier = GlanceModifier.size(16.dp)
                                )
                                Spacer(modifier = GlanceModifier.width(4.dp))
                                Text(
                                    text = actionName,
                                    style = TextStyle(
                                        color = if (isOn) GlanceTheme.colors.onError else GlanceTheme.colors.onPrimaryContainer,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

class StartClimateAction : ActionCallback {
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

        val serviceIntent = android.content.Intent(context, com.spencehouse.logue.service.ClimateControlService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        var temp = if (sessionManager.useCelsius) 22 else 72
        androidx.glance.appwidget.state.updateAppWidgetState(context, glanceId) { prefs ->
            temp = prefs[TemperatureKey] ?: temp
        }
        
        try {
            vehicleService.startClimate(vin, pin, temp)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Initial optimistic update
        sessionManager.cachedClimateStatus = "ON"
        androidx.glance.appwidget.state.updateAppWidgetState(context, glanceId) { p ->
            p[ClimateStatusKey] = "ON"
        }
        ClimateWidget().update(context, glanceId)
        
        // Poll for actual state change
        for (i in 0..5) { // Poll every 5s for 30s
            kotlinx.coroutines.delay(5000)
            try {
                val statusResult = vehicleService.getClimateStatus(vin)
                if (statusResult.isSuccess) {
                    val statusObj = statusResult.getOrNull()
                    val isClimateOn = statusObj?.get("climateStatus")?.jsonPrimitive?.content == "ON"
                    if (isClimateOn) {
                        sessionManager.cachedClimateStatus = "ON"
                        androidx.glance.appwidget.state.updateAppWidgetState(context, glanceId) { p ->
                            p[ClimateStatusKey] = "ON"
                        }
                        ClimateWidget().update(context, glanceId)
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class StopClimateAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val sessionManager = SessionManager(context)
        val pin = sessionManager.pin
        val vin = sessionManager.vin

        if (pin.isNullOrEmpty() || vin.isNullOrEmpty()) return

        val entryPoint = EntryPointAccessors.fromApplication(context, com.spencehouse.logue.di.WidgetModule.WidgetEntryPoint::class.java)
        val vehicleService = entryPoint.vehicleService()

        val stopIntent = android.content.Intent(context, com.spencehouse.logue.service.ClimateControlService::class.java).apply {
            action = "STOP_CLIMATE"
        }
        context.startService(stopIntent)

        // Initial optimistic update
        sessionManager.cachedClimateStatus = "OFF"
        androidx.glance.appwidget.state.updateAppWidgetState(context, glanceId) { p ->
            p[ClimateStatusKey] = "OFF"
        }
        ClimateWidget().update(context, glanceId)
        
        // Poll for actual state change
        for (i in 0..5) { // Poll every 5s for 30s
            kotlinx.coroutines.delay(5000)
            try {
                val statusResult = vehicleService.getClimateStatus(vin)
                if (statusResult.isSuccess) {
                    val statusObj = statusResult.getOrNull()
                    val isClimateOn = statusObj?.get("climateStatus")?.jsonPrimitive?.content == "ON"
                    if (!isClimateOn) {
                        sessionManager.cachedClimateStatus = "OFF"
                        androidx.glance.appwidget.state.updateAppWidgetState(context, glanceId) { p ->
                            p[ClimateStatusKey] = "OFF"
                        }
                        ClimateWidget().update(context, glanceId)
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

val TemperatureKey = androidx.datastore.preferences.core.intPreferencesKey("climate_temperature_key")
val ClimateStatusKey = androidx.datastore.preferences.core.stringPreferencesKey("climate_status_key")

class IncreaseTempAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        androidx.glance.appwidget.state.updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[TemperatureKey] ?: 72
            prefs[TemperatureKey] = current + 1
        }
        ClimateWidget().update(context, glanceId)
    }
}

class DecreaseTempAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        androidx.glance.appwidget.state.updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[TemperatureKey] ?: 72
            prefs[TemperatureKey] = current - 1
        }
        ClimateWidget().update(context, glanceId)
    }
}

class ClimateWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ClimateWidget()
}
