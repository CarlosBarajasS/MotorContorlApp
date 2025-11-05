package com.arranquesuave.motorcontrolapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arranquesuave.motorcontrolapp.network.*
import com.arranquesuave.motorcontrolapp.config.MqttConfig
import java.util.UUID
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

data class WiFiSetupState(
    val isScanning: Boolean = false,
    val availableNetworks: List<WiFiNetwork> = emptyList(),
    val selectedNetwork: WiFiNetwork? = null,
    val isConfiguring: Boolean = false,
    val configurationStep: ConfigurationStep = ConfigurationStep.SCANNING,
    val progress: Float = 0f,
    val statusMessage: String = "",
    val error: String? = null,
    val isCompleted: Boolean = false,
    val esp32Status: ESP32Status? = null
)

enum class ConfigurationStep {
    SCANNING,           // Escaneando redes WiFi
    NETWORK_SELECTION,  // Seleccionando red  
    PASSWORD_INPUT,     // Ingresando contraseña
    ESP32_CONNECTION,   // Conectando al ESP32
    WIFI_CONFIG,        // Configurando WiFi en ESP32
    WAITING_CONNECTION, // Esperando que ESP32 se conecte
    NETWORK_DISCOVERY,  // Buscando ESP32 en la nueva red
    COMPLETED,          // Configuración completada
    ERROR              // Error en el proceso
}

class WiFiSetupViewModel(application: Application) : AndroidViewModel(application) {
    
    private val wifiService = WiFiService(application)
    private val esp32ConfigService = ESP32ConfigService(application)
    private val networkConfigManager = NetworkConfigManagerUpdated(application)
    
    private val _state = MutableStateFlow(WiFiSetupState())
    val state: StateFlow<WiFiSetupState> = _state.asStateFlow()
    
    init {
        // ✅ DETECTAR AUTOMÁTICAMENTE EL ESP32
        detectESP32AutomaticallyStart()
    }

    private fun generateDeviceId(): String {
        return MqttConfig.normalizeDeviceId("esp32-" + UUID.randomUUID().toString().take(8))
    }
    
