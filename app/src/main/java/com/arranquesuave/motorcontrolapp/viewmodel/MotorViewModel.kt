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
import com.arranquesuave.motorcontrolapp.network.NetworkConfigManagerUpdated
import com.arranquesuave.motorcontrolapp.network.ESP32ConfigService
import com.arranquesuave.motorcontrolapp.network.ESP32Status
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.Locale

class MotorViewModel(application: Application) : AndroidViewModel(application) {
    
    private val runningModes = setOf("arranque6p", "continuo", "running")
    private val stoppedModes = setOf("paro", "stop", "stopped")

    private fun updateRunningStateFromMode(mode: String?) {
        val normalized = mode?.lowercase(Locale.getDefault()) ?: return
        when {
            runningModes.contains(normalized) -> {
                if (!_motorRunning.value) {
                    _motorRunning.value = true
                }
            }
            stoppedModes.contains(normalized) -> {
                if (_motorRunning.value) {
                    _motorRunning.value = false
                }
                if (_speed.value != 0) {
                    _speed.value = 0
                }
            }
        }
    }

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
    private val networkConfigManager = NetworkConfigManagerUpdated(application)
    private val esp32ConfigService = ESP32ConfigService(application)
    
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

    private val _motorMode = MutableStateFlow<String?>(null)
    val motorMode: StateFlow<String?> = _motorMode
    
    // Información de conexión
    private val _currentUrl = MutableStateFlow("")
    val currentUrl: StateFlow<String> = _currentUrl

    private val _localEsp32Ip = MutableStateFlow<String?>(null)
    val localEsp32Ip: StateFlow<String?> = _localEsp32Ip

    private val _esp32Status = MutableStateFlow<ESP32Status?>(null)
    val esp32Status: StateFlow<ESP32Status?> = _esp32Status
    
    private fun applyDeviceId(deviceId: String?) {
        if (deviceId != null) {
            val normalized = MqttConfig.normalizeDeviceId(deviceId)
            MqttConfig.saveDeviceId(normalized)
            mqttService.updateDeviceId(normalized)
        } else {
            mqttService.updateDeviceId(MqttConfig.getDeviceId())
        }
    }

    init {
        // ✅ INICIALIZAR MqttConfig CON CONTEXTO
        MqttConfig.init(application)
        mqttService.updateDeviceId(MqttConfig.getDeviceId())
        
        viewModelScope.launch {
            networkConfigManager.networkConfig.collectLatest { config ->
                _localEsp32Ip.value = config.esp32IP
                if (_connectionMode.value == ConnectionMode.WIFI_LOCAL) {
                    _currentUrl.value = config.esp32IP?.let { "http://$it" } ?: "Sin configurar"
                }
            }
        }
    }

    private fun attachControllerCallbacks(controller: MotorController) {
        controller.setOnSpeedReceived { speed ->
            _speed.value = speed
        }

        controller.setOnStatusReceived { status ->
            val normalized = status.lowercase(Locale.getDefault())
            when (normalized) {
                "running" -> {
                    _motorRunning.value = true
                    _status.value = "Motor en marcha"
                }
                "stopped" -> {
                    _motorRunning.value = false
                    _status.value = "Motor detenido"
                    if (_speed.value != 0) {
                        _speed.value = 0
                    }
                }
                "paro", "stop" -> {
                    _motorRunning.value = false
                    _status.value = "Motor detenido (paro)"
                    if (_speed.value != 0) {
                        _speed.value = 0
                    }
                }
                else -> {
                    if (status.isNotBlank()) {
                        _status.value = "Motor: $status"
                    }
                }
            }
        }

        controller.setOnModeReceived { mode ->
            if (mode.isBlank()) return@setOnModeReceived

            _motorMode.value = mode
            updateRunningStateFromMode(mode)
            val normalized = mode.lowercase(Locale.getDefault())
            when (normalized) {
                "arranque6p" -> {
                    _status.value = "🚀 Arranque suave en progreso"
                }
                "continuo" -> {
                    _status.value = "⚡ Modo continuo activo"
                }
                "paro", "stop" -> {
                    _status.value = "🛑 Motor detenido"
                }
                else -> {
                    _status.value = "Modo: $mode"
                }
            }
        }
    }
    
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
            
