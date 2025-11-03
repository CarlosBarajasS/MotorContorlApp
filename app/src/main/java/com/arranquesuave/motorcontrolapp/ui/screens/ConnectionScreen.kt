// app/src/main/java/com/arranquesuave/motorcontrolapp/ui/screens/ConnectionScreen.kt
package com.arranquesuave.motorcontrolapp.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arranquesuave.motorcontrolapp.R
import com.arranquesuave.motorcontrolapp.viewmodel.MotorViewModel

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionScreen(
    viewModel: MotorViewModel,
    onLogout: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    val connectionMode by viewModel.connectionMode.collectAsState()
    val devices by viewModel.discoveredDevices.collectAsState()
    val connectedAddress by viewModel.connectedDeviceAddress.collectAsState()
    val status by viewModel.status.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    var selectedDevice by remember { mutableStateOf<String?>(null) }
    var showBluetoothDialog by remember { mutableStateOf(false) }

    // Reset selected device when connection changes
    LaunchedEffect(connectedAddress) {
        if (connectedAddress != null) selectedDevice = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Logo de fondo desvanecido
        Image(
            painter = painterResource(R.drawable.it_morelia_logo_sinfondo),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize(0.75f)
                .alpha(0.1f)
                .align(Alignment.Center),
            contentScale = ContentScale.Fit
        )

        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("Configuración de Conexión", fontSize = 20.sp) },
                    actions = {
                        IconButton(onClick = onLogout) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                        selected = false,
                        onClick = onNavigateHome
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                        selected = true,
                        onClick = onNavigateSettings
                    )
                }
            }
        ) { innerPadding ->
            LazyColumn(
                state = rememberLazyListState(),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding() + 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + 16.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                item {
                    Text(
                        text = "Selecciona el modo de conexión para controlar el motor de CD",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4A7C59)
                    )
                }
                
                // Selector de Modo de Conexión
                item {
                    ConnectionModeSelector(
                        currentMode = connectionMode,
                        onModeChanged = { viewModel.switchConnectionMode(it) }
                    )
                }
                
                // Panel de Conexión
                item {
                    ConnectionPanel(
                        viewModel = viewModel,
                        onOpenBluetoothDialog = { showBluetoothDialog = true }
                    )
                }
                
                // Información del Modo Actual
                item {
                    ModeInfoCard(connectionMode = connectionMode)
                }
            }
        }
    }
    
    // Dialog para selección de dispositivos Bluetooth
    if (showBluetoothDialog) {
        BluetoothDeviceDialog(
            devices = devices,
            scanning = isScanning,
            onSelect = { device ->
                viewModel.connectDevice(device)
                showBluetoothDialog = false
            },
            onDismiss = { showBluetoothDialog = false },
            onScanAgain = { viewModel.startDiscovery() }
        )
    }
}

@Composable
private fun ModeInfoCard(connectionMode: MotorViewModel.ConnectionMode) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9).copy(alpha = 0.8f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Información del Modo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20)
            )
            
            when (connectionMode) {
                MotorViewModel.ConnectionMode.BLUETOOTH -> {
                    InfoRow(
                        icon = Icons.Filled.Info,
                        title = "Bluetooth",
                        description = "Conexión directa al ESP32. Requiere estar cerca del dispositivo (< 10 metros)."
                    )
                    InfoRow(
                        icon = Icons.Filled.Speed,
                        title = "Baja latencia",
                        description = "Respuesta instantánea para control en tiempo real."
                    )
                }
                
                MotorViewModel.ConnectionMode.WIFI_LOCAL -> {
                    InfoRow(
                        icon = Icons.Filled.Home,
                        title = "WiFi Local",
                        description = "Conexión a través de tu red local WiFi configurada."
                    )
                    InfoRow(
                        icon = Icons.Filled.NetworkWifi,
                        title = "Mayor alcance",
                        description = "Control desde cualquier lugar dentro de tu red local."
                    )
                }
                
                MotorViewModel.ConnectionMode.MQTT_REMOTE -> {
                    InfoRow(
                        icon = Icons.Filled.Cloud,
                        title = "WiFi Remoto",
                        description = "Conexión a través de Internet desde cualquier lugar."
                    )
                    InfoRow(
                        icon = Icons.Filled.Security,
                        title = "Acceso global",
                        description = "Control desde cualquier dispositivo con conexión a Internet."
                    )
                }
                
                MotorViewModel.ConnectionMode.MQTT_TEST -> {
                    InfoRow(
                        icon = Icons.Filled.DeveloperMode,
                        title = "Modo Testing",
                        description = "Conexión a broker público para desarrollo y pruebas desde casa."
                    )
                    InfoRow(
                        icon = Icons.Filled.Code,
                        title = "Para desarrollo",
                        description = "Usa test.mosquitto.org - Perfecto para testing sin hardware físico."
                    )
                }
                
                MotorViewModel.ConnectionMode.WIFI_SETUP -> {
                    InfoRow(
                        icon = Icons.Filled.Settings,
                        title = "Configuración WiFi",
                        description = "Modo para configurar las credenciales WiFi del dispositivo ESP32."
                    )
                    InfoRow(
                        icon = Icons.Filled.Router,
                        title = "Setup inicial",
                        description = "Conecta al ESP32 para configurar su red WiFi personal."
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(16.dp).padding(top = 2.dp)
        )
        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1B5E20)
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color(0xFF4A7C59)
            )
        }
    }
}
