package com.arranquesuave.motorcontrolapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arranquesuave.motorcontrolapp.services.DiscoveredDevice
import com.arranquesuave.motorcontrolapp.services.DiscoveryService
import com.arranquesuave.motorcontrolapp.services.MqttService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de Settings
 * Maneja discovery de ESP32, configuración WiFi y conexiones
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "SettingsViewModel"
    }

    // Services
    private val discoveryService = DiscoveryService(application)
    private var mqttService: MqttService? = null

    // Estado de discovery
    val discoveredDevices = discoveryService.discoveredDevices
    val isDiscovering = discoveryService.isDiscovering

    // Estado de conexiones
    private val _wifiConnected = MutableStateFlow(false)
    val wifiConnected: StateFlow<Boolean> = _wifiConnected.asStateFlow()

    private val _mqttConnected = MutableStateFlow(false)
    val mqttConnected: StateFlow<Boolean> = _mqttConnected.asStateFlow()

    private val _bluetoothConnected = MutableStateFlow(false)
    val bluetoothConnected: StateFlow<Boolean> = _bluetoothConnected.asStateFlow()

    // Mensajes y notificaciones
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    /**
     * Configurar MqttService para discovery
     */
    fun setMqttService(service: MqttService) {
        mqttService = service

        // Configurar callback de discovery
        service.setOnDeviceDiscovered { device ->
            Log.d(TAG, "Device discovered via MQTT: ${device.deviceName}")
            discoveryService.onMqttDeviceDiscovered(device)
        }

        // Actualizar estado de conexión
        _mqttConnected.value = service.isConnected()
    }

    /**
     * Iniciar MQTT Discovery
     */
    fun startMqttDiscovery() {
        viewModelScope.launch {
            try {
                val service = mqttService
                if (service == null) {
                    _statusMessage.value = "MQTT no está configurado"
                    return@launch
                }

                if (!service.isConnected()) {
                    _statusMessage.value = "Conectando a MQTT..."
                    // Intentar conectar primero
                    val result = service.connect("tcp://177.247.175.4:1885")
                    if (result.isFailure) {
                        _statusMessage.value = "Error conectando a MQTT"
                        return@launch
                    }
                }

                // Suscribirse a discovery
                service.subscribeToDiscovery()
                _statusMessage.value = "Escuchando dispositivos via MQTT..."

            } catch (e: Exception) {
                Log.e(TAG, "Error starting MQTT discovery", e)
                _statusMessage.value = "Error: ${e.message}"
            }
        }
    }

    /**
     * Iniciar mDNS Discovery
     */
    fun startMdnsDiscovery() {
        viewModelScope.launch {
            try {
                _statusMessage.value = "Buscando via mDNS..."
                discoveryService.startMdnsDiscovery()

                // Esperar resultado
                kotlinx.coroutines.delay(3000)
                if (discoveredDevices.value.isEmpty()) {
                    _statusMessage.value = "No se encontraron dispositivos via mDNS"
                } else {
                    _statusMessage.value = "Dispositivos encontrados: ${discoveredDevices.value.size}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting mDNS discovery", e)
                _statusMessage.value = "Error: ${e.message}"
            }
        }
    }

    /**
     * Escanear red local
     */
    fun scanNetworkRange(subnet: String = "192.168.1") {
        viewModelScope.launch {
            try {
                _statusMessage.value = "Escaneando red $subnet.x (esto puede tardar)..."
                discoveryService.startNetworkScan(subnet)

                // Monitorear progreso
                kotlinx.coroutines.delay(5000)
                _statusMessage.value = "Escaneando... Encontrados: ${discoveredDevices.value.size}"

            } catch (e: Exception) {
                Log.e(TAG, "Error scanning network", e)
                _statusMessage.value = "Error: ${e.message}"
            }
        }
    }

    /**
     * Configurar WiFi del ESP32
     */
    fun configureEsp32WiFi(
        ssid: String,
        password: String,
        mqttBroker: String,
        mqttPort: Int,
        deviceName: String
    ) {
        viewModelScope.launch {
            try {
                _statusMessage.value = "Configurando ESP32..."

                // TODO: Implementar llamada HTTP POST a 192.168.4.1/configure
                // Por ahora solo mostrar mensaje
                _statusMessage.value = "Configuración enviada al ESP32"

                Log.d(TAG, "ESP32 WiFi config: ssid=$ssid, mqtt=$mqttBroker:$mqttPort, device=$deviceName")

            } catch (e: Exception) {
                Log.e(TAG, "Error configuring ESP32 WiFi", e)
                _statusMessage.value = "Error: ${e.message}"
            }
        }
    }

    /**
     * Conectar a MQTT
     */
    fun connectMqtt() {
        viewModelScope.launch {
            try {
                val service = mqttService ?: return@launch
                _statusMessage.value = "Conectando a MQTT..."

                val result = service.connect("tcp://177.247.175.4:1885")
                if (result.isSuccess) {
                    _mqttConnected.value = true
                    _statusMessage.value = "Conectado a MQTT"
                } else {
                    _statusMessage.value = "Error conectando a MQTT"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to MQTT", e)
                _statusMessage.value = "Error: ${e.message}"
            }
        }
    }

    /**
     * Desconectar MQTT
     */
    fun disconnectMqtt() {
        viewModelScope.launch {
            try {
                mqttService?.disconnect()
                _mqttConnected.value = false
                _statusMessage.value = "Desconectado de MQTT"
            } catch (e: Exception) {
                Log.e(TAG, "Error disconnecting from MQTT", e)
            }
        }
    }

    /**
     * Conectar Bluetooth (delegado a MotorViewModel)
     */
    fun connectBluetooth() {
        // Esta función debería delegar a MotorViewModel
        _statusMessage.value = "Ir a sección Bluetooth para conectar"
    }

    /**
     * Desconectar Bluetooth
     */
    fun disconnectBluetooth() {
        // Esta función debería delegar a MotorViewModel
        _bluetoothConnected.value = false
        _statusMessage.value = "Bluetooth desconectado"
    }

    /**
     * Limpiar dispositivos descubiertos
     */
    fun clearDiscoveredDevices() {
        discoveryService.clearDiscoveredDevices()
        _statusMessage.value = null
    }

    /**
     * Limpiar mensaje de estado
     */
    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    /**
     * Cleanup
     */
    override fun onCleared() {
        super.onCleared()
        discoveryService.cleanup()
    }
}
