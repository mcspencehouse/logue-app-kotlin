package com.spencehouse.logue.service.remote

import com.spencehouse.logue.service.remote.dto.*
import retrofit2.Response
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface IdentityApi {
    @FormUrlEncoded
    @POST("hidas/rs/client/register")
    suspend fun registerClient(
        @FieldMap data: Map<String, String>,
    ): Response<ClientRegistrationResponse>

    @FormUrlEncoded
    @POST("hidas/rs/token/generate")
    suspend fun generateToken(
        @FieldMap data: Map<String, String>,
    ): Response<TokenResponse>
}
