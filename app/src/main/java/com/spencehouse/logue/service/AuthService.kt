package com.spencehouse.logue.service

import android.util.Log
import com.spencehouse.logue.service.remote.HondaWscApi
import com.spencehouse.logue.service.remote.IdentityApi
import com.spencehouse.logue.service.remote.dto.TokenResponse
import com.spencehouse.logue.service.remote.dto.Vehicle
import kotlinx.coroutines.delay
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthService @Inject constructor(
    private val identityApi: IdentityApi,
    private val wscApi: HondaWscApi,
    val sessionManager: SessionManager
) {
    private val tag = "AuthService"
    var vehicles: List<Vehicle> = emptyList()
    var selectedVin: String? = null

    suspend fun login(username: String? = null, password: String? = null, vin: String? = null): Result<Unit> {
        if (username.isNullOrEmpty() || password.isNullOrEmpty()) {
            return Result.failure(Exception("No credentials provided"))
        }

        return try {
            Log.d(tag, "Starting login for $username")
            // 1. Register Client
            val regResp = identityApi.registerClient(
                mapOf(
                    "client_id" to Config.CLIENT_ID,
                    "client_secret" to Config.CLIENT_SECRET
                )
            )
            val clientRegKey = regResp.body()?.clientRegistrationKey?.clientRegKey
                ?: return Result.failure(Exception("Failed to register client: ${regResp.code()}"))
            Log.d(tag, "Client registered successfully")

            // 2. Generate Token with retry
            var attempt = 0
            var tokenResp: Response<TokenResponse>? = null
            while (attempt < 3) {
                Log.d(tag, "Attempting to generate token, attempt ${attempt + 1}")
                tokenResp = identityApi.generateToken(
                    mapOf(
                        "client_reg_key" to clientRegKey,
                        "device_description" to "Android_Logue_Client",
                        "username" to username,
                        "password" to password
                    )
                )
                if (tokenResp.isSuccessful) {
                    break
                }
                Log.w(tag, "Token generation failed with code: ${tokenResp.code()}. Retrying in 1 second.")
                delay(1000)
                attempt++
            }

            val tokenData = tokenResp?.body() ?: return Result.failure(Exception("Auth failed: ${tokenResp?.code()}"))
            if (tokenData.requestStatus != "success") {
                return Result.failure(Exception("Auth status: ${tokenData.requestStatus}"))
            }

            sessionManager.accessToken = tokenData.token.accessToken
            sessionManager.hidasIdent = tokenData.user.hidasIdent
            Log.d(tag, "Token generated and session saved")

            // 3. Get Vehicles
            return fetchVehicles()
        } catch (e: Exception) {
            Log.e(tag, "Login exception", e)
            Result.failure(e)
        }
    }

    suspend fun fetchVehicles(): Result<Unit> {
        return try {
            val token = sessionManager.accessToken ?: return Result.failure(Exception("No token"))
            val ident = sessionManager.hidasIdent ?: return Result.failure(Exception("No hidas_ident"))

            // 3. Get Vehicles
            val vehicleHeaders = Config.COMMON_HEADERS.toMutableMap().apply {
                put("Authorization", "Bearer $token")
                put("hondaHeaderType.version", "2.0")
                put("hondaHeaderType.siteId", "00e0e97f0fb543208a918fc946dea334")
                put("hondaHeaderType.messageId", UUID.randomUUID().toString())
                put("hondaHeaderType.systemId", "com.honda.dealer.cv_android")
                put("hondaHeaderType.userId", ident)
                put("hondaHeaderType.clientType", "Mobile")
                put("hondaHeaderType.collectedTimeStamp", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(
                    Date()
                ))
                put("Content-Type", "application/json")
                put("Accept", "application/json")
            }

            val vehiclesResp = wscApi.getVehicles(vehicleHeaders)
            val vehicleData = vehiclesResp.body() ?: return Result.failure(Exception("Failed to get vehicles: ${vehiclesResp.code()}"))

            if (vehicleData.status != "SUCCESS") {
                return Result.failure(Exception("Get vehicles failed: ${vehicleData.status}"))
            }

            this.vehicles = vehicleData.vehicleInfo
            Log.d(tag, "Fetched ${vehicles.size} vehicles")
            if (vehicles.isEmpty()) {
                return Result.failure(Exception("No vehicles found on this account"))
            }

            val savedVin = sessionManager.vin
            this.selectedVin = savedVin ?: vehicles.first().vin
            sessionManager.vin = selectedVin
            Log.d(tag, "Selected VIN: $selectedVin")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "fetchVehicles exception", e)
            Result.failure(e)
        }
    }

    fun updateSelectedVin(vin: String) {
        this.selectedVin = vin
        sessionManager.vin = vin
        Log.d(tag, "Persistence updated for VIN: $vin")
    }

    fun logout() {
        Log.d(tag, "Logging out")
        sessionManager.logout()
        vehicles = emptyList()
        selectedVin = null
    }

    fun getVehicleName(): String {
        val vehicle = vehicles.find { it.vin == selectedVin } ?: vehicles.firstOrNull()
        val name = vehicle?.aliasName ?: vehicle?.let { "${it.modelYear} ${it.divisionName} ${it.modelCode}" } ?: "Unknown Vehicle"
        Log.d(tag, "getVehicleName: $name (selectedVin: $selectedVin, vehicleCount: ${vehicles.size})")
        return name
    }

    fun isLoggedIn(): Boolean {
        return sessionManager.accessToken != null
    }
}
