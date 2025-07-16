// app/src/main/java/com/arranquesuave/motorcontrolapp/viewmodel/MotorViewModel.kt
package com.arranquesuave.motorcontrolapp.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arranquesuave.motorcontrolapp.services.BluetoothService
import com.arranquesuave.motorcontrolapp.util.Protocol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class MotorViewModel(application: Application) : AndroidViewModel(application) {

    private val service = BluetoothService(application)

    private val _discovered  = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    private val _connectedDeviceAddress = MutableStateFlow<String?>(null)
    val connectedDeviceAddress: StateFlow<String?> = _connectedDeviceAddress
    val discoveredDevices   : StateFlow<List<BluetoothDevice>> = _discovered

    private val _isScanning = MutableStateFlow(false)
    val isScanning          : StateFlow<Boolean> = _isScanning

    private val _speed      = MutableStateFlow(0)
    val speed               : StateFlow<Int> = _speed

    private val _status     = MutableStateFlow("Disconnected")
    val status              : StateFlow<String> = _status
    private val _motorRunning = MutableStateFlow(false)
    val motorRunning: StateFlow<Boolean> = _motorRunning

    val sliders = List(6) { MutableStateFlow(0) }

    // Control continuo
    private val _contSpeed = MutableStateFlow(0)
    val contSpeed: StateFlow<Int> = _contSpeed
    

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        _discovered.value = emptyList()
        _isScanning.value = true
        service.startDiscovery(
            onDeviceFound = { dev ->
                val cur = _discovered.value.toMutableList()
                if (cur.none { it.address == dev.address }) {
                    cur += dev
                    _discovered.value = cur
                }
            },
            onFinished = {
                _isScanning.value = false
            }
        )
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        service.stopDiscovery()
        _isScanning.value = false
    }

    /** Conecta y lee ASCII puro */
    @SuppressLint("MissingPermission")
    fun connectDevice(device: BluetoothDevice) = viewModelScope.launch {
        _status.value = "Connecting…"
        try {
            service.connect(device)
            _connectedDeviceAddress.value = device.address
            _status.value = "Connected"
            service.read { data ->
                // data es texto plano: por ejemplo byteOf(D0+vel)
                Protocol.decodeSpeed(data)?.let { v ->
                    _speed.value = v
                }
            }
        } catch (e: Exception) {
            _status.value = "Error: ${e.localizedMessage}"
        }
    }

    /** Envía arranque suave: cada paso por separado con sufijo a-f */
    @SuppressLint("MissingPermission")
    fun sendArranque6P() = viewModelScope.launch {
        _status.value = "Enviando arranque suave…"
        // Envía cada paso: valor + letra a-f
        sliders.forEachIndexed { index, sf ->
            val v = sf.value.coerceIn(0,254)
            val cmd = Protocol.encodeStep(index, v)
            service.write(cmd)
            delay(100)
        }
        // Dispara la rampa después de los valores
        
        _status.value = "Arranque suave enviado"
        _motorRunning.value = true
    }

    /** Envía paro en binario */
    @SuppressLint("MissingPermission")
    fun sendParo() = viewModelScope.launch {
        _status.value = "Enviando paro…"
        val cmd = Protocol.encodeParo()
        service.write(cmd)
        _status.value = "Paro enviado"
        _motorRunning.value = false
    }

    // Control continuo handlers
    fun onContSpeedChanged(v: Int) {
        _contSpeed.value = v.coerceIn(0,254)
    }

    @SuppressLint("MissingPermission")
    fun sendContinuo() = viewModelScope.launch {
        _status.value = "Enviando arranque continuo…"
        val cmd = Protocol.encodeStartRamp()
        service.write(cmd)
        _status.value = "Continuo enviado"
        _motorRunning.value = true
    }

    fun onSliderChanged(index: Int, v: Int) {
        sliders.getOrNull(index)?.value = v.coerceIn(0,254)
    }

        /** Desconecta el dispositivo Bluetooth */
    @SuppressLint("MissingPermission")
    fun disconnectDevice() = viewModelScope.launch {
        _status.value = "Desconectando…"
        service.close()
        _connectedDeviceAddress.value = null
        _status.value = "Disconnected"
        _motorRunning.value = false
    }

    override fun onCleared() {
        super.onCleared()
        service.close()
    }
}
