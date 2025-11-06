package com.arranquesuave.motorcontrolapp.config

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * =====================================================================================
 * MQTT CONFIG - CONFIGURACIÓN COMPLETA PARA COMPILACIÓN
 * =====================================================================================
 * Configuración completa para satisfacer todas las dependencias
 * Incluye todos los objetos y métodos requeridos por MqttService y MotorViewModel
 * =====================================================================================
 */

object MqttConfig {
    
    const val DEFAULT_DEVICE_ID = "MotorController"
    private const val PREFS_NAME = "mqtt_config"
    private const val KEY_DEVICE_ID = "device_id"
    
    // ✅ CONFIGURACIÓN BÁSICA MQTT - BROKER DEL PROFESOR
    const val serverHost = "177.247.175.4"  // IP del profesor
    const val serverPort = 1885              // Puerto del profesor
    const val qos = 1
    const val retained = false
    const val automaticReconnect = true
    const val cleanSession = true
    const val connectionTimeout = 30
    const val keepAliveInterval = 60
    const val maxInflight = 10
    
    // ✅ PROPIEDADES REQUERIDAS POR OTROS ARCHIVOS
    const val topic = "motor/control"
    const val payloadAsBytes = false
    
    // ✅ URLs PARA DIFERENTES MODOS DE CONEXIÓN
    const val MQTT_BROKER_URL = "tcp://177.247.175.4:1885"  // Broker principal del profesor
    const val MQTT_TEST_URL = "tcp://test.mosquitto.org:1883"  // Para pruebas si es necesario
    
    // ✅ CONFIGURACIÓN DINÁMICA
    private var preferences: SharedPreferences? = null
    
    // ✅ INICIALIZACIÓN CON CONTEXTO
    fun init(context: Context) {
        if (preferences == null) {
            preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }
    
    // ✅ FUNCIONES MÍNIMAS REQUERIDAS
    fun getServerUrl(): String = "tcp://$serverHost:$serverPort"
    
    // ✅ OBTENER URL DEL BROKER PRINCIPAL
    fun getBrokerUrl(): String = MQTT_BROKER_URL
    
    fun getClientConfig(): Map<String, Any> = mapOf(
        "serverHost" to serverHost,
        "serverPort" to serverPort,
        "clientId" to "MotorControlApp",
        "cleanSession" to cleanSession,
        "connectionTimeout" to connectionTimeout,
        "keepAliveInterval" to keepAliveInterval,
        "automaticReconnect" to automaticReconnect
    )
    
    // ✅ TOPICS MQTT
    object Topics {
        fun command(deviceId: String) = "motor/$deviceId/command"
        fun speed(deviceId: String) = "motor/$deviceId/speed"
        fun speedCommand(deviceId: String) = "motor/$deviceId/speed/set"
        fun state(deviceId: String) = "motor/$deviceId/state"
        fun current(deviceId: String) = "motor/$deviceId/current"
        fun voltage(deviceId: String) = "motor/$deviceId/voltage"
        fun raw(deviceId: String) = "motor/$deviceId/raw"
        fun type(deviceId: String) = "motor/$deviceId/type"
    }
    
    // ✅ COMANDOS MQTT
    object Commands {
        private val suffixes = listOf('a', 'b', 'c', 'd', 'e', 'f')
        
        fun arranqueSuavePayload(values: List<Int>): String {
            return values.take(6).mapIndexed { index, value ->
                "${value.coerceIn(0, 254)}${suffixes.getOrElse(index) { 'f' }}"
            }.joinToString(",")
        }
        
        fun continuoPayload(): String = "0i"
        
        fun paroPayload(): String = "0p"
    }
    
    // ✅ UTILIDADES
    object Utils {
        fun generateClientId(): String {
            return "MotorControlApp_${System.currentTimeMillis()}"
        }
    }

    // ✅ DEBUG Y LOGGING
    object Debug {
        fun logTelemetry(topic: String, payload: String) {
            Log.d("MqttTelemetry", "[$topic] $payload")
        }
        
        fun logCommand(command: String, topic: String) {
            Log.d("MqttCommand", "[$topic] $command")
        }
    }

    fun saveDeviceId(deviceId: String) {
        val normalized = normalizeDeviceId(deviceId)
        preferences?.edit()?.putString(KEY_DEVICE_ID, normalized)?.apply()
    }

    fun getDeviceId(): String {
        val stored = preferences?.getString(KEY_DEVICE_ID, null)
        val normalized = normalizeDeviceId(stored)
        if (stored != normalized) {
            saveDeviceId(normalized)
        }
        return normalized
    }

    fun normalizeDeviceId(raw: String?): String {
        val sanitized = raw
            ?.lowercase()
            ?.replace("[^a-z0-9_-]".toRegex(), "-")
            ?.trim('-')
        return if (sanitized.isNullOrBlank()) DEFAULT_DEVICE_ID else sanitized
    }
}