    /**
     * ✅ NUEVO FLUJO: Detectar automáticamente ESP32 sin timeouts muy largos
     */
    private fun detectESP32AutomaticallyStart() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isScanning = true,
                configurationStep = ConfigurationStep.SCANNING,
                statusMessage = "🔍 Detectando ESP32...",
                progress = 0.1f
            )
            
            try {
                // 1️⃣ VERIFICAR SI YA ESTÁ EN RED LOCAL (5 segundos máximo)
                _state.value = _state.value.copy(
                    statusMessage = "🔍 Buscando ESP32 en red local...",
                    progress = 0.2f
                )
                
                val localESP32 = withTimeoutOrNull(5000) {
                    esp32ConfigService.findESP32InLocalNetwork()
                }
                
                if (localESP32 != null) {
                    localESP32.deviceName?.let { MqttConfig.saveDeviceId(it) }
                    // ✅ ENCONTRADO EN RED LOCAL
                    _state.value = _state.value.copy(
                        isScanning = false,
                        configurationStep = ConfigurationStep.COMPLETED,
                        statusMessage = "✅ ESP32 encontrado y funcional",
                        progress = 1.0f,
                        isCompleted = true,
                        esp32Status = localESP32
                    )
                    
                    // Guardar configuración automáticamente
                    localESP32.ip?.let { ip ->
                        networkConfigManager.saveNetworkConfig(ip)
                    }
                    return@launch
                }
                
                // 2️⃣ VERIFICAR MODO CONFIGURACIÓN (5 segundos máximo)
                _state.value = _state.value.copy(
                    statusMessage = "🔍 Verificando modo configuración...",
                    progress = 0.4f
                )
                
                val isConfigMode = withTimeoutOrNull(5000) {
                    // Primero intentar conectar a la red ESP32
                    val connected = esp32ConfigService.connectToESP32Network()
                    if (connected) {
                        delay(2000) // Dar tiempo para que se establezca la conexión
                        esp32ConfigService.isConfigModeAvailable()
                    } else {
                        // Probar conexión directa
                        esp32ConfigService.testDirectConnection()
                    }
                }
                
                if (isConfigMode == true) {
                    // ✅ ESP32 EN MODO CONFIGURACIÓN
                    _state.value = _state.value.copy(
                        statusMessage = "✅ ESP32 detectado en modo configuración",
                        progress = 0.6f
                    )
                    
                    delay(1000)
                    
                    // Continuar con escaneo de redes WiFi
                    startWiFiScanning()
                } else {
                    // ❌ NO DETECTADO - MOSTRAR OPCIONES MANUALES
                    _state.value = _state.value.copy(
                        isScanning = false,
                        configurationStep = ConfigurationStep.NETWORK_SELECTION,
                        statusMessage = "⚠️ ESP32 no detectado automáticamente",
                        progress = 0.0f,
                        availableNetworks = emptyList() // Mostrar opciones manuales
                    )
                }
                
            } catch (e: Exception) {
                // ERROR EN DETECCIÓN - PERMITIR CONFIGURACIÓN MANUAL
                _state.value = _state.value.copy(
                    isScanning = false,
                    configurationStep = ConfigurationStep.NETWORK_SELECTION,
                    statusMessage = "⚠️ Error en detección automática",
                    progress = 0.0f,
                    error = null, // No mostrar como error
                    availableNetworks = emptyList()
                )
            }
        }
    }
    
    /**
     * ✅ ESCANEAR REDES WIFI
     */
    fun startWiFiScanning() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isScanning = true,
                configurationStep = ConfigurationStep.SCANNING,
                statusMessage = "📡 Escaneando redes WiFi...",
                error = null
            )
            
            try {
                if (!wifiService.isWiFiEnabled()) {
                    wifiService.enableWiFi()
                    delay(2000)
                }
                
                val networks = wifiService.scanWiFiNetworks()
                
                _state.value = _state.value.copy(
                    isScanning = false,
                    availableNetworks = networks,
                    configurationStep = ConfigurationStep.NETWORK_SELECTION,
                    statusMessage = "📱 Selecciona tu red WiFi doméstica",
                    progress = 0.3f
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isScanning = false,
                    configurationStep = ConfigurationStep.ERROR,
                    error = "Error al escanear redes: ${e.message}",
                    statusMessage = "Error en el escaneo"
                )
            }
        }
    }
    
    /**
     * ✅ SELECCIONAR RED WIFI
     */
    fun selectNetwork(network: WiFiNetwork) {
        _state.value = _state.value.copy(
            selectedNetwork = network,
            configurationStep = if (network.isSecure) {
                ConfigurationStep.PASSWORD_INPUT
            } else {
                ConfigurationStep.ESP32_CONNECTION
            },
            statusMessage = if (network.isSecure) {
                "🔐 Ingresa la contraseña para ${network.ssid}"
            } else {
                "📡 Red abierta seleccionada: ${network.ssid}"
            },
            progress = 0.5f,
            error = null
        )
        
        // Si es red abierta, proceder automáticamente
        if (!network.isSecure) {
            configureWiFi("")
        }
    }
    
    /**
     * ✅ CONFIGURAR WIFI EN ESP32 (CORREGIDO)
     */
    fun configureWiFi(password: String) {
        val selectedNetwork = _state.value.selectedNetwork ?: return
        
        // ✅ CONFIGURACIÓN MANUAL DE IP
        if (selectedNetwork.ssid == "CONFIGURACIÓN_MANUAL") {
            configureManualIP(password)
            return
        }
        
        // ✅ CONFIGURACIÓN NORMAL DE WIFI
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isConfiguring = true,
                configurationStep = ConfigurationStep.ESP32_CONNECTION,
                statusMessage = "🔌 Conectando al ESP32...",
                progress = 0.6f,
                error = null
            )
            
            try {
                // 1️⃣ ASEGURAR CONEXIÓN A ESP32
                val esp32Connected = esp32ConfigService.connectToESP32Network()
                if (!esp32Connected) {
                    // Verificar si el ESP32 ya está configurado y disponible en la red local
                    val alreadyConfigured = esp32ConfigService.findESP32InLocalNetwork()
                    if (alreadyConfigured != null) {
                        alreadyConfigured.ip?.let { ip ->
                            networkConfigManager.saveNetworkConfig(ip)
                        }
                        alreadyConfigured.deviceName?.let { MqttConfig.saveDeviceId(it) }

                        _state.value = _state.value.copy(
                            isConfiguring = false,
                            configurationStep = ConfigurationStep.COMPLETED,
                            statusMessage = "✅ ESP32 ya estaba configurado y accesible en la red.",
                            progress = 1.0f,
                            isCompleted = true,
                            esp32Status = alreadyConfigured
                        )
                        return@launch
                    } else {
                        throw Exception("No se pudo conectar a la red ESP32-MotorConfig. Asegúrate de estar conectado.")
                    }
                }
                
                delay(2000) // Dar tiempo para establecer conexión
                
                // 2️⃣ VERIFICAR QUE ESP32 RESPONDA
                _state.value = _state.value.copy(
                    statusMessage = "✅ Verificando comunicación con ESP32...",
                    progress = 0.65f
                )
                
                if (!esp32ConfigService.isConfigModeAvailable()) {
                    throw Exception("ESP32 no responde. Verifica que esté encendido y en modo configuración.")
                }
                
                // 3️⃣ ENVIAR CONFIGURACIÓN WIFI
                _state.value = _state.value.copy(
                    configurationStep = ConfigurationStep.WIFI_CONFIG,
                    statusMessage = "⚙️ Enviando configuración WiFi...",
                    progress = 0.7f
                )
                
                val deviceId = generateDeviceId()
                val credentials = WiFiCredentials(
                    ssid = selectedNetwork.ssid,
                    password = password,
                    security = if (selectedNetwork.isSecure) "WPA2" else "OPEN",
                    deviceName = deviceId
                )
                
                val response = esp32ConfigService.configureWiFi(credentials)

                if (!response.success) {
                    throw Exception("Error configurando WiFi: ${response.message}")
                }

                MqttConfig.saveDeviceId(deviceId)
                
                // 4️⃣ ESPERAR REINICIO Y CONEXIÓN DEL ESP32
                _state.value = _state.value.copy(
                    configurationStep = ConfigurationStep.WAITING_CONNECTION,
                    statusMessage = "⏳ ESP32 reiniciando y conectando a ${selectedNetwork.ssid}...",
                    progress = 0.8f
                )
                
                delay(15000) // Dar 15 segundos para que ESP32 se conecte
                
                // 5️⃣ BUSCAR ESP32 EN LA NUEVA RED
                _state.value = _state.value.copy(
                    configurationStep = ConfigurationStep.NETWORK_DISCOVERY,
                    statusMessage = "🔍 Buscando ESP32 en tu red WiFi...",
                    progress = 0.9f
                )
                
                val foundESP32 = withTimeoutOrNull(20000) {
                    esp32ConfigService.findESP32InLocalNetwork()
                }
                
                if (foundESP32 != null) {
                    // ✅ CONFIGURACIÓN EXITOSA
                    foundESP32.ip?.let { ip ->
                        networkConfigManager.saveNetworkConfig(ip)
                    }
                    foundESP32.deviceName?.let { MqttConfig.saveDeviceId(it) }

                    _state.value = _state.value.copy(
                        isConfiguring = false,
                        configurationStep = ConfigurationStep.COMPLETED,
                        statusMessage = "🎉 ¡Configuración exitosa!",
                        progress = 1.0f,
                        isCompleted = true,
                        esp32Status = foundESP32
                    )
                } else {
                    throw Exception("ESP32 configurado pero no encontrado en la red. Verifica la configuración WiFi.")
                }
                
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isConfiguring = false,
                    configurationStep = ConfigurationStep.ERROR,
                    error = e.message,
                    statusMessage = "❌ Error en configuración",
                    progress = 0f
                )
            }
        }
    }
    
    /**
     * ✅ CONFIGURACIÓN MANUAL DE IP
     */
    private fun configureManualIP(ipAddress: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isConfiguring = true,
                configurationStep = ConfigurationStep.WIFI_CONFIG,
                statusMessage = "🔧 Probando IP manual: $ipAddress",
                progress = 0.7f,
                error = null
            )
            
            try {
                if (!isValidIP(ipAddress)) {
                    throw Exception("Formato de IP inválido. Ejemplo: 192.168.1.100")
                }
                
                _state.value = _state.value.copy(
                    statusMessage = "📡 Conectando con $ipAddress...",
                    progress = 0.8f
                )
                
                val testResult = withTimeoutOrNull(8000) {
                    esp32ConfigService.testESP32Connection(ipAddress)
                }
                
                if (testResult == true) {
                    // ✅ CONEXIÓN EXITOSA
                    networkConfigManager.saveNetworkConfig(ipAddress)
                    val status = esp32ConfigService.getStatus(ipAddress)
                    status?.deviceName?.let { MqttConfig.saveDeviceId(it) }
                    
                    _state.value = _state.value.copy(
                        isConfiguring = false,
                        configurationStep = ConfigurationStep.COMPLETED,
                        statusMessage = "✅ ESP32 configurado manualmente",
                        progress = 1.0f,
                        isCompleted = true,
                        esp32Status = status ?: ESP32Status(
                            connected = true,
                            ip = ipAddress,
                            ssid = "Configuración manual",
                            signal = 100
                        )
                    )
                } else {
                    throw Exception("No se pudo conectar con $ipAddress. Verifica que el ESP32 esté encendido y accesible.")
                }
                
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isConfiguring = false,
                    configurationStep = ConfigurationStep.ERROR,
                    error = e.message,
                    statusMessage = "❌ Error en configuración manual",
                    progress = 0f
                )
            }
        }
    }
    
    /**
     * ✅ VALIDAR FORMATO IP
     */
    private fun isValidIP(ip: String): Boolean {
        return try {
            val parts = ip.split(".")
            if (parts.size != 4) return false
            
            parts.all { part ->
                val num = part.toIntOrNull()
                num != null && num in 0..255
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * ✅ REINTENTAR CONFIGURACIÓN
     */
    fun retryConfiguration() {
        _state.value = WiFiSetupState()
        detectESP32AutomaticallyStart()
    }
    
    /**
     * ✅ CANCELAR CONFIGURACIÓN
     */
    fun cancelConfiguration() {
        _state.value = _state.value.copy(
            isConfiguring = false,
            configurationStep = ConfigurationStep.NETWORK_SELECTION,
            statusMessage = "Configuración cancelada",
            progress = 0.3f,
            error = null
        )
    }
    
    /**
     * ✅ VOLVER ATRÁS
     */
    fun goBack() {
        when (_state.value.configurationStep) {
            ConfigurationStep.PASSWORD_INPUT -> {
                _state.value = _state.value.copy(
                    configurationStep = ConfigurationStep.NETWORK_SELECTION,
                    statusMessage = "Selecciona tu red WiFi",
                    progress = 0.3f,
                    error = null
                )
            }
            ConfigurationStep.NETWORK_SELECTION -> {
                startWiFiScanning()
            }
            ConfigurationStep.ERROR -> {
                _state.value = _state.value.copy(
                    configurationStep = ConfigurationStep.NETWORK_SELECTION,
                    statusMessage = "Selecciona tu red WiFi",
                    progress = 0.3f,
                    error = null
                )
            }
            else -> {
                retryConfiguration()
            }
        }
    }
    
    /**
     * ✅ ACTUALIZAR REDES
     */
    fun refreshNetworks() {
        if (!_state.value.isScanning && !_state.value.isConfiguring) {
            startWiFiScanning()
        }
    }
    
    /**
     * ✅ SALTAR A MODO OPERATIVO (USAR ESP32 YA CONFIGURADO)
     */
    fun skipToOperationalMode() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isConfiguring = true,
                configurationStep = ConfigurationStep.NETWORK_DISCOVERY,
                statusMessage = "🔍 Buscando ESP32 configurado...",
                progress = 0.5f,
                error = null
            )
            
            try {
                val foundESP32 = withTimeoutOrNull(10000) {
                    esp32ConfigService.findESP32InLocalNetwork()
                }
                
                if (foundESP32 != null) {
                    // ✅ ESP32 ENCONTRADO
                    foundESP32.ip?.let { ip ->
                        networkConfigManager.saveNetworkConfig(ip)
                    }
                    foundESP32.deviceName?.let { MqttConfig.saveDeviceId(it) }

                    _state.value = _state.value.copy(
                        isConfiguring = false,
                        configurationStep = ConfigurationStep.COMPLETED,
                        statusMessage = "✅ ESP32 detectado y listo",
                        progress = 1.0f,
                        isCompleted = true,
                        esp32Status = foundESP32
                    )
                } else {
                    beginManualConfiguration()
                }
                
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isConfiguring = false,
                    configurationStep = ConfigurationStep.ERROR,
                    error = e.message,
                    statusMessage = "❌ Error detectando ESP32",
                    progress = 0f
                )
            }
        }
    }
    
    fun beginManualConfiguration() {
        _state.value = _state.value.copy(
            isScanning = false,
            isConfiguring = false,
            configurationStep = ConfigurationStep.PASSWORD_INPUT,
            statusMessage = "🔧 Ingresa la IP del ESP32:",
            progress = 0.5f,
            selectedNetwork = WiFiNetwork(
                ssid = "CONFIGURACIÓN_MANUAL",
                bssid = "",
                level = -50,
                frequency = 2400,
                capabilities = "",
                isSecure = true
            ),
            error = null
        )
    }
    
    /**
     * ✅ OBTENER DIAGNÓSTICOS
     */
    fun getNetworkDiagnostics(): Map<String, Any> {
        return networkConfigManager.getNetworkDiagnostics()
    }
}
