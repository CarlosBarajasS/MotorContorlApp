package com.arranquesuave.motorcontrolapp.services

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

/**
 * Servicio para descubrir dispositivos ESP32 en la red
 * Soporta 3 métodos de discovery:
 * 1. MQTT Discovery (automático, vía broker)
 * 2. mDNS Discovery (local network)
 * 3. Network Scan (192.168.x.2-254)
 */
class DiscoveryService(private val context: Context) {

    companion object {
        private const val TAG = "DiscoveryService"
        private const val MDNS_HOSTNAME = "motorcontroller.local"
        private const val STATUS_ENDPOINT = "/status"
    }

    // Lista de dispositivos descubiertos
    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    // Estado de discovery
    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    // Scope para coroutines
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Iniciar discovery MQTT
     * Este método se llama cuando la app se suscribe al topic de discovery
     */
    fun onMqttDeviceDiscovered(device: DiscoveredDevice) {
        Log.d(TAG, "MQTT Discovery: ${device.deviceName} at ${device.getPreferredIp()}")
        addOrUpdateDevice(device)
    }

    /**
     * Buscar dispositivos via mDNS
     */
    fun startMdnsDiscovery() {
        if (_isDiscovering.value) {
            Log.w(TAG, "Discovery already in progress")
            return
        }

        _isDiscovering.value = true

        scope.launch {
            try {
                Log.d(TAG, "Starting mDNS discovery for $MDNS_HOSTNAME")

                // Intentar resolver hostname mDNS
                val address = withContext(Dispatchers.IO) {
                    try {
                        InetAddress.getByName(MDNS_HOSTNAME)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to resolve mDNS hostname", e)
                        null
                    }
                }

                if (address != null) {
                    val ip = address.hostAddress
                    Log.d(TAG, "mDNS resolved: $MDNS_HOSTNAME -> $ip")

                    // Consultar endpoint /status para obtener info completa
                    val device = queryStatusEndpoint(ip!!)
                    if (device != null) {
                        addOrUpdateDevice(device)
                    }
                } else {
                    Log.w(TAG, "mDNS resolution failed for $MDNS_HOSTNAME")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error during mDNS discovery", e)
            } finally {
                _isDiscovering.value = false
            }
        }
    }

    /**
     * Escanear rango de red local (192.168.x.2-254)
     */
    fun startNetworkScan(subnet: String = "192.168.1") {
        if (_isDiscovering.value) {
            Log.w(TAG, "Discovery already in progress")
            return
        }

        _isDiscovering.value = true

        scope.launch {
            try {
                Log.d(TAG, "Starting network scan on $subnet.x")

                val jobs = mutableListOf<Job>()

                // Escanear IPs 2-254 en paralelo
                for (i in 2..254) {
                    val ip = "$subnet.$i"
                    val job = launch {
                        checkIpForEsp32(ip)
                    }
                    jobs.add(job)

                    // Limitar concurrencia a 20 requests simultáneos
                    if (jobs.size >= 20) {
                        jobs.forEach { it.join() }
                        jobs.clear()
                    }
                }

                // Esperar a que terminen todos los trabajos
                jobs.forEach { it.join() }

                Log.d(TAG, "Network scan completed")

            } catch (e: Exception) {
                Log.e(TAG, "Error during network scan", e)
            } finally {
                _isDiscovering.value = false
            }
        }
    }

    /**
     * Verificar si una IP específica es un ESP32
     */
    private suspend fun checkIpForEsp32(ip: String) {
        try {
            // Intentar conectar al endpoint /status
            val device = queryStatusEndpoint(ip)
            if (device != null) {
                Log.d(TAG, "Found ESP32 at $ip: ${device.deviceName}")
                addOrUpdateDevice(device)
            }
        } catch (e: Exception) {
            // Silenciar errores de IPs que no responden
        }
    }

    /**
     * Consultar endpoint /status de un ESP32
     */
    private suspend fun queryStatusEndpoint(ip: String): DiscoveredDevice? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("http://$ip$STATUS_ENDPOINT")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 2000
                connection.readTimeout = 2000

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    parseStatusResponse(response)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Parsear respuesta JSON del endpoint /status
     */
    private fun parseStatusResponse(json: String): DiscoveredDevice? {
        return try {
            // Simple parsing (puedes mejorar con Gson/Moshi)
            val deviceName = json.substringAfter("\"device_name\":\"").substringBefore("\"")
            val apIp = if (json.contains("\"ap_ip\":\"")) {
                json.substringAfter("\"ap_ip\":\"").substringBefore("\"")
            } else "192.168.4.1"

            val apSsid = if (json.contains("\"ap_ssid\":\"")) {
                json.substringAfter("\"ap_ssid\":\"").substringBefore("\"")
            } else "ESP32-MotorSetup"

            val wifiIp = if (json.contains("\"ip_address\":\"")) {
                val ip = json.substringAfter("\"ip_address\":\"").substringBefore("\"")
                if (ip.isNotBlank() && ip != "0.0.0.0") ip else null
            } else null

            val wifiSsid = if (json.contains("\"ssid\":\"")) {
                json.substringAfter("\"ssid\":\"").substringBefore("\"")
            } else null

            DiscoveredDevice(
                deviceName = deviceName,
                apIp = apIp,
                apSsid = apSsid,
                wifiIp = wifiIp,
                wifiSsid = wifiSsid
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse status response", e)
            null
        }
    }

    /**
     * Agregar o actualizar dispositivo en la lista
     */
    private fun addOrUpdateDevice(device: DiscoveredDevice) {
        val currentList = _discoveredDevices.value.toMutableList()

        // Buscar si ya existe (por nombre de dispositivo)
        val existingIndex = currentList.indexOfFirst { it.deviceName == device.deviceName }

        if (existingIndex >= 0) {
            // Actualizar existente
            currentList[existingIndex] = device
        } else {
            // Agregar nuevo
            currentList.add(device)
        }

        _discoveredDevices.value = currentList
    }

    /**
     * Limpiar lista de dispositivos descubiertos
     */
    fun clearDiscoveredDevices() {
        _discoveredDevices.value = emptyList()
    }

    /**
     * Cancelar discovery en progreso
     */
    fun cancelDiscovery() {
        scope.coroutineContext.cancelChildren()
        _isDiscovering.value = false
    }

    /**
     * Limpiar recursos
     */
    fun cleanup() {
        scope.cancel()
        _discoveredDevices.value = emptyList()
    }
}
