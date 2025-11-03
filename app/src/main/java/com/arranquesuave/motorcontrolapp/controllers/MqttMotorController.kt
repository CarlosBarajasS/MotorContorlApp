package com.arranquesuave.motorcontrolapp.controllers

import android.util.Log
import com.arranquesuave.motorcontrolapp.interfaces.MotorController
import com.arranquesuave.motorcontrolapp.services.MqttService
import com.arranquesuave.motorcontrolapp.network.AuthApi
import com.arranquesuave.motorcontrolapp.network.model.MotorCommand

/**
 * CONTROLADOR MQTT PARA MOTOR CD
 * 
 * Implementa MotorController usando comunicación MQTT
 * Funciona tanto para conexión local (casa profesor) como remota (Internet)
 * Mantiene el mismo protocolo ASCII que Bluetooth
 */
class MqttMotorController(
    private val mqttService: MqttService,
    private val authApi: AuthApi,
    private val serverUri: String
) : MotorController {
    
    companion object {
        private const val TAG = "MqttMotorController"
    }
    
    // Callbacks para telemetría
    private var onSpeedCallback: ((Int) -> Unit)? = null
    private var onStatusCallback: ((String) -> Unit)? = null
    private var onCurrentCallback: ((Float) -> Unit)? = null
    private var onVoltageCallback: ((Float) -> Unit)? = null
    
    /**
     * Conectar al broker MQTT
     */
    override suspend fun connect(): Result<Unit> {
        Log.d(TAG, "Connecting to MQTT broker: $serverUri")
        
        return try {
            // Configurar callbacks del servicio MQTT
            setupMqttCallbacks()
            
            // Conectar al broker
            val result = mqttService.connect(serverUri)
            
            if (result.isSuccess) {
                Log.d(TAG, "MQTT connection established successfully")
            } else {
                Log.e(TAG, "Failed to connect to MQTT broker")
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Exception during MQTT connection", e)
            Result.failure(e)
        }
    }
    
    /**
     * Configurar callbacks para telemetría MQTT
     */
    private fun setupMqttCallbacks() {
        mqttService.setOnSpeedReceived { speed ->
            Log.d(TAG, "Speed received: $speed")
            onSpeedCallback?.invoke(speed)
        }
        
        mqttService.setOnStatusReceived { status ->
            Log.d(TAG, "Status received: $status")
            onStatusCallback?.invoke(status)
        }
        
        mqttService.setOnCurrentReceived { current ->
            Log.d(TAG, "Current received: $current A")
            onCurrentCallback?.invoke(current)
        }
        
        mqttService.setOnVoltageReceived { voltage ->
            Log.d(TAG, "Voltage received: $voltage V")
            onVoltageCallback?.invoke(voltage)
        }
    }
    
    /**
     * Enviar comando de arranque suave (6 pasos)
     * Usa tanto MQTT directo como API REST para redundancia
     */
    override suspend fun sendArranque6P(values: List<Int>): Result<Unit> {
        return try {
            Log.d(TAG, "Sending arranque 6P: ${values.joinToString(",")}")
            
            // Opción 1: Envío directo vía MQTT (más rápido)
            mqttService.sendArranque6P(values)
            
            // Opción 2: Envío vía API REST (más confiable, con autenticación)
            try {
                val command = MotorCommand("arranque6p", values)
                val response = authApi.sendMotorCommand(command)
                
                if (response.isSuccessful) {
                    Log.d(TAG, "API command sent successfully")
                } else {
                    Log.w(TAG, "API command failed, but MQTT sent")
                }
            } catch (apiException: Exception) {
                Log.w(TAG, "API call failed, relying on MQTT", apiException)
                // No retornamos error porque MQTT ya se envió
            }
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending arranque 6P", e)
            Result.failure(e)
        }
    }
    
    /**
     * Enviar comando de arranque continuo
     */
    override suspend fun sendContinuo(): Result<Unit> {
        return try {
            Log.d(TAG, "Sending arranque continuo")
            
            // Envío directo vía MQTT
            mqttService.sendContinuo()
            
            // Opcional: Envío vía API REST
            try {
                val command = MotorCommand("continuo", emptyList())
                authApi.sendMotorCommand(command)
            } catch (apiException: Exception) {
                Log.w(TAG, "API call failed for continuo", apiException)
            }
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending continuo", e)
            Result.failure(e)
        }
    }
    
    /**
     * Enviar comando de paro de emergencia
     */
    override suspend fun sendParo(): Result<Unit> {
        return try {
            Log.d(TAG, "Sending paro de emergencia")
            
            // Envío directo vía MQTT (crítico para seguridad)
            mqttService.sendParo()
            
            // También vía API REST para logging
            try {
                val command = MotorCommand("paro", emptyList())
                authApi.sendMotorCommand(command)
            } catch (apiException: Exception) {
                Log.w(TAG, "API call failed for paro", apiException)
            }
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending paro", e)
            Result.failure(e)
        }
    }
    
    /**
     * Desconectar del broker MQTT
     */
    override suspend fun disconnect() {
        Log.d(TAG, "Disconnecting from MQTT broker")
        mqttService.disconnect()
    }
    
    /**
     * Verificar si está conectado
     */
    override fun isConnected(): Boolean {
        return mqttService.isConnected()
    }
    
    /**
     * Obtener información de conexión
     */
    override fun getConnectionInfo(): String {
        return mqttService.getConnectionInfo()
    }
    
    // ============================================
    // CONFIGURACIÓN DE CALLBACKS
    // ============================================
    
    override fun setOnSpeedReceived(callback: (Int) -> Unit) {
        onSpeedCallback = callback
    }
    
    override fun setOnStatusReceived(callback: (String) -> Unit) {
        onStatusCallback = callback
    }
    
    override fun setOnCurrentReceived(callback: (Float) -> Unit) {
        onCurrentCallback = callback
    }
    
    override fun setOnVoltageReceived(callback: (Float) -> Unit) {
        onVoltageCallback = callback
    }
}