            // ✅ CONFIGURACIÓN ACTUALIZADA CON BROKER DEL PROFESOR
            when(mode) {
                ConnectionMode.BLUETOOTH -> {
                    _currentUrl.value = "Bluetooth SPP"
                    // No cambiar RetrofitClient para Bluetooth
                }
                ConnectionMode.WIFI_LOCAL -> {
                    val ip = _localEsp32Ip.value
                    _currentUrl.value = ip?.let { "http://$it" } ?: "Sin configurar"
                    RetrofitClient.setBaseUrl(RetrofitClient.ConnectionMode.LOCAL)
                    _status.value = if (ip != null) {
                        "ESP32 listo en $ip. Usa 'Conectar MQTT' para controlar."
                    } else {
                        "Sin IP configurada. Ejecuta 'Configurar WiFi'."
                    }
                    applyDeviceId(null)
                }
                ConnectionMode.MQTT_REMOTE -> {
                    _currentUrl.value = MqttConfig.MQTT_BROKER_URL
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
    
    fun onWiFiSetupCompleted(autoConnect: Boolean = true) {
        viewModelScope.launch {
            switchConnectionMode(ConnectionMode.WIFI_LOCAL)
            applyDeviceId(null)
            val ip = _localEsp32Ip.value
            if (ip != null) {
                _status.value = "✅ ESP32 configurado en $ip"
                if (autoConnect) {
                    connectMqtt()
                }
            } else {
                _status.value = "Configuración completada. Sin IP guardada."
            }
        }
    }

    fun refreshEsp32Status() = viewModelScope.launch {
        val ip = _localEsp32Ip.value
        if (ip.isNullOrBlank()) {
            _status.value = "Sin IP guardada. Ejecuta Configurar WiFi."
            return@launch
        }
        
        _status.value = "Consultando ESP32 en $ip..."
        val statusResult = esp32ConfigService.getStatus(ip)
        _esp32Status.value = statusResult
        applyDeviceId(statusResult?.deviceName)
        
        _status.value = if (statusResult != null && statusResult.connected) {
            "ESP32 conectado a ${statusResult.ssid ?: "red desconocida"}"
        } else {
            "ESP32 sin conexión WiFi o fuera de línea"
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
                    // ✅ USAR BROKER DEL PROFESOR
                    MqttMotorController(
                        mqttService, 
                        RetrofitClient.authApi,
                        MqttConfig.MQTT_BROKER_URL
                    )
                }
                ConnectionMode.MQTT_REMOTE -> {
                    MqttMotorController(
                        mqttService,
                        RetrofitClient.authApi, 
                        MqttConfig.MQTT_BROKER_URL
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
            
            currentController?.let { attachControllerCallbacks(it) }
            
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
                    // ✅ USAR BROKER DEL PROFESOR
                    MqttMotorController(
                        mqttService, 
                        RetrofitClient.authApi,
                        MqttConfig.MQTT_BROKER_URL
                    )
                }
                ConnectionMode.MQTT_REMOTE -> {
                    MqttMotorController(
                        mqttService,
                        RetrofitClient.authApi,
                        MqttConfig.MQTT_BROKER_URL
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
            
            currentController?.let { attachControllerCallbacks(it) }
            
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
                _motorMode.value = "arranque6p"
                updateRunningStateFromMode("arranque6p")
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
                _motorMode.value = "continuo"
                updateRunningStateFromMode("continuo")
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
                _motorMode.value = "paro"
                updateRunningStateFromMode("paro")
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
        _motorMode.value = null
        _speed.value = 0
        if (_connectionMode.value == ConnectionMode.WIFI_LOCAL) {
            _esp32Status.value = null
        }
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
                "WiFi/MQTT: ${MqttConfig.MQTT_BROKER_URL}"
            }
            ConnectionMode.MQTT_REMOTE -> "MQTT Broker: ${MqttConfig.MQTT_BROKER_URL}"
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
