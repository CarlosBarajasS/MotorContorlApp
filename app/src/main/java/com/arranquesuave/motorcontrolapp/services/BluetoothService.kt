// app/src/main/java/com/arranquesuave/motorcontrolapp/services/BluetoothService.kt
package com.arranquesuave.motorcontrolapp.services

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.Manifest
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import java.io.IOException
import java.util.*

class BluetoothService(private val ctx: Context) {
    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var socket: BluetoothSocket? = null
    private val sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // --- Discovery ---
    private var discoveryReceiver: BroadcastReceiver? = null

    /** Inicia un scan de dispositivos clásicos en rango */
    @SuppressLint("MissingPermission")
    fun startDiscovery(
        onDeviceFound: (BluetoothDevice) -> Unit,
        onFinished: () -> Unit
    ) {
        adapter?.takeIf { it.isEnabled }?.apply {
            if (isDiscovering) cancelDiscovery()
            // Registra el BroadcastReceiver
            discoveryReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    when (intent?.action) {
                        BluetoothDevice.ACTION_FOUND -> {
                            val device: BluetoothDevice? =
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                            device?.let(onDeviceFound)
                        }
                        BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                            onFinished()
                            stopDiscovery()
                        }
                    }
                }
            }
            ctx.registerReceiver(
                discoveryReceiver,
                IntentFilter(BluetoothDevice.ACTION_FOUND).apply {
                    addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                }
            )
            startDiscovery()
        } ?: onFinished()
    }

    /** Cancela el scan y desregistra el receiver */
    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        try {
            adapter?.cancelDiscovery()
            discoveryReceiver?.let {
                ctx.unregisterReceiver(it)
                discoveryReceiver = null
            }
        } catch (_: Exception) {}
    }

    // --- Conexión SPP clásica ---
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun getPairedDevices(): List<BluetoothDevice> =
        adapter?.bondedDevices?.toList() ?: emptyList()

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun connect(device: BluetoothDevice) = withContext(Dispatchers.IO) {
        adapter?.cancelDiscovery()
        var attempt = 0
        var lastError: Exception? = null
        while (attempt < 3) {
            try {
                @Suppress("MissingPermission")
                val sock = device.createInsecureRfcommSocketToServiceRecord(sppUuid)
                @Suppress("MissingPermission")
                sock.connect()
                socket = sock
                return@withContext
            } catch (e: Exception) {
                lastError = e
                attempt++
                delay(1000)
            }
        }
        throw lastError ?: IOException("Failed to connect device")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun write(data: ByteArray) = withContext(Dispatchers.IO) {
        @Suppress("MissingPermission")
        socket?.outputStream?.write(data)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun read(onData: (ByteArray) -> Unit) = withContext(Dispatchers.IO) {
        val buf = ByteArray(1024)
        @Suppress("MissingPermission")
        val input = socket?.inputStream ?: return@withContext
        while (true) {
            val len = input.read(buf)
            if (len > 0) onData(buf.copyOf(len))
        }
    }

    fun close() {
        try { socket?.close() } catch (_: IOException) {}
    }
}
