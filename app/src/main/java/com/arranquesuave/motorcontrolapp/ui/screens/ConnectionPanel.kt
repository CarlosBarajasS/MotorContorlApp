// app/src/main/java/com/arranquesuave/motorcontrolapp/ui/screens/ConnectionPanel.kt
package com.arranquesuave.motorcontrolapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arranquesuave.motorcontrolapp.network.ESP32Status
import com.arranquesuave.motorcontrolapp.viewmodel.MotorViewModel
import java.util.Locale

@Composable
fun ConnectionPanel(
    viewModel: MotorViewModel,
    onOpenBluetoothDialog: () -> Unit,
    esp32Status: ESP32Status?,
    onRefreshEsp32Status: () -> Unit = {},
    onNavigateToWiFiSetup: () -> Unit = {}, // ✅ NUEVO PARÁMETRO
    modifier: Modifier = Modifier
) {
    val connectionMode by viewModel.connectionMode.collectAsState()
    val status by viewModel.status.collectAsState()
    val connectedDeviceAddress by viewModel.connectedDeviceAddress.collectAsState()
    val speed by viewModel.speed.collectAsState()
    val motorMode by viewModel.motorMode.collectAsState()
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE5F2EC).copy(alpha = 0.8f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Título
            Text(
                text = "Conexión",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20)
            )
            
            // Estado de conexión
            StatusCard(
                status = status,
                motorMode = motorMode,
                connectedDevice = connectedDeviceAddress,
                speed = speed,
                esp32Status = esp32Status,
                isLocalMode = connectionMode == MotorViewModel.ConnectionMode.WIFI_LOCAL
            )
            
            // Botones según el modo
            when (connectionMode) {
                MotorViewModel.ConnectionMode.BLUETOOTH -> {
                    BluetoothConnectionButtons(
                        onOpenDialog = onOpenBluetoothDialog,
                        onDisconnect = { viewModel.disconnectDevice() },
                        isConnected = connectedDeviceAddress != null
                    )
                }
                
                MotorViewModel.ConnectionMode.WIFI_LOCAL,
                MotorViewModel.ConnectionMode.MQTT_REMOTE,
                MotorViewModel.ConnectionMode.MQTT_TEST -> {
                    if (connectionMode == MotorViewModel.ConnectionMode.WIFI_LOCAL) {
                        OutlinedButton(
                            onClick = onRefreshEsp32Status,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Verificar ESP32", fontSize = 12.sp)
                        }
                    }
                    
                    MqttConnectionButtons(
                        connectionMode = connectionMode,
                        onConnect = { viewModel.connectMqtt() },
                        onDisconnect = { viewModel.disconnectDevice() },
                        isConnected = connectedDeviceAddress != null
                    )
                }
                
                MotorViewModel.ConnectionMode.WIFI_SETUP -> {
                    // En modo WIFI_SETUP, solo mostrar información
                    // La navegación se maneja desde ConnectionModeSelector
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Settings,
                                    contentDescription = null,
                                    tint = Color(0xFF7B1FA2)
                                )
                                Text(
                                    text = "Modo Configuración WiFi",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF7B1FA2)
                                )
                            }
                            Text(
                                text = "Usa el botón 'Configurar WiFi' para acceder al escaneo de redes disponibles",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF4A4A4A)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    status: String,
    motorMode: String?,
    connectedDevice: String?,
    speed: Int,
    esp32Status: ESP32Status?,
    isLocalMode: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (connectedDevice != null) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                    contentDescription = null,
                    tint = if (connectedDevice != null) Color(0xFF29D07F) else Color(0xFFE57373),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = status,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1B5E20)
                )
            }
            
            if (connectedDevice != null) {
                Text(
                    text = "Dispositivo: $connectedDevice",
                    fontSize = 12.sp,
                    color = Color(0xFF4A7C59)
                )
                if (!motorMode.isNullOrBlank()) {
                    Text(
                        text = "Modo: ${motorMode.uppercase(Locale.getDefault())}",
                        fontSize = 12.sp,
                        color = Color(0xFF4A7C59)
                    )
                }
                Text(
                    text = "Velocidad: $speed RPM",
                    fontSize = 12.sp,
                    color = Color(0xFF4A7C59)
                )
            } else if (isLocalMode && esp32Status != null) {
                val localStatus = esp32Status
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "ESP32: ${localStatus.ip ?: "N/A"}",
                    fontSize = 12.sp,
                    color = Color(0xFF4A7C59)
                )
                if (!localStatus.deviceName.isNullOrBlank()) {
                    Text(
                        text = "ID: ${localStatus.deviceName}",
                        fontSize = 12.sp,
                        color = Color(0xFF4A7C59)
                    )
                }
                Text(
                    text = "SSID: ${localStatus.ssid ?: "Desconocido"}",
                    fontSize = 12.sp,
                    color = Color(0xFF4A7C59)
                )
                Text(
                    text = "Señal: ${localStatus.signal ?: 0}%",
                    fontSize = 12.sp,
                    color = Color(0xFF4A7C59)
                )
            }
        }
    }
}

@Composable
private fun BluetoothConnectionButtons(
    onOpenDialog: () -> Unit,
    onDisconnect: () -> Unit,
    isConnected: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!isConnected) {
            Button(
                onClick = onOpenDialog,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF29D07F)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Bluetooth,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Conectar Bluetooth", fontSize = 12.sp)
            }
        } else {
            Button(
                onClick = onDisconnect,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Desconectar", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun MqttConnectionButtons(
    connectionMode: MotorViewModel.ConnectionMode,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    isConnected: Boolean
) {
    val modeText = when (connectionMode) {
        MotorViewModel.ConnectionMode.WIFI_LOCAL -> "Conectar WiFi Local"
        MotorViewModel.ConnectionMode.MQTT_REMOTE -> "Conectar WiFi Remoto"
        MotorViewModel.ConnectionMode.MQTT_TEST -> "Conectar Testing"
        else -> "Conectar"
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!isConnected) {
            Button(
                onClick = onConnect,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF29D07F)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Wifi,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(modeText, fontSize = 12.sp)
            }
        } else {
            Button(
                onClick = onDisconnect,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Desconectar", fontSize = 12.sp)
            }
        }
    }
}
