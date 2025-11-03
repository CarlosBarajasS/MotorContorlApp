package com.arranquesuave.motorcontrolapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arranquesuave.motorcontrolapp.network.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

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
    private val esp32ConfigService = ESP32ConfigService()
    private val networkConfigManager = NetworkConfigManagerUpdated(application)
    
    private val _state = MutableStateFlow(WiFiSetupState())
    val state: StateFlow<WiFiSetupState> = _state.asStateFlow()
    
    init {
        startWiFiScanning()
    }
    
    /**
     * Inicia el escaneo de redes WiFi
     */
    fun startWiFiScanning() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isScanning = true,
                configurationStep = ConfigurationStep.SCANNING,
                statusMessage = "Escaneando redes WiFi disponibles...",
                error = null
            )
            
            try {
                if (!wifiService.isWiFiEnabled()) {
                    wifiService.enableWiFi()
                    delay(2000) // Esperar a que se habilite WiFi
                }
                
                val networks = wifiService.scanWiFiNetworks()
                
                _state.value = _state.value.copy(
                    isScanning = false,
                    availableNetworks = networks,
                    configurationStep = ConfigurationStep.NETWORK_SELECTION,
                    statusMessage = "Selecciona tu red WiFi",
                    progress = 0.2f
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
     * Selecciona una red WiFi
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
                "Ingresa la contraseña para ${network.ssid}"
            } else {
                "Red abierta seleccionada: ${network.ssid}"
            },
            progress = 0.4f,
            error = null
        )
        
        // Si es una red abierta, proceder automáticamente
        if (!network.isSecure) {
            configureWiFi("")
        }
    }
    
    /**
     * Configura WiFi con contraseña
     */
    fun configureWiFi(password: String) {
        val selectedNetwork = _state.value.selectedNetwork ?: return
        
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isConfiguring = true,
                configurationStep = ConfigurationStep.ESP32_CONNECTION,
                statusMessage = "Conectando al ESP32...",
                progress = 0.6f,
                error = null
            )
            
            try {
                // 1. Verificar si ESP32 está en modo configuración
                if (!esp32ConfigService.isConfigModeAvailable()) {
                    throw Exception("ESP32 no está disponible. Asegúrate de que esté en modo configuración.")
                }
                
                _state.value = _state.value.copy(
                    configurationStep = ConfigurationStep.WIFI_CONFIG,
                    statusMessage = "Enviando configuración WiFi al ESP32...",
                    progress = 0.7f
                )
                
                // 2. Enviar credenciales WiFi al ESP32
                val credentials = WiFiCredentials(
                    ssid = selectedNetwork.ssid,
                    password = password,
                    security = if (selectedNetwork.isSecure) "WPA2" else "OPEN"
                )
                
                val response = esp32ConfigService.configureWiFi(credentials)
                
                if (!response.success) {
                    throw Exception("Error al configurar WiFi: ${response.message}")
                }
                
                _state.value = _state.value.copy(
                    configurationStep = ConfigurationStep.WAITING_CONNECTION,
                    statusMessage = "Esperando que ESP32 se conecte a ${selectedNetwork.ssid}...",
                    progress = 0.8f
                )
                
                // 3. Esperar a que ESP32 se conecte y obtener nueva IP
                delay(10000) // Dar tiempo al ESP32 para conectarse
                
                _state.value = _state.value.copy(
                    configurationStep = ConfigurationStep.NETWORK_DISCOVERY,
                    statusMessage = "Buscando ESP32 en la red...",
                    progress = 0.9f
                )
                
                // 4. Configurar automáticamente la aplicación
                val autoConfigSuccess = networkConfigManager.autoConfigureNetwork()
                
                if (autoConfigSuccess) {
                    // 5. Verificar el estado final
                    delay(2000)
                    val finalStatus = esp32ConfigService.getStatus()
                    
                    _state.value = _state.value.copy(
                        isConfiguring = false,
                        configurationStep = ConfigurationStep.COMPLETED,
                        statusMessage = "¡Configuración completada exitosamente!",
                        progress = 1.0f,
                        isCompleted = true,
                        esp32Status = finalStatus
                    )
                } else {
                    throw Exception("No se pudo encontrar el ESP32 en la red. Verifica la configuración.")
                }
                
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isConfiguring = false,
                    configurationStep = ConfigurationStep.ERROR,
                    error = e.message,
                    statusMessage = "Error en la configuración",
                    progress = 0f
                )
            }
        }
    }
    
    /**
     * Reintenta la configuración
     */
    fun retryConfiguration() {
        _state.value = WiFiSetupState()
        startWiFiScanning()
    }
    
    /**
     * Cancela la configuración actual
     */
    fun cancelConfiguration() {
        _state.value = _state.value.copy(
            isConfiguring = false,
            configurationStep = ConfigurationStep.NETWORK_SELECTION,
            statusMessage = "Configuración cancelada",
            progress = 0.2f,
            error = null
        )
    }
    
    /**
     * Vuelve al paso anterior
     */
    fun goBack() {
        when (_state.value.configurationStep) {
            ConfigurationStep.PASSWORD_INPUT -> {
                _state.value = _state.value.copy(
                    configurationStep = ConfigurationStep.NETWORK_SELECTION,
                    statusMessage = "Selecciona tu red WiFi",
                    progress = 0.2f,
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
                    progress = 0.2f,
                    error = null
                )
            }
            else -> {
                // Para otros pasos, reiniciar completamente
                retryConfiguration()
            }
        }
    }
    
    /**
     * Actualiza manualmente la lista de redes
     */
    fun refreshNetworks() {
        if (!_state.value.isScanning && !_state.value.isConfiguring) {
            startWiFiScanning()
        }
    }
    
    /**
     * Obtiene el diagnóstico de red actual
     */
    fun getNetworkDiagnostics(): Map<String, Any> {
        return networkConfigManager.getNetworkDiagnostics()
    }
}
