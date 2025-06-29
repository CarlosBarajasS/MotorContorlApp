package com.arranquesuave.motorcontrolapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arranquesuave.motorcontrolapp.viewmodel.MotorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothControlScreen(
    viewModel: MotorViewModel,
    onLogout: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    val devices by viewModel.discoveredDevices.collectAsState()
    val connectedAddress by viewModel.connectedDeviceAddress.collectAsState()
    var selected by remember { mutableStateOf(connectedAddress) }
    LaunchedEffect(connectedAddress) { selected = connectedAddress }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bluetooth Control") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = "Logout")
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
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (selected == connectedAddress) "Connected" else "Connect", color = MaterialTheme.colorScheme.onPrimary)
                }
            } else {
                LazyColumn {
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
                            viewModel.discoveredDevices.value.find { it.address == addr }?.let { viewModel.connectDevice(it) }
                        }
                    },
                    enabled = selected != null && selected != connectedAddress,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Connect", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}
