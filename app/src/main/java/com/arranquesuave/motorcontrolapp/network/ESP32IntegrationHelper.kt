package com.arranquesuave.motorcontrolapp.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * =====================================================================================
 * ESP32 INTEGRATION HELPER - OPTIMIZADO PARA CONFIGURACIÓN WIFI/BLUETOOTH [CORREGIDO]
 * =====================================================================================
 * Simplifica la integración entre la app Android y el ESP32
 * Compatible con ESP32_WiFi_Config_Complete.ino
 *
 * Funcionalidades:
 * ✅ Auto-detección de ESP32 en modo configuración y operativo
 * ✅ Configuración automática de WiFi
 * ✅ Recuperación de errores
 * ✅ Testing de conectividad
 * ✅ Estado en tiempo real
 *
 * ✅ CORREGIDO: Errores de compilación con ESP32ConfigResult
 * =====================================================================================
 */

@Serializable
data class ESP32DeviceInfo(
    val deviceName: String = "ESP32-MotorControl",
    val version: String = "1.0.0",
    val mode: String = "unknown", // configuration, operational, error
    val isOnline: Boolean = false,
    val ipAddress: String? = null,
    val signalStrength: Int = 0,
    val uptime: Long = 0,
    val freeHeap: Int = 0,
    val lastSeen: Long = System.currentTimeMillis()
)

@Serializable
data class ESP32ConfigResult(
    val success: Boolean,
    val message: String,
    val deviceInfo: ESP32DeviceInfo? = null,
    val error: String? = null,
    val nextStep: String? = null
)

enum class ESP32ConnectionState {
    DISCONNECTED,       // No hay conexión
    SEARCHING,          // Buscando ESP32
    CONFIG_MODE,        // ESP32 en modo configuración
    CONFIGURING,        // Enviando configuración WiFi
    WAITING_RESTART,    // Esperando reinicio del ESP32
    DISCOVERING,        // Buscando ESP32 en red operativa
    CONNECTED,          // Conectado y funcionando
    ERROR              // Error en el proceso
}

class ESP32IntegrationHelper(private val context: Context) {

    companion object {
        private const val TAG = "ESP32Integration"
        private const val CONFIG_TIMEOUT = 15000L // 15 segundos
        private const val DISCOVERY_TIMEOUT = 10000L // 10 segundos
        private const val PING_INTERVAL = 5000L // 5 segundos
    }

    private val esp32ConfigService = ESP32ConfigService(context)
    private val wifiService = WiFiService(context)
    private val networkConfigManager = NetworkConfigManagerUpdated(context)

