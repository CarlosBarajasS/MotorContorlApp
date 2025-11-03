package com.arranquesuave.motorcontrolapp.config

import android.content.Context
import kotlinx.coroutines.flow.first
import com.arranquesuave.motorcontrolapp.utils.NetworkConfigManager

/**
 * CONFIGURACIÓN MQTT PARA MOTOR CONTROL APP
 * 
 * Proyecto: Sistema de Control de Arranque Suave para Motores CD con IoT
 * Instituto: Tecnológico de Morelia
 * Fecha: Octubre 2025
 * 
 * NUEVO: Soporte para configuración WiFi local personalizable
 */
class MqttConfig(private val context: Context) {
    
    private val networkConfigManager = NetworkConfigManager(context)
    
    // ✅ URLs FIJAS (no cambian)
    companion object {
        // 🌐 Remoto - Desde tu casa (Internet) ✅ FUNCIONA
        const val MQTT_REMOTE_URL = "tcp://177.247.175.4:1885"
        
        // 🧪 Testing - Desarrollo sin hardware
        const val MQTT_TEST_URL = "tcp://test.mosquitto.org:1883"
    }
    
    // ✅ URL LOCAL DINÁMICA (configurada por usuario)
    suspend fun getMqttLocalUrl(): String {
        val config = networkConfigManager.localNetworkConfig.first()
        return config.getMqttUrl()
    }
    
    suspend fun getLocalNetworkInfo(): String {
        val config = networkConfigManager.localNetworkConfig.first()
        return config.networkName
    }
    
    // ============================================
    // TOPICS MQTT DEFINIDOS
    // ============================================
    
    // Comandos hacia ESP32
    object Topics {
        const val MOTOR_COMMAND = "motor/control/command"
        const val MOTOR_TYPE = "motor/control/type"
        
        // Telemetría desde ESP32
        const val MOTOR_SPEED = "motor/status/speed"
        const val MOTOR_CURRENT = "motor/status/current"
        const val MOTOR_VOLTAGE = "motor/status/voltage"
        const val MOTOR_STATE = "motor/status/state"
        const val MOTOR_RAW = "motor/status/raw"
        
        // Configuración
        const val MOTOR_CONFIG_PID = "motor/config/pid"
        const val MOTOR_CONFIG_LOGISTIC = "motor/config/logistic"
    }
    
    // ============================================
    // CONFIGURACIÓN DE CLIENTE MQTT
    // ============================================
    object ClientConfig {
        const val CLIENT_ID_PREFIX = "MotorControlApp"
        const val KEEP_ALIVE_INTERVAL = 60 // segundos
        const val CONNECTION_TIMEOUT = 30 // segundos
        const val CLEAN_SESSION = true
        const val AUTO_RECONNECT = true
        const val QOS_LEVEL = 1 // At least once delivery
    }
    
    // ============================================
    // COMANDOS DEL MOTOR (PROTOCOLO ASCII)
    // ============================================
    object Commands {
        // Formato: "50a,100b,150c,200d,250e,254f"
        fun createArranque6P(values: List<Int>): String {
            return values.take(6).mapIndexed { index, value ->
                "$value${('a' + index)}"
            }.joinToString(",")
        }
        
        // Arranque continuo
        const val CONTINUO = "0i,"
        
        // Paro de emergencia
        const val PARO = "0p,"
    }
    
    // ============================================
    // MAPEO DE MODOS DE CONEXIÓN
    // ============================================
    enum class ConnectionMode(val description: String) {
        LOCAL("Red local personalizada"),
        REMOTE("Internet desde tu casa"),
        TEST("Testing sin hardware")
    }
    
    // ============================================
    // UTILIDADES
    // ============================================
    object Utils {
        fun generateClientId(): String {
            return "${ClientConfig.CLIENT_ID_PREFIX}_${System.currentTimeMillis()}"
        }
        
        fun validateCommand(command: String): Boolean {
            return when {
                command == Commands.CONTINUO -> true
                command == Commands.PARO -> true
                command.matches(Regex("""^\d+[a-f](,\d+[a-f])*$""")) -> true
                else -> false
            }
        }
        
        fun parseArranque6P(command: String): List<Int>? {
            return try {
                command.split(",").map { part ->
                    part.dropLast(1).toInt() // Remove letter, get number
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    // ============================================
    // CONFIGURACIÓN DE TIMEOUT
    // ============================================
    object Timeouts {
        const val COMMAND_RESPONSE_TIMEOUT = 5000L // 5 segundos
        const val TELEMETRY_TIMEOUT = 10000L // 10 segundos
        const val CONNECTION_RETRY_DELAY = 2000L // 2 segundos
        const val MAX_RETRY_ATTEMPTS = 3
    }
    
    // ============================================
    // LOGGING Y DEBUG
    // ============================================
    object Debug {
        const val ENABLE_MQTT_LOGGING = true
        const val LOG_TAG = "MqttMotorControl"
        
        fun logCommand(command: String, topic: String) {
            if (ENABLE_MQTT_LOGGING) {
                android.util.Log.d(LOG_TAG, "MQTT Publish: $topic = $command")
            }
        }
        
        fun logTelemetry(topic: String, payload: String) {
            if (ENABLE_MQTT_LOGGING) {
                android.util.Log.d(LOG_TAG, "MQTT Received: $topic = $payload")
            }
        }
    }
}
