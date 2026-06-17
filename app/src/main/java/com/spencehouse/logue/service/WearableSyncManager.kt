package com.spencehouse.logue.service

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WearableSyncManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val tag = "WearableSyncManager"

    fun syncVehicleTelemetry(
        vin: String,
        batteryPercentage: Int,
        range: Int,
        statusText: String
    ) {
        try {
            Log.d(tag, "Syncing telemetry to Wear OS - VIN: $vin, Battery: $batteryPercentage%, Range: $range miles, Status: $statusText")
            
            val dataClient = Wearable.getDataClient(context)
            val putDataMapReq = PutDataMapRequest.create("/vehicle/telemetry").apply {
                dataMap.putString("vin", vin)
                dataMap.putInt("batteryPercentage", batteryPercentage)
                dataMap.putInt("range", range)
                dataMap.putString("statusText", statusText)
                dataMap.putLong("timestamp", System.currentTimeMillis())
            }
            
            val putDataReq = putDataMapReq.asPutDataRequest()
            putDataReq.setUrgent() // Send instantly

            dataClient.putDataItem(putDataReq)
                .addOnSuccessListener {
                    Log.d(tag, "Successfully queued telemetry sync item")
                }
                .addOnFailureListener { e ->
                    Log.e(tag, "Failed to sync telemetry to Wear OS", e)
                }
        } catch (e: Exception) {
            Log.e(tag, "Exception syncing telemetry to Wear OS", e)
        }
    }
}
