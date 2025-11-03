package com.arranquesuave.motorcontrolapp.network.model

/**
 * MODELOS PARA COMANDOS DE MOTOR VÍA API REST
 * 
 * Usados para enviar comandos al backend cuando se usa MQTT
 * Proporcionan redundancia y logging de comandos
 */

/**
 * Comando de motor para enviar al backend
 */
data class MotorCommand(
    val command: String,        // "arranque6p", "continuo", "paro"
    val values: List<Int>       // Lista de valores PWM (solo para arranque6p)
)

/**
 * Respuesta del backend al enviar comando
 */
data class MotorCommandResponse(
    val success: Boolean,
    val command: String,
    val payload: String,        // Comando ASCII generado
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Estado actual del motor desde el backend
 */
data class MotorStatus(
    val speed: Int,             // Velocidad actual (0-254)
    val state: String,          // "running", "stopped", "starting"
    val current: Float,         // Corriente en Amperios
    val voltage: Float,         // Voltaje en Voltios
    val timestamp: Long         // Timestamp del último update
)

/**
 * Telemetría histórica del motor
 */
data class MotorTelemetry(
    val id: Long,
    val speed: Int,
    val current: Float,
    val voltage: Float,
    val state: String,
    val timestamp: Long
)

/**
 * Comando de configuración para el motor
 */
data class MotorConfig(
    val type: String,           // "pid", "logistic", "general"
    val parameters: Map<String, Any>    // Parámetros específicos del tipo
)

/**
 * Respuesta de configuración
 */
data class MotorConfigResponse(
    val success: Boolean,
    val message: String,
    val config: MotorConfig?
)
