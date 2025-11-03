package com.arranquesuave.motorcontrolapp.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Serializable
data class WiFiCredentials(
    val ssid: String,
    val password: String,
    val security: String = "WPA2"
)

@Serializable
data class ESP32Status(
    val connected: Boolean,
    val ssid: String? = null,
    val ip: String? = null,
    val signal: Int? = null,
    val error: String? = null
)

@Serializable
data class ESP32Response(
    val success: Boolean,
    val message: String,
    val data: Map<String, String>? = null
)

class ESP32ConfigService {
    
    companion object {
        private const val ESP32_CONFIG_IP = "192.168.4.1"
        private const val CONFIG_PORT = 80
        private const val TIMEOUT_SECONDS = 10L
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    /**
     * Configura las credenciales WiFi en el ESP32
     */
    suspend fun configureWiFi(credentials: WiFiCredentials): ESP32Response {
        return try {
            val jsonBody = json.encodeToString(WiFiCredentials.serializer(), credentials)
            val request = Request.Builder()
                .url("http://$ESP32_CONFIG_IP:$CONFIG_PORT/configure")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).executeAsync()
            val responseBody = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                json.decodeFromString(ESP32Response.serializer(), responseBody)
            } else {
                ESP32Response(
                    success = false,
                    message = "Error HTTP ${response.code}: ${response.message}"
                )
            }
        } catch (e: Exception) {
            ESP32Response(
                success = false,
                message = "Error de conexión: ${e.message}"
            )
        }
    }
    
    /**
     * Obtiene el estado actual del ESP32
     */
    suspend fun getStatus(): ESP32Status {
        return try {
            val request = Request.Builder()
                .url("http://$ESP32_CONFIG_IP:$CONFIG_PORT/status")
                .get()
                .build()
            
            val response = client.newCall(request).executeAsync()
            val responseBody = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                json.decodeFromString(ESP32Status.serializer(), responseBody)
            } else {
                ESP32Status(
                    connected = false,
                    error = "Error HTTP ${response.code}: ${response.message}"
                )
            }
        } catch (e: Exception) {
            ESP32Status(
                connected = false,
                error = "Error de conexión: ${e.message}"
            )
        }
    }
    
    /**
     * Verifica si el ESP32 está disponible en modo configuración
     */
    suspend fun isConfigModeAvailable(): Boolean {
        return try {
            val request = Request.Builder()
                .url("http://$ESP32_CONFIG_IP:$CONFIG_PORT/ping")
                .get()
                .build()
            
            val response = client.newCall(request).executeAsync()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Reinicia el ESP32
     */
    suspend fun restart(): ESP32Response {
        return try {
            val request = Request.Builder()
                .url("http://$ESP32_CONFIG_IP:$CONFIG_PORT/restart")
                .post("".toRequestBody())
                .build()
            
            val response = client.newCall(request).executeAsync()
            val responseBody = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                json.decodeFromString(ESP32Response.serializer(), responseBody)
            } else {
                ESP32Response(
                    success = false,
                    message = "Error HTTP ${response.code}: ${response.message}"
                )
            }
        } catch (e: Exception) {
            ESP32Response(
                success = false,
                message = "Error de conexión: ${e.message}"
            )
        }
    }
    
    /**
     * Obtiene la nueva IP del ESP32 después de conectarse a WiFi
     */
    suspend fun getNewIP(): String? {
        val status = getStatus()
        return if (status.connected) status.ip else null
    }
    
    /**
     * Busca el ESP32 en la red local después de la configuración
     */
    suspend fun findESP32InNetwork(baseIP: String = "192.168.1"): String? {
        // Buscar en el rango de IPs de la red local
        for (i in 100..254) {
            val testIP = "$baseIP.$i"
            if (testESP32Connection(testIP)) {
                return testIP
            }
        }
        return null
    }
    
    /**
     * Prueba la conexión con una IP específica
     */
    private suspend fun testESP32Connection(ip: String): Boolean {
        return try {
            val request = Request.Builder()
                .url("http://$ip:$CONFIG_PORT/ping")
                .get()
                .build()
            
            val response = client.newCall(request).executeAsync()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Extensión para convertir las llamadas OkHttp en suspending functions
 */
private suspend fun Call.executeAsync(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            continuation.resumeWithException(e)
        }
        
        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response)
        }
    })
}
