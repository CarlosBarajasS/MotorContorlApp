package com.arranquesuave.motorcontrolapp.services

import android.content.Context
import android.util.Log
import com.arranquesuave.motorcontrolapp.config.MqttConfig
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.datatypes.MqttQos
import kotlinx.coroutines.*
import java.net.URI
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * SERVICIO MQTT MODERNO PARA MOTOR CONTROL APP
 * 
 * Usa HiveMQ MQTT Client (moderno, compatible con AndroidX)
 * Maneja comunicación MQTT con el broker en Raspberry Pi
 * Soporte para comandos de motor y telemetría en tiempo real
 */
class MqttService(private val context: Context) {
    
    companion object {
        private const val TAG = "MqttService"
    }
    
    // Cliente MQTT moderno (HiveMQ)
    private var mqttClient: Mqtt3AsyncClient? = null
    
    // Callbacks para telemetría
    private var onSpeedReceived: ((Int) -> Unit)? = null
    private var onStatusReceived: ((String) -> Unit)? = null
    private var onCurrentReceived: ((Float) -> Unit)? = null
    private var onVoltageReceived: ((Float) -> Unit)? = null
    
    // Estado de conexión
    private var isConnecting = false
    private var currentServerUri = ""
    
    /**
     * Conectar al broker MQTT usando HiveMQ Client
     */
    suspend fun connect(serverUri: String): Result<Unit> = suspendCancellableCoroutine { cont ->
        if (isConnecting) {
            cont.resume(Result.failure(Exception("Already connecting")))
            return@suspendCancellableCoroutine
        }
        
        try {
            isConnecting = true
            currentServerUri = serverUri
            Log.d(TAG, "Connecting to MQTT broker: $serverUri")
            
            // Parsear URI (tcp://192.168.1.12:1885)
            val uri = URI(serverUri)
            val host = uri.host
            val port = uri.port
            
            val clientId = MqttConfig.Utils.generateClientId()
            
            // Crear cliente HiveMQ con sintaxis correcta
            mqttClient = MqttClient.builder()
                .useMqttVersion3()
                .identifier(clientId)
                .serverHost(host)
                .serverPort(port)
                .buildAsync()
            
            // Conectar al broker
            mqttClient?.connect()
                ?.whenComplete { connAck, throwable ->
                    isConnecting = false
                    
                    if (throwable != null) {
                        Log.e(TAG, "MQTT Connection failed", throwable)
                        cont.resume(Result.failure(throwable))
                    } else {
                        Log.d(TAG, "MQTT Connected successfully: ${connAck.returnCode}")
                        
                        // Suscribirse a topics de telemetría DESPUÉS de conectar
                        subscribeToTelemetryTopics()
                        
                        cont.resume(Result.success(Unit))
                    }
                }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating MQTT connection", e)
            isConnecting = false
            cont.resume(Result.failure(e))
        }
    }
    
    /**
     * Suscribirse a topics de telemetría del motor
     */
    private fun subscribeToTelemetryTopics() {
        val topics = arrayOf(
            MqttConfig.Topics.MOTOR_SPEED,
            MqttConfig.Topics.MOTOR_STATE,
            MqttConfig.Topics.MOTOR_CURRENT,
            MqttConfig.Topics.MOTOR_VOLTAGE,
            MqttConfig.Topics.MOTOR_RAW
        )
        
        topics.forEach { topic ->
            mqttClient?.subscribeWith()
                ?.topicFilter(topic)
                ?.qos(MqttQos.AT_LEAST_ONCE)  // ✅ Sintaxis correcta HiveMQ
                ?.callback { publish ->
                    handleIncomingMessage(publish.topic.toString(), String(publish.payloadAsBytes))
                }
                ?.send()
                ?.whenComplete { subAck, throwable ->
                    if (throwable != null) {
                        Log.e(TAG, "Failed to subscribe to $topic", throwable)
                    } else {
                        Log.d(TAG, "Subscribed to $topic successfully")
                    }
                }
        }
    }
    
