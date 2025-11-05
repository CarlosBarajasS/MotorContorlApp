package com.arranquesuave.motorcontrolapp.network

/**
 * Modelos de datos compartidos entre los servicios de red y las capas de UI.
 */
data class WiFiCredentials(
    val ssid: String,
    val password: String,
    val security: String = "WPA2",
    val deviceName: String? = null
)

data class ESP32Status(
    val connected: Boolean = false,
    val ssid: String? = null,
    val ip: String? = null,
    val signal: Int? = null,
    val firmwareVersion: String? = null,
    val deviceName: String? = null
)

data class ESP32ConfigResponse(
    val success: Boolean,
    val message: String
)
