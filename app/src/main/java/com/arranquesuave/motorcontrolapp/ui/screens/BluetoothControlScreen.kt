package com.arranquesuave.motorcontrolapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arranquesuave.motorcontrolapp.viewmodel.MotorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothControlScreen(
    viewModel: MotorViewModel,
    onBack: () -> Unit
) {
    val devices by viewModel.discoveredDevices.collectAsState()
    var selected by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bluetooth Control") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp)
        ) {
            Text(
                text = "Manage your Bluetooth connection to control the motor.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(24.dp))
            if (devices.isEmpty()) {
                Button(
                    onClick = { viewModel.startDiscovery() },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Connect")
                }
            } else {
                LazyColumn {
                    items(devices) { dev ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selected = dev.address }
                                .padding(8.dp)
                        ) {
                            Icon(Icons.Default.Bluetooth, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(dev.name ?: dev.address)
                            if (selected == dev.address) {
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Default.Check, contentDescription = "Selected")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        selected?.let { addr ->
                            viewModel.discoveredDevices.value.find { it.address == addr }?.let { viewModel.connectDevice(it) }
                        }
                    },
                    enabled = selected != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Connect")
                }
            }
        }
    }
}
