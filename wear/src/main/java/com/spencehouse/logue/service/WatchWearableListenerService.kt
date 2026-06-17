package com.spencehouse.logue.service

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

class WatchWearableListenerService : WearableListenerService() {
    private val tag = "WatchWearListener"

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d(tag, "Data changed event received")
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val path = event.dataItem.uri.path
                if (path == "/vehicle/telemetry") {
                    try {
                        val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                        val vin = dataMap.getString("vin", "")
                        val batteryPct = dataMap.getInt("batteryPercentage", -1)
                        val range = dataMap.getInt("range", -1)
                        val statusText = dataMap.getString("statusText", "")
                        val timestamp = dataMap.getLong("timestamp", 0)

                        Log.i(tag, "Received telemetry from phone - VIN: $vin, Battery: $batteryPct%, Range: $range, Status: $statusText")

                        // Save to preferences
                        getSharedPreferences("wear_logue_prefs", Context.MODE_PRIVATE)
                            .edit()
                            .putString("vin", vin)
                            .putInt("batteryPercentage", batteryPct)
                            .putInt("range", range)
                            .putString("statusText", statusText)
                            .putLong("timestamp", timestamp)
                            .apply()

                        // Trigger Complication Update
                        val complicationRequester = ComplicationDataSourceUpdateRequester.create(
                            applicationContext,
                            ComponentName(applicationContext, BatteryComplicationService::class.java)
                        )
                        complicationRequester.requestUpdateAll()
                        Log.d(tag, "Triggered watch complication update request")
                    } catch (e: Exception) {
                        Log.e(tag, "Error parsing telemetry data from phone", e)
                    }
                }
            }
        }
    }
}
