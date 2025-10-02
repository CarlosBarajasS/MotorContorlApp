package com.arranquesuave.motorcontrolapp.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arranquesuave.motorcontrolapp.viewmodel.MotorViewModel
import androidx.compose.material3.ExperimentalMaterial3Api

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothControlScreen(
    viewModel: MotorViewModel,
    onNavigateHome: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    val devices by viewModel.discoveredDevices.collectAsState()
    val connectedAddress by viewModel.connectedDeviceAddress.collectAsState()
    val status by viewModel.status.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    var selected by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(connectedAddress) {
        if (connectedAddress != null) selected = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bluetooth Control") }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Maneja la conexion del control de motor.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            Text(text = "Estado: $status", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))

            when {
                connectedAddress != null -> {
                    Text(
                        text = "Conectado a: $connectedAddress",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.disconnectDevice() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Desconectar")
                    }
                }

                isScanning -> {
                    Text(text = "Buscando dispositivos…", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.stopDiscovery() },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Detener búsqueda", color = MaterialTheme.colorScheme.onPrimary)
                    }
                    // Mostrar dispositivos encontrados durante el escaneo
                    if (devices.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(devices) { dev ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selected = dev.address }
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Bluetooth,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(dev.name ?: dev.address, style = MaterialTheme.typography.bodyLarge)
                                        if (selected == dev.address) {
                                            Spacer(Modifier.weight(1f))
                                            Icon(Icons.Filled.Check, contentDescription = null)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = {
                                selected?.let { addr ->
                                    viewModel.discoveredDevices.value.find { it.address == addr }
                                        ?.let { viewModel.connectDevice(it) }
                                }
                            },
                            enabled = selected != null && selected != connectedAddress,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Conectar", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }

                devices.isEmpty() -> {
                    Button(
                        onClick = { viewModel.startDiscovery() },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Buscar dispositivos", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }

                else -> {
                    Button(
                        onClick = {
                            selected = null
                            viewModel.startDiscovery()
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Buscar nuevamente", color = MaterialTheme.colorScheme.onPrimary)
                    }
                    Spacer(Modifier.height(16.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(devices) { dev ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selected = dev.address }
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Bluetooth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(dev.name ?: dev.address, style = MaterialTheme.typography.bodyLarge)
                                    if (selected == dev.address) {
                                        Spacer(Modifier.weight(1f))
                                        Icon(Icons.Filled.Check, contentDescription = null)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = {
                            selected?.let { addr ->
                                viewModel.discoveredDevices.value.find { it.address == addr }
                                    ?.let { viewModel.connectDevice(it) }
                            }
                        },
                        enabled = selected != null && selected != connectedAddress,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Conectar", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}
