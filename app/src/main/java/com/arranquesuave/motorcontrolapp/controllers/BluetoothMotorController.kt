// app/src/main/java/com/arranquesuave/motorcontrolapp/controllers/BluetoothMotorController.kt
package com.arranquesuave.motorcontrolapp.controllers

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import com.arranquesuave.motorcontrolapp.interfaces.MotorController
import com.arranquesuave.motorcontrolapp.services.BluetoothService
import com.arranquesuave.motorcontrolapp.util.Protocol
import kotlinx.coroutines.delay

/**
 * Controlador para motor via Bluetooth SPP
 * Envuelve el BluetoothService existente
 */
class BluetoothMotorController(
    private val bluetoothService: BluetoothService,
    private val device: BluetoothDevice
) : MotorController {
    
    private var onSpeedCallback: ((Int) -> Unit)? = null
    private var onStatusCallback: ((String) -> Unit)? = null
    
    @SuppressLint("MissingPermission")
    override suspend fun connect(): Result<Unit> {
        return try {
            bluetoothService.connect(device)
            
            // Iniciar lectura de datos en segundo plano
            bluetoothService.read { data ->
                val speed = Protocol.decodeSpeed(data) ?: 0
                onSpeedCallback?.invoke(speed)
                
                // Determinar estado basado en velocidad
                val status = if (speed > 0) "running" else "stopped"
                onStatusCallback?.invoke(status)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    @SuppressLint("MissingPermission")
    override suspend fun disconnect() {
        bluetoothService.close()
    }
    
    @SuppressLint("MissingPermission")
    override suspend fun sendArranque6P(values: List<Int>): Result<Unit> {
        return try {
            // Usar Protocol.encodeArranqueSuave que ya devuelve ByteArray
            val command = Protocol.encodeArranqueSuave(values)
            bluetoothService.write(command)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    @SuppressLint("MissingPermission")
    override suspend fun sendContinuo(): Result<Unit> {
        return try {
            // Usar Protocol.encodeStartRamp que ya devuelve ByteArray
            val command = Protocol.encodeStartRamp()
            bluetoothService.write(command)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    @SuppressLint("MissingPermission")
    override suspend fun sendParo(): Result<Unit> {
        return try {
            // Usar Protocol.encodeParo que ya devuelve ByteArray
            val command = Protocol.encodeParo()
            bluetoothService.write(command)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun setOnSpeedReceived(callback: (Int) -> Unit) {
        onSpeedCallback = callback
    }
    
    override fun setOnStatusReceived(callback: (String) -> Unit) {
        onStatusCallback = callback
    }
    
    override fun isConnected(): Boolean = bluetoothService.isConnected()
    
    @SuppressLint("MissingPermission")
    override fun getConnectionInfo(): String = "Bluetooth: ${device.name ?: device.address}"
    
    // Métodos opcionales para telemetría extendida (no soportados en Bluetooth)
    override fun setOnCurrentReceived(callback: (Float) -> Unit) {
        // Bluetooth no soporta telemetría de corriente
    }
    
    override fun setOnVoltageReceived(callback: (Float) -> Unit) {
        // Bluetooth no soporta telemetría de voltaje
    }
}
