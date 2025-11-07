// app/src/main/java/com/arranquesuave/motorcontrolapp/controllers/BluetoothMotorController.kt
package com.arranquesuave.motorcontrolapp.controllers

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.util.Log
import com.arranquesuave.motorcontrolapp.interfaces.MotorController
import com.arranquesuave.motorcontrolapp.services.BluetoothService
import com.arranquesuave.motorcontrolapp.util.Protocol
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale

/**
 * Controlador para motor via Bluetooth SPP
 * Envuelve el BluetoothService existente
 */
class BluetoothMotorController(
    private val bluetoothService: BluetoothService,
    private val device: BluetoothDevice
) : MotorController {
    
    companion object {
        private const val TAG = "BluetoothMotorCtrl"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var onSpeedCallback: ((Int) -> Unit)? = null
    private var onStatusCallback: ((String) -> Unit)? = null
    private var onModeCallback: ((String) -> Unit)? = null
    private var readerJob: Job? = null
    private val asciiBuffer = StringBuilder()
    
    @SuppressLint("MissingPermission")
    override suspend fun connect(): Result<Unit> {
        return try {
            bluetoothService.connect(device)
            launchReaderLoop()
            onStatusCallback?.invoke("connected")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error conectando por Bluetooth", e)
            Result.failure(e)
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun disconnect() {
        bluetoothService.close()
        readerJob?.cancelAndJoin()
        readerJob = null
        asciiBuffer.clear()
        onStatusCallback?.invoke("disconnected")
    }
    
    @SuppressLint("MissingPermission")
    override suspend fun sendArranque6P(values: List<Int>): Result<Unit> {
        return try {
            val command = String(Protocol.encodeArranqueSuave(values), Charsets.UTF_8)
            writeAsciiCommand(command)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    @SuppressLint("MissingPermission")
    override suspend fun sendContinuo(): Result<Unit> {
        return try {
            val command = String(Protocol.encodeStartRamp(), Charsets.UTF_8)
            writeAsciiCommand(command)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    @SuppressLint("MissingPermission")
    override suspend fun sendParo(): Result<Unit> {
        return try {
            val command = String(Protocol.encodeParo(), Charsets.UTF_8)
            writeAsciiCommand(command)
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

    override fun setOnModeReceived(callback: (String) -> Unit) {
        onModeCallback = callback
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

    private fun launchReaderLoop() {
        readerJob?.cancel()
        readerJob = scope.launch {
            try {
                bluetoothService.read { data ->
                    val decodedSpeed = Protocol.decodeSpeed(data)
                    if (decodedSpeed != null) {
                        onSpeedCallback?.invoke(decodedSpeed)
                        val status = if (decodedSpeed > 0) "running" else "stopped"
                        onStatusCallback?.invoke(status)
                        if (decodedSpeed == 0) {
                            onModeCallback?.invoke("paro")
                        }
                        return@read
                    }

                    val text = String(data, Charsets.UTF_8)
                    handleAsciiChunk(text)
                }
            } catch (cancel: CancellationException) {
                Log.d(TAG, "Bluetooth reader cancelado")
            } catch (e: Exception) {
                Log.w(TAG, "Bluetooth reader finalizó con error", e)
                onStatusCallback?.invoke("error:${e.message ?: "read_failed"}")
                bluetoothService.close()
            } finally {
                if (isConnected().not()) {
                    onStatusCallback?.invoke("disconnected")
                }
            }
        }
    }

    private suspend fun writeAsciiCommand(command: String) {
        val trimmed = command.trim()
        val payload = (trimmed + "\n").toByteArray(Charsets.UTF_8)
        bluetoothService.write(payload)
        Log.d(TAG, "Bluetooth TX -> $trimmed")
    }

    private fun handleAsciiChunk(chunk: String) {
        chunk.forEach { char ->
            when (char) {
                '\n' -> {
                    val message = asciiBuffer.toString().trim()
                    asciiBuffer.clear()
                    if (message.isNotEmpty()) {
                        Log.d(TAG, "Bluetooth RX <- $message")
                        handleAckMessage(message)
                    }
                }
                '\r' -> Unit
                else -> if (asciiBuffer.length < 512) {
                    asciiBuffer.append(char)
                } else {
                    asciiBuffer.clear()
                }
            }
        }
    }

    private fun handleAckMessage(message: String) {
        if (!message.startsWith("{")) {
            onStatusCallback?.invoke(message)
            return
        }

        try {
            val json = JSONObject(message)
            val status = json.optString("status").lowercase(Locale.getDefault())
            val command = json.optString("command")
            val info = json.optString("message")

            when (status) {
                "ok" -> handleSuccessfulAck(command, info)
                "error" -> {
                    val errorMessage = if (info.isNullOrBlank()) {
                        "Comando rechazado ($command)"
                    } else {
                        "Error: $info"
                    }
                    onStatusCallback?.invoke(errorMessage)
                }
                else -> onStatusCallback?.invoke(message)
            }
        } catch (e: Exception) {
            Log.w(TAG, "ACK JSON parse error", e)
            onStatusCallback?.invoke(message)
        }
    }

    private fun handleSuccessfulAck(command: String?, info: String) {
        if (command.isNullOrBlank()) {
            onStatusCallback?.invoke("Comando ejecutado")
            return
        }

        val normalized = command.lowercase(Locale.getDefault())
        when (normalized) {
            "arranque6p" -> {
                onModeCallback?.invoke("arranque6p")
                onStatusCallback?.invoke("running")
            }
            "continuo", "0i" -> {
                onModeCallback?.invoke("continuo")
                onStatusCallback?.invoke("running")
            }
            "0p", "paro", "stop" -> {
                onModeCallback?.invoke("paro")
                onStatusCallback?.invoke("stopped")
                onSpeedCallback?.invoke(0)
            }
            "speed" -> {
                info.toIntOrNull()?.let { speedValue ->
                    onSpeedCallback?.invoke(speedValue)
                    if (speedValue == 0) {
                        onStatusCallback?.invoke("stopped")
                        onModeCallback?.invoke("paro")
                    }
                }
            }
            else -> onStatusCallback?.invoke("ACK $command")
        }
    }
}
