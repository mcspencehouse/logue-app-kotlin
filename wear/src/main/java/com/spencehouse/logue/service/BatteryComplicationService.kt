package com.spencehouse.logue.service

import android.content.Context
import android.util.Log
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest

class BatteryComplicationService : ComplicationDataSourceService() {
    private val tag = "BatteryComplication"

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return createComplicationData(80, 246, type)
    }

    override fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationRequestListener
    ) {
        val sharedPrefs = getSharedPreferences("wear_logue_prefs", Context.MODE_PRIVATE)
        val batteryPct = sharedPrefs.getInt("batteryPercentage", -1)
        val range = sharedPrefs.getInt("range", -1)

        Log.d(tag, "Complication update requested. Cached battery: $batteryPct%, range: $range")
        val data = createComplicationData(batteryPct, range, request.complicationType)
        listener.onComplicationData(data)
    }

    private fun createComplicationData(
        batteryPct: Int,
        range: Int,
        type: ComplicationType
    ): ComplicationData {
        val hasData = batteryPct in 0..100
        val textStr = if (hasData) "$batteryPct%" else "--"
        val titleStr = if (hasData && range >= 0) "$range mi" else "Car"

        val plainText = PlainComplicationText.Builder(textStr).build()
        val plainTitle = PlainComplicationText.Builder(titleStr).build()
        val desc = PlainComplicationText.Builder("Vehicle battery charge level").build()

        return when (type) {
            ComplicationType.RANGED_VALUE -> {
                val valueVal = if (hasData) batteryPct.toFloat() else 0f
                RangedValueComplicationData.Builder(
                    value = valueVal,
                    min = 0f,
                    max = 100f,
                    contentDescription = desc
                )
                .setText(plainText)
                .setTitle(plainTitle)
                .build()
            }
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = plainText,
                    contentDescription = desc
                )
                .setTitle(plainTitle)
                .build()
            }
            else -> {
                // Return a basic fallback if watch face requests unsupported types
                ShortTextComplicationData.Builder(
                    text = plainText,
                    contentDescription = desc
                ).build()
            }
        }
    }
}