    /**
     * Manejar mensajes entrantes del broker MQTT
     */
    private fun handleIncomingMessage(topic: String, payload: String) {
        MqttConfig.Debug.logTelemetry(topic, payload)
        
        when (topic) {
            MqttConfig.Topics.MOTOR_SPEED -> {
                payload.toIntOrNull()?.let { speed ->
                    onSpeedReceived?.invoke(speed)
                }
            }
            
            MqttConfig.Topics.MOTOR_STATE -> {
                onStatusReceived?.invoke(payload)
            }
            
            MqttConfig.Topics.MOTOR_CURRENT -> {
                payload.toFloatOrNull()?.let { current ->
                    onCurrentReceived?.invoke(current)
                }
            }
            
            MqttConfig.Topics.MOTOR_VOLTAGE -> {
                payload.toFloatOrNull()?.let { voltage ->
                    onVoltageReceived?.invoke(voltage)
                }
            }
            
            MqttConfig.Topics.MOTOR_RAW -> {
                Log.d(TAG, "Raw motor data: $payload")
            }
        }
    }
    
    /**
     * Publicar comando al motor usando HiveMQ
     */
    fun publish(topic: String, message: String) {
        try {
            if (!isConnected()) {
                Log.w(TAG, "Cannot publish - not connected to MQTT broker")
                return
            }
            
            mqttClient?.publishWith()
                ?.topic(topic)
                ?.payload(message.toByteArray())
                ?.qos(MqttQos.AT_LEAST_ONCE)  // ✅ Sintaxis correcta HiveMQ
                ?.send()
                ?.whenComplete { publishResult, throwable ->
                    if (throwable != null) {
                        Log.e(TAG, "Failed to publish message to $topic", throwable)
                    } else {
                        MqttConfig.Debug.logCommand(message, topic)
                    }
                }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error publishing message", e)
        }
    }
    
    /**
     * Enviar comando de arranque suave (6 pasos)
     */
    fun sendArranque6P(values: List<Int>) {
        val command = MqttConfig.Commands.createArranque6P(values)
        publish(MqttConfig.Topics.MOTOR_COMMAND, command)
        publish(MqttConfig.Topics.MOTOR_TYPE, "arranque6p")
    }
    
    /**
     * Enviar comando de arranque continuo
     */
    fun sendContinuo() {
        publish(MqttConfig.Topics.MOTOR_COMMAND, MqttConfig.Commands.CONTINUO)
        publish(MqttConfig.Topics.MOTOR_TYPE, "continuo")
    }
    
    /**
     * Enviar comando de paro de emergencia
     */
    fun sendParo() {
        publish(MqttConfig.Topics.MOTOR_COMMAND, MqttConfig.Commands.PARO)
        publish(MqttConfig.Topics.MOTOR_TYPE, "paro")
    }
    
    // ============================================
    // CONFIGURACIÓN DE CALLBACKS
    // ============================================
    
    fun setOnSpeedReceived(callback: (Int) -> Unit) {
        onSpeedReceived = callback
    }
    
    fun setOnStatusReceived(callback: (String) -> Unit) {
        onStatusReceived = callback
    }
    
    fun setOnCurrentReceived(callback: (Float) -> Unit) {
        onCurrentReceived = callback
    }
    
    fun setOnVoltageReceived(callback: (Float) -> Unit) {
        onVoltageReceived = callback
    }
    
    // ============================================
    // ESTADO Y CONTROL
    // ============================================
    
    /**
     * Verificar si está conectado al broker MQTT
     */
    fun isConnected(): Boolean = mqttClient?.state?.isConnected == true
    
    /**
     * Desconectar del broker MQTT
     */
    fun disconnect() {
        try {
            mqttClient?.disconnect()?.whenComplete { _, throwable ->
                if (throwable != null) {
                    Log.e(TAG, "Error disconnecting from MQTT", throwable)
                } else {
                    Log.d(TAG, "MQTT Disconnected successfully")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting from MQTT", e)
        }
    }
    
    /**
     * Obtener información de conexión
     */
    fun getConnectionInfo(): String {
        return if (isConnected()) {
            "Connected to $currentServerUri"
        } else {
            "Disconnected"
        }
    }
    
    /**
     * Limpiar recursos
     */
    fun cleanup() {
        try {
            disconnect()
            mqttClient = null
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up MQTT service", e)
        }
    }
}
