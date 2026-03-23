package com.spencehouse.logue.service

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(@ApplicationContext context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "logue_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )



    var vin: String?
        get() = sharedPreferences.getString("honda_vin", null)
        set(value) = sharedPreferences.edit { putString("honda_vin", value) }

    var pin: String?
        get() = sharedPreferences.getString("honda_pin", null)
        set(value) = sharedPreferences.edit { putString("honda_pin", value) }

    var useCelsius: Boolean
        get() = sharedPreferences.getBoolean("use_celsius", false)
        set(value) = sharedPreferences.edit { putBoolean("use_celsius", value) }

    var useKilometers: Boolean
        get() = sharedPreferences.getBoolean("use_kilometers", false)
        set(value) = sharedPreferences.edit { putBoolean("use_kilometers", value) }

    var useKpa: Boolean
        get() = sharedPreferences.getBoolean("use_kpa", false)
        set(value) = sharedPreferences.edit { putBoolean("use_kpa", value) }

    var accessToken: String?
        get() = sharedPreferences.getString("access_token", null)
        set(value) = sharedPreferences.edit { putString("access_token", value) }

    var hidasIdent: String?
        get() = sharedPreferences.getString("hidas_ident", null)
        set(value) = sharedPreferences.edit { putString("hidas_ident", value) }


    fun logout() {
        sharedPreferences.edit { clear() }
    }

    var cachedBatteryPercentage: Int
        get() = sharedPreferences.getInt("cached_battery_percentage", -1)
        set(value) = sharedPreferences.edit { putInt("cached_battery_percentage", value) }

    var cachedRange: Int
        get() = sharedPreferences.getInt("cached_range", -1)
        set(value) = sharedPreferences.edit { putInt("cached_range", value) }

    var cachedChargeStatus: String?
        get() = sharedPreferences.getString("cached_charge_status", null)
        set(value) = sharedPreferences.edit { putString("cached_charge_status", value) }

    var cachedIsPluggedIn: Boolean
        get() = sharedPreferences.getBoolean("cached_is_plugged_in", false)
        set(value) = sharedPreferences.edit { putBoolean("cached_is_plugged_in", value) }

    var cachedClimateStatus: String?
        get() = sharedPreferences.getString("cached_climate_status", null)
        set(value) = sharedPreferences.edit { putString("cached_climate_status", value) }

    var targetChargeLevel: Int
        get() = sharedPreferences.getInt("target_charge_level", 80)
        set(value) = sharedPreferences.edit { putInt("target_charge_level", value) }

    var cachedVoltage: Int
        get() = sharedPreferences.getInt("cached_voltage", -1)
        set(value) = sharedPreferences.edit { putInt("cached_voltage", value) }
}
