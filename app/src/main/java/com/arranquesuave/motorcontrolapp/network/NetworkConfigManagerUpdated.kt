package com.arranquesuave.motorcontrolapp.network

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NetworkConfig(
    val esp32IP: String? = null,
    val mqttBrokerIP: String? = null,
    val isConfigured: Boolean = false,
    val lastConfigTime: Long = 0L,
    val networkSSID: String? = null
)

class NetworkConfigManagerUpdated(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "network_config"
        private const val KEY_ESP32_IP = "esp32_ip"
        private const val KEY_MQTT_BROKER_IP = "mqtt_broker_ip"
        private const val KEY_IS_CONFIGURED = "is_configured"
        private const val KEY_LAST_CONFIG_TIME = "last_config_time"
        private const val KEY_NETWORK_SSID = "network_ssid"
        
        // IPs por defecto cuando no se ha configurado
        private const val DEFAULT_ESP32_PORT = "80"
        private const val DEFAULT_MQTT_PORT = "1883"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _networkConfig = MutableStateFlow(loadConfig())
    val networkConfig: StateFlow<NetworkConfig> = _networkConfig.asStateFlow()
    
    /**
     * Carga la configuración guardada
     */
    private fun loadConfig(): NetworkConfig {
        return NetworkConfig(
            esp32IP = prefs.getString(KEY_ESP32_IP, null),
            mqttBrokerIP = prefs.getString(KEY_MQTT_BROKER_IP, null),
            isConfigured = prefs.getBoolean(KEY_IS_CONFIGURED, false),
            lastConfigTime = prefs.getLong(KEY_LAST_CONFIG_TIME, 0L),
            networkSSID = prefs.getString(KEY_NETWORK_SSID, null)
        )
    }
    
    /**
     * Guarda la configuración de red
     */
    fun saveNetworkConfig(esp32IP: String, mqttBrokerIP: String? = null) {
        val currentSSID = getCurrentNetworkSSID()
        
        prefs.edit().apply {
            putString(KEY_ESP32_IP, esp32IP)
            putString(KEY_MQTT_BROKER_IP, mqttBrokerIP ?: esp32IP) // Usar misma IP si no se especifica
            putBoolean(KEY_IS_CONFIGURED, true)
            putLong(KEY_LAST_CONFIG_TIME, System.currentTimeMillis())
            putString(KEY_NETWORK_SSID, currentSSID)
            apply()
        }
        
        _networkConfig.value = loadConfig()
    }
    
    /**
     * Configura automáticamente las IPs después de configurar WiFi en ESP32
     */
    suspend fun autoConfigureNetwork(): Boolean {
        val esp32ConfigService = ESP32ConfigService()
        
        // 1. Intentar obtener la nueva IP del ESP32
        val esp32IP = esp32ConfigService.getNewIP()
        
        if (esp32IP != null) {
            // 2. Configurar con la IP obtenida
            saveNetworkConfig(esp32IP)
            return true
        }
        
        // 3. Si no se pudo obtener, buscar en la red local
        val currentNetworkIP = getCurrentNetworkBaseIP()
        if (currentNetworkIP != null) {
            val foundIP = esp32ConfigService.findESP32InNetwork(currentNetworkIP)
            if (foundIP != null) {
                saveNetworkConfig(foundIP)
                return true
            }
        }
        
        return false
    }
    
    /**
     * Obtiene la IP base de la red actual (ej: "192.168.1" de "192.168.1.100")
     */
    private fun getCurrentNetworkBaseIP(): String? {
        val wifiInfo = wifiManager.connectionInfo
        val ipAddress = wifiInfo.ipAddress
        
        if (ipAddress != 0) {
            val ip = String.format(
                "%d.%d.%d",
                ipAddress and 0xFF,
                ipAddress shr 8 and 0xFF,
                ipAddress shr 16 and 0xFF
            )
            return ip
        }
        return null
    }
    
    /**
     * Obtiene el SSID de la red actual
     */
    private fun getCurrentNetworkSSID(): String? {
        val wifiInfo = wifiManager.connectionInfo
        return wifiInfo?.ssid?.removeSurrounding("\"")
    }
    
    /**
     * Verifica si estamos en la misma red que cuando se configuró
     */
    fun isInConfiguredNetwork(): Boolean {
        val currentSSID = getCurrentNetworkSSID()
        val configuredSSID = _networkConfig.value.networkSSID
        return currentSSID != null && currentSSID == configuredSSID
    }
    
    /**
     * Verifica si hay conexión a internet
     */
    fun hasInternetConnection(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    /**
     * Verifica si hay conexión WiFi
     */
    fun hasWiFiConnection(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
    
    /**
     * Obtiene la URL completa del ESP32
     */
    fun getESP32URL(): String? {
        val config = _networkConfig.value
        return config.esp32IP?.let { "http://$it:$DEFAULT_ESP32_PORT" }
    }
    
    /**
     * Obtiene la configuración MQTT
     */
    fun getMQTTConfig(): Pair<String, String>? {
        val config = _networkConfig.value
        return config.mqttBrokerIP?.let { ip ->
            Pair(ip, DEFAULT_MQTT_PORT)
        }
    }
    
    /**
     * Resetea la configuración de red
     */
    fun resetConfiguration() {
        prefs.edit().clear().apply()
        _networkConfig.value = NetworkConfig()
    }
    
    /**
     * Verifica si la configuración actual es válida
     */
    fun isConfigurationValid(): Boolean {
        val config = _networkConfig.value
        return config.isConfigured && 
               !config.esp32IP.isNullOrBlank() && 
               isInConfiguredNetwork()
    }
    
    /**
     * Obtiene información de diagnóstico de red
     */
    fun getNetworkDiagnostics(): Map<String, Any> {
        val config = _networkConfig.value
        return mapOf(
            "isConfigured" to config.isConfigured,
            "esp32IP" to (config.esp32IP ?: "No configurado"),
            "mqttBrokerIP" to (config.mqttBrokerIP ?: "No configurado"),
            "currentSSID" to (getCurrentNetworkSSID() ?: "Desconectado"),
            "configuredSSID" to (config.networkSSID ?: "Ninguno"),
            "isInConfiguredNetwork" to isInConfiguredNetwork(),
            "hasWiFiConnection" to hasWiFiConnection(),
            "hasInternetConnection" to hasInternetConnection(),
            "lastConfigTime" to if (config.lastConfigTime > 0) {
                java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
                    .format(java.util.Date(config.lastConfigTime))
            } else "Nunca"
        )
    }
}
