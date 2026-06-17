package com.spencehouse.logue.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.spencehouse.logue.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class ClimateControlService : Service() {

    @Inject lateinit var vehicleService: VehicleService
    @Inject lateinit var authService: AuthService

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private val tag = "ClimateControlService"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(tag, "onStartCommand action: $action")

        if (action == "STOP_CLIMATE") {
            stopClimate()
        } else {
            startForegroundService()
        }

        return START_NOT_STICKY
    }

    private fun startForegroundService() {
        Log.d(tag, "Starting Climate Control Foreground Service")
        
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, ClimateControlService::class.java).apply {
            action = "STOP_CLIMATE"
        }
        val stopPendingIntent = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, "climate_control")
            .setContentTitle("Climate Control is Running")
            .setContentText("Your vehicle's climate control is active.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_delete, "Stop", stopPendingIntent)
            .setOngoing(true)
            .build()

        if (android.os.Build.VERSION.SDK_INT >= 34) {
            startForeground(2001, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(2001, notification)
        }
    }

    private fun stopClimate() {
        val vin = authService.selectedVin
        val pin = authService.sessionManager.pin
        
        if (vin.isNullOrEmpty() || pin.isNullOrEmpty()) {
            Log.e(tag, "Missing VIN or PIN to stop climate")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        serviceScope.launch {
            Log.d(tag, "Stopping climate via API")
            val result = vehicleService.stopClimate(vin, pin)
            result.onSuccess {
                Log.d(tag, "Successfully stopped climate")
                authService.sessionManager.cachedClimateStatus = "OFF"
            }.onFailure {
                Log.e(tag, "Failed to stop climate", it)
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
