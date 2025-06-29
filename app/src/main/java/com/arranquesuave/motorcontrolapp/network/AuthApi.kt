package com.arranquesuave.motorcontrolapp.network

import com.arranquesuave.motorcontrolapp.network.model.AuthRequest
import com.arranquesuave.motorcontrolapp.network.model.VerifyRequest
import com.arranquesuave.motorcontrolapp.network.model.AuthResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("/api/auth/signup")
    suspend fun signup(@Body req: AuthRequest): Response<Void>

    @POST("/api/auth/verify")
    suspend fun verify(@Body req: VerifyRequest): AuthResponse

    @POST("/api/auth/login")
    suspend fun login(@Body req: AuthRequest): AuthResponse
    @POST("/api/auth/logout")
    suspend fun logout(): Response<Void>
}
