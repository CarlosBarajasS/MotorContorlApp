package com.arranquesuave.motorcontrolapp.network

import com.arranquesuave.motorcontrolapp.network.model.MotorCommand
import com.arranquesuave.motorcontrolapp.network.model.MotorCommandResponse
import com.arranquesuave.motorcontrolapp.network.model.MotorStatus
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface MotorApi {
    @POST("/api/motor/command")
    suspend fun sendMotorCommand(@Body command: MotorCommand): Response<MotorCommandResponse>

    @GET("/api/motor/status")
    suspend fun getMotorStatus(): Response<MotorStatus>

    @GET("/api/motor/telemetry")
    suspend fun getMotorTelemetry(): Response<List<MotorStatus>>
}
