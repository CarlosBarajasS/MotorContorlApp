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
    private var currentDeviceId: String
    private var subscribedDeviceId: String? = null

    init {
        MqttConfig.init(context)
        currentDeviceId = MqttConfig.getDeviceId()
    }
    
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
        val deviceId = currentDeviceId
        subscribedDeviceId = deviceId
        val topics = arrayOf(
            MqttConfig.Topics.speed(deviceId),
            MqttConfig.Topics.state(deviceId),
            MqttConfig.Topics.current(deviceId),
            MqttConfig.Topics.voltage(deviceId),
            MqttConfig.Topics.raw(deviceId),
            MqttConfig.Topics.type(deviceId)
        )
        
        topics.forEach { topicName ->
            mqttClient?.subscribeWith()
                ?.topicFilter(topicName)
                ?.qos(MqttQos.AT_LEAST_ONCE)
                ?.callback { publish ->
                    val topicStr = publish.topic.toString()
                    val payloadStr = String(publish.payloadAsBytes)
                    handleIncomingMessage(topicStr, payloadStr)
                }
                ?.send()
                ?.whenComplete { subAck, throwable ->
                    if (throwable != null) {
                        Log.e(TAG, "Failed to subscribe to $topicName", throwable)
                    } else {
                        Log.d(TAG, "Subscribed to $topicName successfully")
                    }
                }
        }
    }

    private fun unsubscribeFromDevice(deviceId: String?) {
        if (deviceId.isNullOrBlank()) return
        val client = mqttClient ?: return
        listOf(
            MqttConfig.Topics.speed(deviceId),
            MqttConfig.Topics.state(deviceId),
            MqttConfig.Topics.current(deviceId),
            MqttConfig.Topics.voltage(deviceId),
            MqttConfig.Topics.raw(deviceId),
            MqttConfig.Topics.type(deviceId)
        ).forEach { topic ->
            client.unsubscribeWith()
                .topicFilter(topic)
                .send()
                .whenComplete { _, throwable ->
                    if (throwable != null) {
                        Log.w(TAG, "Failed to unsubscribe from $topic", throwable)
                    }
                }
        }
    }

    fun updateDeviceId(deviceId: String) {
        val normalized = MqttConfig.normalizeDeviceId(deviceId)
        if (normalized == currentDeviceId) return
        val previous = currentDeviceId
        currentDeviceId = normalized
        MqttConfig.saveDeviceId(normalized)
        if (isConnected()) {
            unsubscribeFromDevice(previous)
            subscribeToTelemetryTopics()
        }
    }

    /**
     * Manejar mensajes entrantes del broker MQTT
     */
    private fun handleIncomingMessage(topicStr: String, payload: String) {
        MqttConfig.Debug.logTelemetry(topicStr, payload)
        
        val metric = topicStr.substringAfterLast('/')
        when (metric) {
            "speed" -> {
                payload.toIntOrNull()?.let { speed ->
                    onSpeedReceived?.invoke(speed)
                }
            }
            "state" -> {
                onStatusReceived?.invoke(payload)
            }
            "current" -> {
                payload.toFloatOrNull()?.let { current ->
                    onCurrentReceived?.invoke(current)
                }
            }
            "voltage" -> {
                payload.toFloatOrNull()?.let { voltage ->
                    onVoltageReceived?.invoke(voltage)
                }
            }
            "type" -> {
                onStatusReceived?.invoke(payload)
            }
            "raw" -> {
                Log.d(TAG, "Raw motor data: $payload")
            }
        }
    }
    
    /**
     * Publicar comando al motor usando HiveMQ
     */
    fun publish(topicName: String, message: String) {
        try {
            if (!isConnected()) {
                Log.w(TAG, "Cannot publish - not connected to MQTT broker")
                return
            }
            
            mqttClient?.publishWith()
                ?.topic(topicName)
                ?.payload(message.toByteArray())
                ?.qos(MqttQos.AT_LEAST_ONCE)
                ?.send()
                ?.whenComplete { publishResult, throwable ->
                    if (throwable != null) {
                        Log.e(TAG, "Failed to publish message to $topicName", throwable)
                    } else {
                        MqttConfig.Debug.logCommand(message, topicName)
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
        val command = MqttConfig.Commands.arranqueSuavePayload(values)
        val deviceId = currentDeviceId
        publish(MqttConfig.Topics.command(deviceId), command)
        publish(MqttConfig.Topics.type(deviceId), "arranque6p")
    }
    
    /**
     * Enviar comando de arranque continuo
     */
    fun sendContinuo() {
        val deviceId = currentDeviceId
        publish(MqttConfig.Topics.command(deviceId), MqttConfig.Commands.continuoPayload())
        publish(MqttConfig.Topics.type(deviceId), "continuo")
    }
    
    /**
     * Enviar comando de paro de emergencia
     */
    fun sendParo() {
        val deviceId = currentDeviceId
        publish(MqttConfig.Topics.command(deviceId), MqttConfig.Commands.paroPayload())
        publish(MqttConfig.Topics.type(deviceId), "paro")
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
            unsubscribeFromDevice(subscribedDeviceId)
            mqttClient?.disconnect()?.whenComplete { _, throwable ->
                if (throwable != null) {
                    Log.e(TAG, "Error disconnecting from MQTT", throwable)
                } else {
                    Log.d(TAG, "MQTT Disconnected successfully")
                }
            }
            subscribedDeviceId = null
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
