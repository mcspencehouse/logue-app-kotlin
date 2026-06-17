package com.spencehouse.logue.service.remote

import com.spencehouse.logue.service.remote.dto.*
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.*

interface HondaWscApi {
    @GET("REST/NGT/MyVehicle/1.0")
    suspend fun getVehicles(
        @HeaderMap headers: Map<String, String>,
    ): Response<VehicleInfoResponse>

    @POST("REST/CIG/services/1.0/token")
    suspend fun getCigToken(
        @HeaderMap headers: Map<String, String>,
        @Body request: CigTokenRequest,
    ): Response<CigTokenResponse>

    @POST("REST/NGT/CIG/dbd/async")
    suspend fun requestDashboard(
        @HeaderMap headers: Map<String, String>,
        @Body request: DashboardRequest,
    ): Response<DashboardResponse>

    @POST("REST/NGT/CIG/eng/async/srt")
    suspend fun startClimate(
        @HeaderMap headers: Map<String, String>,
        @Body request: ClimateRequest,
    ): Response<ClimateResponse>

    @POST("REST/NGT/CIG/eng/async/sop")
    suspend fun stopClimate(
        @HeaderMap headers: Map<String, String>,
        @Body request: ClimateRequest,
    ): Response<ClimateResponse>

    @POST("REST/NGT/TargetChargeLevel/1.0")
    suspend fun setTargetChargeLevel(
        @HeaderMap headers: Map<String, String>,
        @Body request: TargetChargeLevelRequest,
    ): Response<RemoteCommandResponse>

    @POST("REST/NGT/CIG/cfhl/async/{action}")
    suspend fun requestLightHorn(
        @Path("action") action: String, // "lgt" or "hrn"
        @HeaderMap headers: Map<String, String>,
        @Body request: RemoteCommandRequest,
    ): Response<RemoteCommandResponse>

    @POST("REST/NGT/CIG/cfhl/async/{action}")
    suspend fun requestStopLightHorn(
        @Path("action") action: String, // "sop"
        @HeaderMap headers: Map<String, String>,
        @Body request: RemoteCommandRequest,
    ): Response<RemoteCommandResponse>

    @POST("REST/NGT/CIG/lk/async/{action}")
    suspend fun requestDoorLock(
        @Path("action") action: String, // "alk" or "dulk"
        @HeaderMap headers: Map<String, String>,
        @Body request: RemoteCommandRequest,
    ): Response<RemoteCommandResponse>

    @GET("REST/NGT/getClimateStatus/1.0/{vin}")
    suspend fun getClimateStatus(
        @Path("vin") vin: String,
        @HeaderMap headers: Map<String, String>,
    ): Response<JsonObject> // Using Map for generic response

    @POST("REST/NGT/CIG/{action}/async/")
    suspend fun requestVehicleLocation(
        @Path("action") action: String, // "cfl"
        @HeaderMap headers: Map<String, String>,
        @Body request: RemoteCommandRequest,
    ): Response<RemoteCommandResponse>
}
