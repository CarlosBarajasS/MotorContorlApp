package com.arranquesuave.motorcontrolapp.network

import com.arranquesuave.motorcontrolapp.network.model.AuthRequest
import com.arranquesuave.motorcontrolapp.network.model.AuthResponse
import com.arranquesuave.motorcontrolapp.network.model.MotorCommand
import com.arranquesuave.motorcontrolapp.network.model.MotorCommandResponse
import com.arranquesuave.motorcontrolapp.network.model.MotorStatus
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {
    // ============================================
    // ENDPOINTS DE AUTENTICACIÓN
    // ============================================
    
    @POST("/api/auth/signup")
    suspend fun signup(@Body req: AuthRequest): Response<Void>

    @POST("/api/auth/login")
    suspend fun login(@Body req: AuthRequest): AuthResponse
    
    @POST("/api/auth/logout")
    suspend fun logout(): Response<Void>
    
    // ============================================
    // ENDPOINTS DE MOTOR (MQTT + API)
    // ============================================
    
    /**
     * Enviar comando al motor (arranque6p, continuo, paro)
     * Funciona en paralelo con MQTT para redundancia
     */
    @POST("/api/motor/command")
    suspend fun sendMotorCommand(@Body command: MotorCommand): Response<MotorCommandResponse>
    
    /**
     * Obtener estado actual del motor
     */
    @GET("/api/motor/status")
    suspend fun getMotorStatus(): Response<MotorStatus>
    
    /**
     * Obtener historial de telemetría del motor
     */
    @GET("/api/motor/telemetry")
    suspend fun getMotorTelemetry(): Response<List<MotorStatus>>
}
