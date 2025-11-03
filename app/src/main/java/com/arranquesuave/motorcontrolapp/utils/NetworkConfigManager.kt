// app/src/main/java/com/arranquesuave/motorcontrolapp/utils/NetworkConfigManager.kt
package com.arranquesuave.motorcontrolapp.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * GESTOR DE CONFIGURACIÓN DE RED LOCAL
 * 
 * Permite a cada usuario configurar su propia red WiFi local:
 * - IP del broker MQTT local
 * - Puerto MQTT local  
 * - IP del backend API local
 * - Puerto backend local
 */
class NetworkConfigManager(private val context: Context) {
    
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "network_config")
        
        // Keys para configuración
        private val MQTT_LOCAL_IP = stringPreferencesKey("mqtt_local_ip")
        private val MQTT_LOCAL_PORT = stringPreferencesKey("mqtt_local_port")
        private val API_LOCAL_IP = stringPreferencesKey("api_local_ip")
        private val API_LOCAL_PORT = stringPreferencesKey("api_local_port")
        private val NETWORK_NAME = stringPreferencesKey("network_name")
        
        // Valores por defecto (del profesor como ejemplo)
        const val DEFAULT_MQTT_IP = "192.168.1.12"
        const val DEFAULT_MQTT_PORT = "1885"
        const val DEFAULT_API_IP = "192.168.1.12"
        const val DEFAULT_API_PORT = "3000"
        const val DEFAULT_NETWORK_NAME = "Mi Red Local"
    }
    
    /**
     * Configuración de red local del usuario
     */
    data class LocalNetworkConfig(
        val mqttIp: String = DEFAULT_MQTT_IP,
        val mqttPort: String = DEFAULT_MQTT_PORT,
        val apiIp: String = DEFAULT_API_IP,
        val apiPort: String = DEFAULT_API_PORT,
        val networkName: String = DEFAULT_NETWORK_NAME
    ) {
        fun getMqttUrl(): String = "tcp://$mqttIp:$mqttPort"
        fun getApiUrl(): String = "http://$apiIp:$apiPort/"
        
        fun isValidConfig(): Boolean {
            return mqttIp.isValidIP() && 
                   mqttPort.isValidPort() && 
                   apiIp.isValidIP() && 
                   apiPort.isValidPort() &&
                   networkName.isNotBlank()
        }
    }
    
    /**
     * Obtener configuración actual como Flow
     */
    val localNetworkConfig: Flow<LocalNetworkConfig> = context.dataStore.data
        .map { preferences ->
            LocalNetworkConfig(
                mqttIp = preferences[MQTT_LOCAL_IP] ?: DEFAULT_MQTT_IP,
                mqttPort = preferences[MQTT_LOCAL_PORT] ?: DEFAULT_MQTT_PORT,
                apiIp = preferences[API_LOCAL_IP] ?: DEFAULT_API_IP,
                apiPort = preferences[API_LOCAL_PORT] ?: DEFAULT_API_PORT,
                networkName = preferences[NETWORK_NAME] ?: DEFAULT_NETWORK_NAME
            )
        }
    
    /**
     * Guardar configuración de red local
     */
    suspend fun saveLocalNetworkConfig(config: LocalNetworkConfig) {
        context.dataStore.edit { preferences ->
            preferences[MQTT_LOCAL_IP] = config.mqttIp
            preferences[MQTT_LOCAL_PORT] = config.mqttPort
            preferences[API_LOCAL_IP] = config.apiIp
            preferences[API_LOCAL_PORT] = config.apiPort
            preferences[NETWORK_NAME] = config.networkName
        }
    }
    
    /**
     * Restablecer a configuración por defecto
     */
    suspend fun resetToDefaults() {
        saveLocalNetworkConfig(LocalNetworkConfig())
    }
    
    /**
     * Configuraciones predefinidas comunes
     */
    fun getCommonConfigurations(): List<LocalNetworkConfig> {
        return listOf(
            LocalNetworkConfig(
                mqttIp = "192.168.1.12",
                mqttPort = "1885", 
                apiIp = "192.168.1.12",
                apiPort = "3000",
                networkName = "Red Profesor ITM"
            ),
            LocalNetworkConfig(
                mqttIp = "192.168.1.100",
                mqttPort = "1883",
                apiIp = "192.168.1.100", 
                apiPort = "3000",
                networkName = "Mi Casa (192.168.1.x)"
            ),
            LocalNetworkConfig(
                mqttIp = "10.0.0.100",
                mqttPort = "1883",
                apiIp = "10.0.0.100",
                apiPort = "3000", 
                networkName = "Mi Casa (10.0.0.x)"
            ),
            LocalNetworkConfig(
                mqttIp = "172.16.0.100",
                mqttPort = "1883",
                apiIp = "172.16.0.100",
                apiPort = "3000",
                networkName = "Red Empresarial"
            )
        )
    }
}

/**
 * Extensiones para validación
 */
private fun String.isValidIP(): Boolean {
    return try {
        val parts = this.split(".")
        if (parts.size != 4) return false
        parts.all { 
            val num = it.toIntOrNull() ?: return false
            num in 0..255
        }
    } catch (e: Exception) {
        false
    }
}

private fun String.isValidPort(): Boolean {
    return try {
        val port = this.toIntOrNull() ?: return false
        port in 1..65535
    } catch (e: Exception) {
        false
    }
}
