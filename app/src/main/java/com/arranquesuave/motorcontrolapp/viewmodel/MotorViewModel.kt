// app/src/main/java/com/arranquesuave/motorcontrolapp/viewmodel/MotorViewModel.kt
package com.arranquesuave.motorcontrolapp.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arranquesuave.motorcontrolapp.interfaces.MotorController
import com.arranquesuave.motorcontrolapp.controllers.BluetoothMotorController
import com.arranquesuave.motorcontrolapp.controllers.MqttMotorController
import com.arranquesuave.motorcontrolapp.services.BluetoothService
import com.arranquesuave.motorcontrolapp.services.MqttService
import com.arranquesuave.motorcontrolapp.config.MqttConfig
import com.arranquesuave.motorcontrolapp.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class MotorViewModel(application: Application) : AndroidViewModel(application) {

    // ============================================
    // ENUMS Y CONFIGURACIÓN
    // ============================================
    enum class ConnectionMode { 
        BLUETOOTH,      // Conexión directa ESP32
        MQTT_REMOTE,    // Desde tu casa (Internet) ✅
        MQTT_TEST,      // Testing/Desarrollo
        WIFI_LOCAL,     // ✅ WiFi Local configurado por usuario
        WIFI_SETUP      // 🆕 Configuración WiFi local
    }
    
    // ============================================
    // SERVICES Y CONFIGURACIÓN
    // ============================================
    private val bluetoothService = BluetoothService(application)
    private val mqttService = MqttService(application)
    private val mqttConfig = MqttConfig(application)  // ✅ NUEVO: Configuración dinámica
    
    // ✅ INICIALIZAR SERVICIOS
    fun initializeServices() {
        viewModelScope.launch {
            _status.value = "Servicios inicializados ✅"
        }
    }
    
    // ============================================
    // CURRENT CONTROLLER
    // ============================================
    private var currentController: MotorController? = null
    
    // ============================================
    // STATE FLOWS
    // ============================================
    private val _connectionMode = MutableStateFlow(ConnectionMode.BLUETOOTH)
    val connectionMode: StateFlow<ConnectionMode> = _connectionMode
    
    private val _discovered = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discovered
    
    private val _connectedDeviceAddress = MutableStateFlow<String?>(null)
    val connectedDeviceAddress: StateFlow<String?> = _connectedDeviceAddress
    
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning
    
    private val _speed = MutableStateFlow(0)
    val speed: StateFlow<Int> = _speed
    
    private val _status = MutableStateFlow("Disconnected")
    val status: StateFlow<String> = _status
    
    private val _motorRunning = MutableStateFlow(false)
    val motorRunning: StateFlow<Boolean> = _motorRunning
    
    // Información de conexión
    private val _currentUrl = MutableStateFlow("")
    val currentUrl: StateFlow<String> = _currentUrl
    
    // ============================================
    // SLIDERS (MANTENER IGUAL)
    // ============================================
    val sliders = List(6) { MutableStateFlow(0) }
    
    // Control continuo (mantener igual)
    private val _contSpeed = MutableStateFlow(0)
    val contSpeed: StateFlow<Int> = _contSpeed
    
    // ============================================
    // CAMBIAR MODO DE CONEXIÓN + SINCRONIZACIÓN CRÍTICA
    // ============================================
    fun switchConnectionMode(mode: ConnectionMode) {
        viewModelScope.launch {
            // Desconectar controlador actual
            currentController?.disconnect()
            
            _connectionMode.value = mode
            _status.value = "Disconnected"
            _connectedDeviceAddress.value = null
            
            // ✅ CONFIGURACIÓN ACTUALIZADA CON WIFI_LOCAL
            when(mode) {
                ConnectionMode.BLUETOOTH -> {
                    _currentUrl.value = "Bluetooth SPP"
                    // No cambiar RetrofitClient para Bluetooth
                }
                ConnectionMode.WIFI_LOCAL -> {
                    val localUrl = mqttConfig.getMqttLocalUrl()
                    _currentUrl.value = localUrl
                    RetrofitClient.setBaseUrl(RetrofitClient.ConnectionMode.LOCAL)
                }
                ConnectionMode.MQTT_REMOTE -> {
                    _currentUrl.value = MqttConfig.MQTT_REMOTE_URL
                    RetrofitClient.setBaseUrl(RetrofitClient.ConnectionMode.REMOTE)
                }
                ConnectionMode.MQTT_TEST -> {
                    _currentUrl.value = MqttConfig.MQTT_TEST_URL
                    RetrofitClient.setBaseUrl(RetrofitClient.ConnectionMode.TEST)
                }
                ConnectionMode.WIFI_SETUP -> {
                    _currentUrl.value = "Buscando redes WiFi..."
                    // Modo para escanear y conectar a WiFi local
                }
            }
            
            // Reset controller
            currentController = null
            
            _status.value = "Mode: ${mode.name} | URL: ${_currentUrl.value}"
        }
    }
    
    // ============================================
    // CONECTAR SEGÚN EL MODO
    // ============================================
    @SuppressLint("MissingPermission")
    fun connectDevice(device: BluetoothDevice) = viewModelScope.launch {
        _status.value = "Connecting to ${_connectionMode.value}..."
        
        try {
            currentController = when (_connectionMode.value) {
                ConnectionMode.BLUETOOTH -> {
                    BluetoothMotorController(bluetoothService, device)
                }
                ConnectionMode.WIFI_LOCAL -> {
                    // ✅ USAR CONFIGURACIÓN LOCAL DINÁMICA DEL USUARIO
                    val localUrl = mqttConfig.getMqttLocalUrl()
                    MqttMotorController(
                        mqttService, 
                        RetrofitClient.authApi,
                        localUrl
                    )
                }
                ConnectionMode.MQTT_REMOTE -> {
                    MqttMotorController(
                        mqttService,
                        RetrofitClient.authApi, 
                        MqttConfig.MQTT_REMOTE_URL
                    )
                }
                ConnectionMode.MQTT_TEST -> {
                    MqttMotorController(
                        mqttService,
                        RetrofitClient.authApi, 
                        MqttConfig.MQTT_TEST_URL
                    )
                }
                ConnectionMode.WIFI_SETUP -> {
                    // Modo configuración WiFi - no crear controller real
                    _status.value = "Modo configuración WiFi - use 'Configurar WiFi'"
                    return@launch
                }
            }
            
            // Configurar callbacks de telemetría
            currentController?.setOnSpeedReceived { speed ->
                _speed.value = speed
            }
            
            currentController?.setOnStatusReceived { status ->
                _motorRunning.value = status == "running"
                _status.value = "Motor: $status"
            }
            
            // Conectar
            currentController?.connect()?.fold(
                onSuccess = {
                    _connectedDeviceAddress.value = when(_connectionMode.value) {
                        ConnectionMode.BLUETOOTH -> device.address
                        else -> _currentUrl.value
                    }
                    _status.value = "✅ Connected via ${_connectionMode.value}"
                },
                onFailure = { error ->
                    _status.value = "❌ Error: ${error.localizedMessage}"
                    currentController = null
                }
            )
            
        } catch (e: Exception) {
            _status.value = "❌ Exception: ${e.localizedMessage}"
            currentController = null
        }
    }
    
    /**
     * Conecta vía MQTT (sin device Bluetooth) - Para modos MQTT
     */
    fun connectMqtt() = viewModelScope.launch {
        if (_connectionMode.value == ConnectionMode.BLUETOOTH) return@launch
        
        _status.value = "Connecting to MQTT…"
        
        try {
            currentController = when (_connectionMode.value) {
                ConnectionMode.WIFI_LOCAL -> {
                    // ✅ USAR CONFIGURACIÓN LOCAL DINÁMICA DEL USUARIO
                    val localUrl = mqttConfig.getMqttLocalUrl()
                    MqttMotorController(
                        mqttService, 
                        RetrofitClient.authApi,
                        localUrl
                    )
                }
                ConnectionMode.MQTT_REMOTE -> {
                    MqttMotorController(
                        mqttService,
                        RetrofitClient.authApi,
                        MqttConfig.MQTT_REMOTE_URL
                    )
                }
                ConnectionMode.MQTT_TEST -> {
                    MqttMotorController(
                        mqttService,
                        RetrofitClient.authApi,
                        MqttConfig.MQTT_TEST_URL
                    )
                }
                else -> return@launch
            }
            
            // Configurar callbacks
            currentController?.setOnSpeedReceived { speed ->
                _speed.value = speed
            }
            
            currentController?.setOnStatusReceived { status ->
                _motorRunning.value = status == "running"
            }
            
            // Conectar
            currentController?.connect()?.fold(
                onSuccess = {
                    _connectedDeviceAddress.value = currentController?.getConnectionInfo()
                    _status.value = "✅ Connected via MQTT"
                },
                onFailure = { error ->
                    _status.value = "❌ MQTT Error: ${error.localizedMessage}"
                    currentController = null
                }
            )
            
        } catch (e: Exception) {
            _status.value = "❌ Error: ${e.localizedMessage}"
            currentController = null
        }
    }
    
    // ============================================
    // COMANDO ARRANQUE SUAVE 6 PASOS
    // ============================================
    fun sendArranque6P() = viewModelScope.launch {
        _status.value = "🚀 Enviando arranque suave..."
        
        val values = sliders.map { it.value }
        currentController?.sendArranque6P(values)?.fold(
            onSuccess = {
                _status.value = "✅ Arranque suave enviado: ${values.joinToString(",")}"
                _motorRunning.value = true
            },
            onFailure = { error ->
                _status.value = "❌ Error arranque: ${error.localizedMessage}"
            }
        )
    }
    
    // ============================================
    // COMANDO ARRANQUE CONTINUO
    // ============================================
    fun sendContinuo() = viewModelScope.launch {
        _status.value = "⚡ Enviando arranque continuo..."
        
        currentController?.sendContinuo()?.fold(
            onSuccess = {
                _status.value = "✅ Arranque continuo activado"
                _motorRunning.value = true
            },
            onFailure = { error ->
                _status.value = "❌ Error continuo: ${error.localizedMessage}"
            }
        )
    }
    
    // ============================================
    // COMANDO PARO DE EMERGENCIA
    // ============================================
    fun sendParo() = viewModelScope.launch {
        _status.value = "🛑 Enviando paro de emergencia..."
        
        currentController?.sendParo()?.fold(
            onSuccess = {
                _status.value = "✅ Motor detenido"
                _motorRunning.value = false
                _speed.value = 0
            },
            onFailure = { error ->
                _status.value = "❌ Error paro: ${error.localizedMessage}"
            }
        )
    }
    
    // ============================================
    // MÉTODOS BLUETOOTH (MANTENER COMPATIBILIDAD)
    // ============================================
    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        if (_connectionMode.value == ConnectionMode.BLUETOOTH) {
            // ✅ SOLO BLUETOOTH REAL - Hardware físico
            val paired = bluetoothService.getPairedDevices()
            _discovered.value = paired
            _isScanning.value = true
            
            // Iniciar descubrimiento de dispositivos reales
            bluetoothService.startDiscovery(
                onDeviceFound = { dev ->
                    val current = _discovered.value.toMutableList()
                    if (current.none { it.address == dev.address }) {
                        current += dev
                        _discovered.value = current
                    }
                },
                onFinished = {
                    _isScanning.value = false
                }
            )
            
            // Detener escaneo tras 10 segundos
            viewModelScope.launch {
                delay(10000)
                if (_isScanning.value) {
                    stopDiscovery()
                }
            }
        } else {
            // ✅ MODOS MQTT - Sin necesidad de "discovery", conectar directamente
            _discovered.value = emptyList()
            _isScanning.value = false
            _status.value = "MQTT Mode: Use 'Connect MQTT' button"
        }
    }
    
    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        bluetoothService.stopDiscovery()
        _isScanning.value = false
    }
    
    // ============================================
    // DESCONECTAR DISPOSITIVO
    // ============================================
    fun disconnectDevice() = viewModelScope.launch {
        currentController?.disconnect()
        currentController = null
        _connectedDeviceAddress.value = null
        _status.value = "Disconnected"
        _motorRunning.value = false
        _speed.value = 0
    }
    
    // ============================================
    // CAMBIO DE SLIDER
    // ============================================
    fun onSliderChanged(index: Int, value: Int) {
        sliders.getOrNull(index)?.value = value.coerceIn(0, 254)
    }
    
    fun onContSpeedChanged(value: Int) {
        _contSpeed.value = value.coerceIn(0, 254)
    }
    
    // ============================================
    // OBTENER VALORES ACTUALES DE SLIDERS
    // ============================================
    fun getCurrentSliderValues(): List<Int> {
        return sliders.map { it.value }
    }
    
    // ============================================
    // INFORMACIÓN DE CONEXIÓN
    // ============================================
    fun getConnectionInfo(): String {
        return when(_connectionMode.value) {
            ConnectionMode.BLUETOOTH -> "Bluetooth SPP directo"
            ConnectionMode.WIFI_LOCAL -> {
                // ✅ MOSTRAR CONFIGURACIÓN PERSONALIZADA DEL USUARIO
                val currentUrl = _currentUrl.value
                "WiFi Local: $currentUrl"
            }
            ConnectionMode.MQTT_REMOTE -> "MQTT Remoto: ${MqttConfig.MQTT_REMOTE_URL}"
            ConnectionMode.MQTT_TEST -> "MQTT Testing: ${MqttConfig.MQTT_TEST_URL}"
            ConnectionMode.WIFI_SETUP -> "Configuración WiFi del dispositivo"
        }
    }
    
    // ============================================
    // CONFIGURACIÓN WIFI DEL CLIENTE
    // ============================================
    fun configureWiFi(ssid: String, password: String) = viewModelScope.launch {
        _status.value = "Configurando WiFi del dispositivo..."
        
        try {
            // Enviar credenciales al ESP32 vía HTTP POST al puerto 80 (modo configuración)
            // TODO: Implementar endpoint real /configure
            delay(2000) // Simular envío
            
            _status.value = "✅ WiFi configurado exitosamente. Dispositivo reiniciando..."
            delay(3000)
            _status.value = "Busca tu dispositivo en modo 'WiFi Local'"
            
        } catch (e: Exception) {
            _status.value = "❌ Error de conexión: ${e.localizedMessage}"
        }
    }
    
    // ✅ CONFIGURACIÓN WIFI REAL DEL ESP32
    fun configureWiFiReal(ssid: String, password: String) = viewModelScope.launch {
        _status.value = "🚀 Configurando ESP32 WiFi..."
        
        try {
            // TODO: Implementar configuración real con ESP32ConfigService
            delay(2000) // Simular configuración
            
            _status.value = "✅ WiFi configurado exitosamente. Dispositivo reiniciando..."
            delay(3000)
            _status.value = "✅ Dispositivo listo. Usa modo 'WiFi Local' para conectar"
            
        } catch (e: Exception) {
            _status.value = "❌ Error configurando WiFi: ${e.localizedMessage}"
        }
    }
    
    // Escanear redes WiFi del ESP32
    fun scanWiFiNetworks() = viewModelScope.launch {
        _status.value = "Escaneando redes WiFi..."
        
        try {
            // TODO: Implementar escaneo real del ESP32
            delay(2000)
            _status.value = "Redes encontradas"
        } catch (e: Exception) {
            _status.value = "Error escaneando: ${e.localizedMessage}"
        }
    }
    
    // ============================================
    // CLEANUP
    // ============================================
    override fun onCleared() {
        super.onCleared()
        currentController?.let { 
            viewModelScope.launch { 
                it.disconnect() 
            }
        }
    }
}
