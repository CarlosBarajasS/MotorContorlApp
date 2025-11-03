package com.arranquesuave.motorcontrolapp.network

import android.content.Context
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import android.Manifest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class WiFiNetwork(
    val ssid: String,
    val bssid: String,
    val level: Int,
    val frequency: Int,
    val capabilities: String,
    val isSecure: Boolean = capabilities.contains("WPA") || capabilities.contains("WEP")
) {
    val signalStrength: Int
        get() = when {
            level >= -50 -> 4  // Excelente
            level >= -60 -> 3  // Buena
            level >= -70 -> 2  // Regular
            level >= -80 -> 1  // Débil
            else -> 0          // Muy débil
        }
}

class WiFiService(private val context: Context) {
    
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    
    /**
     * Escanea las redes WiFi disponibles
     */
    suspend fun scanWiFiNetworks(): List<WiFiNetwork> {
        if (!hasWiFiPermissions()) {
            throw SecurityException("No se tienen los permisos necesarios para escanear WiFi")
        }
        
        // Iniciar escaneo
        val scanStarted = wifiManager.startScan()
        if (!scanStarted) {
            throw RuntimeException("No se pudo iniciar el escaneo WiFi")
        }
        
        // Esperar a que termine el escaneo
        delay(3000)
        
        // Obtener resultados
        val scanResults = wifiManager.scanResults
        
        return scanResults
            .filter { !it.SSID.isNullOrBlank() }
            .map { scanResult ->
                WiFiNetwork(
                    ssid = scanResult.SSID,
                    bssid = scanResult.BSSID,
                    level = scanResult.level,
                    frequency = scanResult.frequency,
                    capabilities = scanResult.capabilities
                )
            }
            .distinctBy { it.ssid }
            .sortedByDescending { it.level }
    }
    
    /**
     * Busca específicamente el ESP32 en modo configuración
     */
    suspend fun findESP32ConfigNetwork(): WiFiNetwork? {
        val networks = scanWiFiNetworks()
        return networks.find { 
            it.ssid.contains("ESP32", ignoreCase = true) || 
            it.ssid.contains("MotorControl", ignoreCase = true) ||
            it.ssid.startsWith("ESP_")
        }
    }
    
    /**
     * Verifica si el WiFi está habilitado
     */
    fun isWiFiEnabled(): Boolean = wifiManager.isWifiEnabled
    
    /**
     * Habilita el WiFi
     */
    fun enableWiFi(): Boolean {
        return if (!wifiManager.isWifiEnabled) {
            wifiManager.isWifiEnabled = true
            true
        } else {
            true
        }
    }
    
    /**
     * Obtiene la red WiFi actual conectada
     */
    fun getCurrentNetwork(): String? {
        val wifiInfo = wifiManager.connectionInfo
        return wifiInfo?.ssid?.removeSurrounding("\"")
    }
    
    /**
     * Verifica los permisos necesarios
     */
    private fun hasWiFiPermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE
        )
        
        return permissions.all { permission ->
            ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Obtiene un flow de redes WiFi que se actualiza periódicamente
     */
    fun getWiFiNetworksFlow(): Flow<List<WiFiNetwork>> = flow {
        while (true) {
            try {
                val networks = scanWiFiNetworks()
                emit(networks)
            } catch (e: Exception) {
                emit(emptyList())
            }
            delay(10000) // Actualizar cada 10 segundos
        }
    }
}