    private val _connectionState = MutableStateFlow(ESP32ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ESP32ConnectionState> = _connectionState.asStateFlow()

    private val _deviceInfo = MutableStateFlow<ESP32DeviceInfo?>(null)
    val deviceInfo: StateFlow<ESP32DeviceInfo?> = _deviceInfo.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private var pingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * ✅ FUNCIÓN PRINCIPAL: Auto-setup completo del ESP32
     */
    suspend fun autoSetupESP32(): ESP32ConfigResult {
        return withContext(Dispatchers.IO) {
            try {
                _connectionState.value = ESP32ConnectionState.SEARCHING
                _lastError.value = null

                Log.d(TAG, "🔍 Iniciando auto-setup del ESP32...")

                // 1. Verificar si ya está configurado y funcionando
                val existingConfig = checkExistingConfiguration()
                if (existingConfig.success) {
                    return@withContext existingConfig
                }

                // 2. Buscar ESP32 en modo configuración
                val configModeResult = checkConfigurationMode()
                if (!configModeResult.success) {
                    return@withContext ESP32ConfigResult(
                        success = false,
                        message = "ESP32 no encontrado en modo configuración",
                        error = "ESP32 no encontrado. Verifica que esté encendido y en modo configuración.",
                        nextStep = "Reiniciar ESP32 y verificar LED parpadeando"
                    )
                }

                // 3. Obtener credenciales WiFi actuales
                val currentNetwork = wifiService.getCurrentNetwork()
                if (currentNetwork == null) {
                    return@withContext ESP32ConfigResult(
                        success = false,
                        message = "Celular no conectado a WiFi",
                        error = "Celular no está conectado a WiFi. Conecta a la red donde quieres que esté el ESP32.",
                        nextStep = "Conectar celular a red WiFi destino"
                    )
                }

                // 4. Configurar WiFi en ESP32 (requiere contraseña del usuario)
                return@withContext ESP32ConfigResult(
                    success = true,
                    message = "ESP32 encontrado en modo configuración. Listo para configurar WiFi.",
                    deviceInfo = configModeResult.deviceInfo,
                    nextStep = "input_wifi_password"
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error en auto-setup: ${e.message}", e)
                _connectionState.value = ESP32ConnectionState.ERROR
                _lastError.value = e.message

                return@withContext ESP32ConfigResult(
                    success = false,
                    message = "Error en auto-setup",
                    error = "Error inesperado: ${e.message}",
                    nextStep = "Reintentar o configurar manualmente"
                )
            }
        }
    }

    /**
     * ✅ Configura WiFi en ESP32 con las credenciales proporcionadas
     */
    suspend fun configureESP32WiFi(ssid: String, password: String): ESP32ConfigResult {
        return withContext(Dispatchers.IO) {
            try {
                _connectionState.value = ESP32ConnectionState.CONFIGURING

                Log.d(TAG, "📡 Configurando WiFi en ESP32: $ssid")

                // 1. Enviar credenciales al ESP32
                val credentials = WiFiCredentials(ssid, password, "WPA2")
                val configResponse = withTimeoutOrNull(CONFIG_TIMEOUT) {
                    esp32ConfigService.configureWiFi(credentials)
                }

                if (configResponse?.success != true) {
                    throw Exception("Error al enviar configuración: ${configResponse?.message ?: "Timeout"}")
                }

                _connectionState.value = ESP32ConnectionState.WAITING_RESTART
                Log.d(TAG, "⏳ Esperando que ESP32 se reinicie y conecte...")

                // 2. Esperar que el ESP32 se reinicie y conecte
                delay(8000) // Dar tiempo para reinicio y conexión

                _connectionState.value = ESP32ConnectionState.DISCOVERING

                // 3. Buscar ESP32 en la red operativa
                val discoveryResult = withTimeoutOrNull(DISCOVERY_TIMEOUT) {
                    discoverESP32InNetwork()
                }

                if (discoveryResult?.success == true) {
                    _connectionState.value = ESP32ConnectionState.CONNECTED
                    startPeriodicPing()

                    return@withContext ESP32ConfigResult(
                        success = true,
                        message = "✅ ESP32 configurado exitosamente y conectado a $ssid",
                        deviceInfo = discoveryResult.deviceInfo
                    )
                } else {
                    return@withContext ESP32ConfigResult(
                        success = false,
                        message = "ESP32 configurado pero no encontrado en red",
                        error = "ESP32 configurado pero no encontrado en red. Puede estar conectándose...",
                        nextStep = "Esperar 30 segundos y buscar manualmente por IP"
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error configurando WiFi: ${e.message}", e)
                _connectionState.value = ESP32ConnectionState.ERROR
                _lastError.value = e.message

                return@withContext ESP32ConfigResult(
                    success = false,
                    message = "Error al configurar WiFi",
                    error = "Error al configurar WiFi: ${e.message}",
                    nextStep = "Verificar credenciales y reintentar"
                )
            }
        }
    }

    /**
     * ✅ Verifica si hay una configuración existente que funcione
     */
    private suspend fun checkExistingConfiguration(): ESP32ConfigResult {
        return try {
            // Verificar configuración guardada
            if (!networkConfigManager.isConfigurationValid()) {
                return ESP32ConfigResult(
                    success = false,
                    message = "No hay configuración válida"
                )
            }

            // Probar conexión con configuración existente
            val esp32URL = networkConfigManager.getESP32URL()
            if (esp32URL != null) {
                val ip = esp32URL.substringAfter("//").substringBefore(":")
                val testResult = esp32ConfigService.testESP32Connection(ip)

                if (testResult) {
                    val status = esp32ConfigService.getStatus(ip) ?: ESP32Status(connected = true, ip = ip)
                    val deviceInfo = ESP32DeviceInfo(
                        mode = "operational",
                        isOnline = true,
                        ipAddress = ip,
                        signalStrength = status.signal ?: 0
                    )

                    _connectionState.value = ESP32ConnectionState.CONNECTED
                    _deviceInfo.value = deviceInfo
                    startPeriodicPing()

                    return ESP32ConfigResult(
                        success = true,
                        message = "✅ ESP32 ya está configurado y funcionando",
                        deviceInfo = deviceInfo
                    )
                }
            }

            ESP32ConfigResult(
                success = false,
                message = "Configuración existente no funciona"
            )

        } catch (e: Exception) {
            Log.w(TAG, "Error verificando configuración existente: ${e.message}")
            ESP32ConfigResult(
                success = false,
                message = "Error verificando configuración",
                error = e.message
            )
        }
    }

    /**
     * ✅ Verifica si ESP32 está en modo configuración
     */
    private suspend fun checkConfigurationMode(): ESP32ConfigResult {
        return try {
            val isAvailable = esp32ConfigService.isConfigModeAvailable()

            if (isAvailable) {
                val deviceInfo = ESP32DeviceInfo(
                    mode = "configuration",
                    isOnline = true,
                    ipAddress = "192.168.4.1"
                )

                _connectionState.value = ESP32ConnectionState.CONFIG_MODE
                _deviceInfo.value = deviceInfo

                return ESP32ConfigResult(
                    success = true,
                    message = "ESP32 encontrado en modo configuración",
                    deviceInfo = deviceInfo
                )
            } else {
                return ESP32ConfigResult(
                    success = false,
                    message = "ESP32 no está en modo configuración"
                )
            }

        } catch (e: Exception) {
            Log.w(TAG, "Error verificando modo configuración: ${e.message}")
            ESP32ConfigResult(
                success = false,
                message = "Error conectando a modo configuración",
                error = e.message
            )
        }
    }

    /**
     * ✅ Busca ESP32 en la red local después de la configuración
     */
    private suspend fun discoverESP32InNetwork(): ESP32ConfigResult {
        return try {
            val foundStatus = esp32ConfigService.findESP32InLocalNetwork()

            if (foundStatus != null && foundStatus.connected) {
                val deviceInfo = ESP32DeviceInfo(
                    mode = "operational",
                    isOnline = true,
                    ipAddress = foundStatus.ip,
                    signalStrength = foundStatus.signal ?: 0
                )

                // Guardar configuración
                foundStatus.ip?.let { ip ->
                    networkConfigManager.saveNetworkConfig(ip)
                }

                _deviceInfo.value = deviceInfo

                return ESP32ConfigResult(
                    success = true,
                    message = "ESP32 encontrado en red local",
                    deviceInfo = deviceInfo
                )
            } else {
                return ESP32ConfigResult(
                    success = false,
                    message = "ESP32 no encontrado en red local"
                )
            }

        } catch (e: Exception) {
            Log.w(TAG, "Error en discovery: ${e.message}")
            ESP32ConfigResult(
                success = false,
                message = "Error buscando ESP32 en red",
                error = e.message
            )
        }
    }

    /**
     * ✅ Configuración manual con IP específica
     */
    suspend fun configureManualIP(ipAddress: String): ESP32ConfigResult {
        return withContext(Dispatchers.IO) {
            try {
                _connectionState.value = ESP32ConnectionState.DISCOVERING

                // Validar IP
                if (!isValidIPAddress(ipAddress)) {
                    throw Exception("Formato de IP inválido: $ipAddress")
                }

                // Probar conexión
                val isConnected = esp32ConfigService.testESP32Connection(ipAddress)

                if (isConnected) {
                    val deviceInfo = ESP32DeviceInfo(
                        mode = "operational",
                        isOnline = true,
                        ipAddress = ipAddress,
                        signalStrength = 100 // Valor por defecto para configuración manual
                    )

                    // Guardar configuración
                    networkConfigManager.saveNetworkConfig(ipAddress)

                    _connectionState.value = ESP32ConnectionState.CONNECTED
                    _deviceInfo.value = deviceInfo
                    startPeriodicPing()

                    return@withContext ESP32ConfigResult(
                        success = true,
                        message = "✅ ESP32 conectado exitosamente con IP manual: $ipAddress",
                        deviceInfo = deviceInfo
                    )
                } else {
                    throw Exception("No se pudo conectar con $ipAddress")
                }

            } catch (e: Exception) {
                _connectionState.value = ESP32ConnectionState.ERROR
                _lastError.value = e.message

                return@withContext ESP32ConfigResult(
                    success = false,
                    message = "Error configuración manual",
                    error = "Error configuración manual: ${e.message}",
                    nextStep = "Verificar IP y que ESP32 esté encendido"
                )
            }
        }
    }

    /**
     * ✅ Inicia ping periódico para monitorear estado
     */
    private fun startPeriodicPing() {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (isActive) {
                try {
                    val currentDeviceInfo = _deviceInfo.value
                    val ipAddress = currentDeviceInfo?.ipAddress

                    if (ipAddress != null) {
                        val isOnline = esp32ConfigService.testESP32Connection(ipAddress)

                        _deviceInfo.value = currentDeviceInfo.copy(
                            isOnline = isOnline,
                            lastSeen = if (isOnline) System.currentTimeMillis() else currentDeviceInfo.lastSeen
                        )

                        if (!isOnline) {
                            _connectionState.value = ESP32ConnectionState.DISCONNECTED
                            Log.w(TAG, "⚠️ ESP32 desconectado: $ipAddress")
                        } else if (_connectionState.value != ESP32ConnectionState.CONNECTED) {
                            _connectionState.value = ESP32ConnectionState.CONNECTED
                            Log.d(TAG, "✅ ESP32 reconectado: $ipAddress")
                        }
                    }

                } catch (e: Exception) {
                    Log.w(TAG, "Error en ping periódico: ${e.message}")
                }

                delay(PING_INTERVAL)
            }
        }
    }

    /**
     * ✅ Utilitarios
     */
    private fun isValidIPAddress(ip: String): Boolean {
        return try {
            val parts = ip.split(".")
            parts.size == 4 && parts.all {
                val num = it.toIntOrNull()
                num != null && num in 0..255
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * ✅ Resetear configuración
     */
    fun resetConfiguration() {
        pingJob?.cancel()
        networkConfigManager.resetConfiguration()
        _connectionState.value = ESP32ConnectionState.DISCONNECTED
        _deviceInfo.value = null
        _lastError.value = null
    }

    /**
     * ✅ Obtener diagnóstico completo
     */
    fun getDiagnostics(): Map<String, Any> {
        val networkDiagnostics = networkConfigManager.getNetworkDiagnostics()
        val deviceInfo = _deviceInfo.value

        return networkDiagnostics + mapOf(
            "connectionState" to _connectionState.value.name,
            "deviceOnline" to (deviceInfo?.isOnline ?: false),
            "deviceIP" to (deviceInfo?.ipAddress ?: "Desconocido"),
            "lastError" to (_lastError.value ?: "Ninguno"),
            "lastSeen" to if (deviceInfo?.lastSeen != null) {
                java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                    .format(java.util.Date(deviceInfo.lastSeen))
            } else "Nunca"
        )
    }

    /**
     * ✅ Cleanup
     */
    fun cleanup() {
        pingJob?.cancel()
        scope.cancel()
    }
}
