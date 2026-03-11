package com.arranquesuave.motorcontrolapp.auth.network

import com.arranquesuave.motorcontrolapp.auth.model.AuthRequest
import com.arranquesuave.motorcontrolapp.auth.model.AuthResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("/api/auth/signup")
    suspend fun signup(@Body req: AuthRequest): AuthResponse

    @POST("/api/auth/login")
    suspend fun login(@Body req: AuthRequest): AuthResponse

    @POST("/api/auth/logout")
    suspend fun logout(): Response<Void>
}
