package com.spencehouse.logue.ui.model

data class VehicleUiModel(
    val vin: String,
    val modelYear: String,
    val divisionName: String,
    val modelCode: String,
    val aliasName: String?,
    val asset34FrontPath: String?
)

data class VehicleLocation(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)

data class DashboardUiState(
    val vehicleName: String = "",
    val vehicles: List<VehicleUiModel> = emptyList(),
    val selectedVin: String? = null,
    val isEv: Boolean = true,
    val batteryPercentage: Int? = null,
    val range: Int? = null,
    val chargeStatus: String = "--",
    val chargeVoltage: String? = null,
    val chargeCompletionTime: String? = null,
    val odometer: Int? = null,
    val tirePressures: Map<String, Double?> = emptyMap(),
    val climateStatus: String = "OFF",
    val statusText: String = "Connecting...",
    val lastUpdated: String = "Never",
    val useCelsius: Boolean = false,
    val useKilometers: Boolean = false,
    val useKpa: Boolean = false,
    val isPluggedIn: Boolean = false,
    val targetChargeLevel: Int = 80,
    val isLoading: Boolean = false,
    val error: String? = null,
    val savedPin: String? = null,
    val isFlashing: Boolean = false,
    val isHonking: Boolean = false,
    val vehicleLocation: VehicleLocation? = null,
    val vehicleLocationError: String? = null
)
