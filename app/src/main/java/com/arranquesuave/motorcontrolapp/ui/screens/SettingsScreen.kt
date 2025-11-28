package com.arranquesuave.motorcontrolapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arranquesuave.motorcontrolapp.viewmodel.MotorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MotorViewModel,
    modifier: Modifier = Modifier
) {
    val connectionMode by viewModel.connectionMode.collectAsState()
    val localIp by viewModel.localEsp32Ip.collectAsState()
    val esp32Status by viewModel.esp32Status.collectAsState()
    val connectedAddress by viewModel.connectedDeviceAddress.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Discovery & Connection
        item {
            SettingsSection(title = "Discovery & Conexión") {
                DiscoverySection(
                    viewModel = viewModel,
                    esp32Status = esp32Status
                )
            }
        }

        // Section 2: WiFi Configuration
        item {
            SettingsSection(title = "Configuración WiFi") {
                WiFiConfigSection(
                    viewModel = viewModel,
                    localIp = localIp
                )
            }
        }

        // Section 3: Bluetooth
        item {
            SettingsSection(title = "Bluetooth") {
                BluetoothSection(
                    viewModel = viewModel,
                    connectionMode = connectionMode,
                    connectedAddress = connectedAddress
                )
            }
        }

        // Section 4: MQTT
        item {
            SettingsSection(title = "MQTT") {
                MqttSection(viewModel = viewModel)
            }
        }

        // Section 5: Session Info
        item {
            SettingsSection(title = "Información de Sesión") {
                SessionInfoSection(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
fun DiscoverySection(
    viewModel: MotorViewModel,
    esp32Status: Map<String, Any?>?
) {
    var isDiscovering by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Buscar dispositivos ESP32 en la red",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        // Auto-discovery buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    isDiscovering = true
                    viewModel.startMqttDiscovery()
                },
                modifier = Modifier.weight(1f),
                enabled = !isDiscovering
            ) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("MQTT Discovery", fontSize = 12.sp)
            }

            Button(
                onClick = {
                    isDiscovering = true
                    viewModel.startMdnsDiscovery()
                },
                modifier = Modifier.weight(1f),
                enabled = !isDiscovering
            ) {
                Icon(Icons.Default.Cast, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("mDNS Scan", fontSize = 12.sp)
            }
        }

        Button(
            onClick = {
                isDiscovering = true
                viewModel.scanNetworkRange()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isDiscovering
        ) {
            Icon(Icons.Default.Router, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Scan Red (192.168.x.2-254)")
        }

        // Status info
        if (esp32Status != null) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = "Último dispositivo encontrado:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            InfoRow("IP AP", esp32Status["ap_ip"]?.toString() ?: "N/A")
            InfoRow("IP WiFi", esp32Status["ip_address"]?.toString() ?: "N/A")
            InfoRow("Nombre", esp32Status["device_name"]?.toString() ?: "N/A")
        }
    }
}

@Composable
fun WiFiConfigSection(
    viewModel: MotorViewModel,
    localIp: String
) {
    var showConfigDialog by remember { mutableStateOf(false) }
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var mqttBroker by remember { mutableStateOf("177.247.175.4") }
    var mqttPort by remember { mutableStateOf("1885") }
    var deviceName by remember { mutableStateOf("MotorController") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Configurar WiFi del ESP32 mediante AP (192.168.4.1)",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Button(
            onClick = { showConfigDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Settings, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Configurar WiFi en ESP32")
        }

        if (localIp.isNotBlank()) {
            InfoRow("IP Local detectada", localIp)
        }
    }

    if (showConfigDialog) {
        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            title = { Text("Configurar ESP32") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ssid,
                        onValueChange = { ssid = it },
                        label = { Text("WiFi SSID") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("WiFi Password") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = mqttBroker,
                        onValueChange = { mqttBroker = it },
                        label = { Text("MQTT Broker") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = mqttPort,
                        onValueChange = { mqttPort = it },
                        label = { Text("MQTT Port") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = deviceName,
                        onValueChange = { deviceName = it },
                        label = { Text("Nombre del Dispositivo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.configureEsp32WiFi(
                            ssid = ssid,
                            password = password,
                            mqttBroker = mqttBroker,
                            mqttPort = mqttPort.toIntOrNull() ?: 1885,
                            deviceName = deviceName
                        )
                        showConfigDialog = false
                    }
                ) {
                    Text("Enviar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfigDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun BluetoothSection(
    viewModel: MotorViewModel,
    connectionMode: String,
    connectedAddress: String?
) {
    var showBluetoothDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (connectionMode == "bluetooth") "Conectado" else "Desconectado",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (connectionMode == "bluetooth")
                    Color(0xFF4CAF50)
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Switch(
                checked = connectionMode == "bluetooth",
                onCheckedChange = { enabled ->
                    if (enabled) {
                        viewModel.switchConnectionMode("bluetooth")
                        showBluetoothDialog = true
                    } else {
                        viewModel.disconnectBluetooth()
                    }
                }
            )
        }

        if (connectionMode == "bluetooth" && connectedAddress != null) {
            InfoRow("Dispositivo", connectedAddress)
            Button(
                onClick = { viewModel.disconnectBluetooth() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Desconectar")
            }
        } else if (connectionMode == "bluetooth") {
            Button(
                onClick = { showBluetoothDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Bluetooth, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Buscar Dispositivos")
            }
        }
    }

    if (showBluetoothDialog) {
        // TODO: Implement Bluetooth discovery dialog
        // For now, just navigate to existing Bluetooth screen
        showBluetoothDialog = false
    }
}

@Composable
fun MqttSection(viewModel: MotorViewModel) {
    val mqttConnected by viewModel.mqttConnected.collectAsState()
    val connectionMode by viewModel.connectionMode.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (mqttConnected) "Conectado" else "Desconectado",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (mqttConnected)
                    Color(0xFF4CAF50)
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Switch(
                checked = connectionMode == "wifi",
                onCheckedChange = { enabled ->
                    if (enabled) {
                        viewModel.switchConnectionMode("wifi")
                    } else {
                        viewModel.disconnectMqtt()
                    }
                }
            )
        }

        InfoRow("Broker", "177.247.175.4:1885")

        if (mqttConnected) {
            Button(
                onClick = { viewModel.disconnectMqtt() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Desconectar MQTT")
            }
        } else {
            Button(
                onClick = { viewModel.connectMqtt() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Conectar MQTT")
            }
        }
    }
}

@Composable
fun SessionInfoSection(viewModel: MotorViewModel) {
    val motorRunning by viewModel.motorRunning.collectAsState()
    val motorMode by viewModel.motorMode.collectAsState()
    val connectedAddress by viewModel.connectedDeviceAddress.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        InfoRow("Estado del Motor", if (motorRunning) "Encendido" else "Apagado")
        InfoRow("Modo Actual", motorMode ?: "N/A")
        InfoRow("Dispositivo Conectado", connectedAddress ?: "Ninguno")
        InfoRow("Versión App", "1.0.0")
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
