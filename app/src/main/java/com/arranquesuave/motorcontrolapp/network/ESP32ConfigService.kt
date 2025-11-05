package com.arranquesuave.motorcontrolapp.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.arranquesuave.motorcontrolapp.config.MqttConfig
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Servicio encargado de la comunicación HTTP con el ESP32 tanto en modo de configuración
 * (AP propio) como cuando ya está integrado a la red local.
 */
class ESP32ConfigService(private val context: Context) {

    companion object {
        private const val TAG = "ESP32ConfigService"
        private const val ESP32_CONFIG_IP = "192.168.4.1"
        private const val ESP32_PORT = 80
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    private val wifiService = WiFiService(context)

    /**
     * Solicita al ESP32 (en modo configuración) el listado de redes disponibles.
     */
    suspend fun scanWiFiNetworks(): Result<List<String>> = suspendCoroutine { cont ->
        val request = Request.Builder()
            .url("http://$ESP32_CONFIG_IP:$ESP32_PORT/scan")
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Error escaneando redes WiFi", e)
                cont.resume(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    if (!response.isSuccessful) {
                        cont.resume(Result.failure(Exception("HTTP ${response.code}")))
                        return
                    }

                    val body = response.body?.string().orEmpty()
                    val json = JSONObject(body)
                    val array = json.optJSONArray("networks")
                    val result = mutableListOf<String>()
                    if (array != null) {
                        for (index in 0 until array.length()) {
                            val obj = array.optJSONObject(index)
                            if (obj != null) {
                                val ssid = obj.optString("ssid")
                                if (ssid.isNotBlank()) {
                                    result.add(ssid)
                                }
                            }
                        }
                    }
                    cont.resume(Result.success(result))
                } catch (e: Exception) {
                    cont.resume(Result.failure(e))
                } finally {
                    response.close()
                }
            }
        })
    }

    /**
     * Envía credenciales WiFi al ESP32 mientras está en modo configuración.
     */
    suspend fun configureWiFi(credentials: WiFiCredentials): ESP32ConfigResponse =
        withContext(Dispatchers.IO) {
            runCatching {
                val json = JSONObject().apply {
                    put("ssid", credentials.ssid)
                    put("password", credentials.password)
                    put("mqtt_broker", "177.247.175.4")
                    put("mqtt_port", 1885)
                    put("device_name", credentials.deviceName ?: MqttConfig.DEFAULT_DEVICE_ID)
                }

                val request = Request.Builder()
                    .url("http://$ESP32_CONFIG_IP:$ESP32_PORT/configure")
                    .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        ESP32ConfigResponse(true, "WiFi configurado exitosamente")
                    } else {
                        ESP32ConfigResponse(false, "Error HTTP ${response.code}")
                    }
                }
            }.getOrElse { error ->
                ESP32ConfigResponse(false, error.message ?: "Error configurando WiFi")
            }
        }

    suspend fun configureWiFi(ssid: String, password: String): ESP32ConfigResponse {
        return configureWiFi(WiFiCredentials(ssid, password))
    }

    /**
     * Verifica si el teléfono sigue conectado al AP temporal del ESP32.
     * Actualmente no se realiza un cambio de red automático; el usuario debe conectarse manualmente.
     */
    suspend fun connectToESP32Network(): Boolean = withContext(Dispatchers.IO) {
        val current = wifiService.getCurrentNetwork()
        current != null && (current.equals("ESP32-MotorSetup", ignoreCase = true)
                || current.contains("ESP32", ignoreCase = true))
    }

    /**
     * Comprueba si el ESP32 responde en modo configuración.
     */
    suspend fun isConfigModeAvailable(): Boolean = testDirectConnection()

    /**
     * Solicita el estado actual al ESP32 (independientemente del modo).
     */
    suspend fun getStatus(): ESP32Status = withContext(Dispatchers.IO) {
        requestStatus("http://$ESP32_CONFIG_IP:$ESP32_PORT/status")
            ?: ESP32Status(connected = false)
    }

    suspend fun getStatus(ipAddress: String): ESP32Status? = withContext(Dispatchers.IO) {
        requestStatus("http://$ipAddress:$ESP32_PORT/status")
    }

    /**
     * Intenta acceder al endpoint /status en el AP de configuración.
     */
    suspend fun testDirectConnection(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("http://$ESP32_CONFIG_IP:$ESP32_PORT/status")
                .build()

            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        }.getOrDefault(false)
    }

    /**
     * Verifica si el ESP32 responde en la IP proporcionada (modo operativo).
     */
    suspend fun testESP32Connection(ipAddress: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("http://$ipAddress:$ESP32_PORT/status")
                .build()

            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        }.getOrDefault(false)
    }

    /**
     * Intenta localizar el ESP32 en la red local usando la IP guardada previamente.
     */
    suspend fun findESP32InLocalNetwork(): ESP32Status? = withContext(Dispatchers.IO) {
        val manager = NetworkConfigManagerUpdated(context)
        val savedIP = manager.networkConfig.value.esp32IP
        if (savedIP.isNullOrBlank()) {
            return@withContext null
        }

        if (!testESP32Connection(savedIP)) {
            return@withContext null
        }

        requestStatus("http://$savedIP:$ESP32_PORT/status")?.copy(
            connected = true,
            ip = savedIP
        )
    }

    /**
     * Mejor esfuerzo para localizar al ESP32 dentro del rango de la red actual.
     * Se limita a unos pocos hosts para evitar bloquear la UI.
     */
    suspend fun findESP32InNetwork(baseIp: String): String? = withContext(Dispatchers.IO) {
        val hostsToProbe = (2..20)
        for (host in hostsToProbe) {
            val candidate = "$baseIp.$host"
            if (testESP32Connection(candidate)) {
                return@withContext candidate
            }
        }
        null
    }

    /**
     * En ocasiones el firmware devuelve la nueva IP después de configurar WiFi.
     * Se deja como un helper por si a futuro se implementa dentro del firmware.
     */
    suspend fun getNewIP(): String? = withContext(Dispatchers.IO) {
        // Intentar leer un endpoint hipotético /ip. Si falla, devolvemos null.
        runCatching {
            val request = Request.Builder()
                .url("http://$ESP32_CONFIG_IP:$ESP32_PORT/ip")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()?.trim()?.takeIf { it.isNotEmpty() }
                } else {
                    null
                }
            }
        }.getOrNull()
    }

    /**
     * Intenta determinar si el host con la IP dada está activo en la red.
     */
    suspend fun isHostReachable(ip: String, timeout: Int = 500): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            InetAddress.getByName(ip).isReachable(timeout)
        }.getOrDefault(false)
    }

    /**
     * Recupera el estado e intenta mapearlo a [ESP32Status].
     */
    private fun requestStatus(url: String): ESP32Status? {
        return try {
            val request = Request.Builder().url(url).build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string().orEmpty()
                parseStatus(body)
            }
        } catch (e: SocketTimeoutException) {
            Log.w(TAG, "Timeout consultando estado en $url")
            null
        } catch (e: Exception) {
            Log.w(TAG, "Error consultando estado: ${e.message}")
            null
        }
    }

    private fun parseStatus(body: String): ESP32Status {
        return try {
            val json = JSONObject(body)
            ESP32Status(
                connected = json.optBoolean("wifi_connected", json.optBoolean("connected", false)),
                ssid = json.optString("ssid", null),
                ip = json.optString("ip_address", json.optString("ip", null)).takeIf { it?.isNotBlank() == true },
                signal = when {
                    json.has("signal") -> json.optInt("signal")
                    json.has("wifi_signal") -> json.optInt("wifi_signal")
                    else -> null
                },
                firmwareVersion = json.optString("firmware", null),
                deviceName = json.optString("device_name", null)
            )
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo parsear estado del ESP32", e)
            ESP32Status(connected = false)
        }
    }
}
