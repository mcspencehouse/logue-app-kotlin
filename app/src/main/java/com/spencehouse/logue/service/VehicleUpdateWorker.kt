package com.spencehouse.logue.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.spencehouse.logue.MainActivity
import com.spencehouse.logue.widget.BatteryWidget
import com.spencehouse.logue.widget.ClimateWidget
import com.spencehouse.logue.widget.CommandsWidget
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@HiltWorker
class VehicleUpdateWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val authService: AuthService,
    private val vehicleService: VehicleService
) : CoroutineWorker(appContext, workerParams) {

    private val tag = "VehicleUpdateWorker"

    override suspend fun doWork(): Result {
        Log.d(tag, "Starting background vehicle update")
        
        val vin = authService.selectedVin
        if (vin.isNullOrEmpty()) {
            Log.e(tag, "No vehicle selected, aborting update")
            return Result.success()
        }

        try {
            val result = vehicleService.getDashboardData(vin)
            result.onSuccess { data ->
                Log.d(tag, "Successfully fetched dashboard data in background: $data")
                val sessionManager = authService.sessionManager
                
                val oldBattery = sessionManager.cachedBatteryPercentage
                val newBattery = data.batteryPercentage
                
                sessionManager.cachedBatteryPercentage = newBattery
                sessionManager.cachedRange = data.range
                
                // Check if charging target is reached
                val target = sessionManager.targetChargeLevel
                if (newBattery >= target && oldBattery < target) {
                    sendChargeCompleteNotification(newBattery)
                }

                BatteryWidget().updateAll(appContext)
                ClimateWidget().updateAll(appContext)
                CommandsWidget().updateAll(appContext)
                
            }.onFailure {
                Log.e(tag, "Failed to get dashboard data", it)
                return Result.retry()
            }
            
            // Also update climate status
            val climateResult = vehicleService.getClimateStatus(vin)
            climateResult.onSuccess {
                val status = it.jsonObject["climateStatus"]?.jsonPrimitive?.content ?: "OFF"
                authService.sessionManager.cachedClimateStatus = status.uppercase()
            }
            
            return Result.success()
        } catch (e: Exception) {
            Log.e(tag, "Worker exception", e)
            return Result.retry()
        }
    }

    private fun sendChargeCompleteNotification(batteryPercentage: Int) {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(appContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(appContext, "vehicle_status")
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentTitle("Charge Target Reached")
            .setContentText("Your vehicle has reached its charge target ($batteryPercentage%).")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1001, builder.build())
    }
}