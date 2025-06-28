// app/src/main/java/com/arranquesuave/motorcontrolapp/ui/screens/MotorControlScreen.kt
package com.arranquesuave.motorcontrolapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arranquesuave.motorcontrolapp.viewmodel.MotorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotorControlScreen(
    viewModel: MotorViewModel
) {
    val speed     by viewModel.speed.collectAsState()
    val status    by viewModel.status.collectAsState()
    
    val sliderStates = viewModel.sliders.map { it.collectAsState() }
    val devices   by viewModel.discoveredDevices.collectAsState()
    val scanning  by viewModel.isScanning.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            TopAppBar(title = { Text("Control Motor CD", fontSize = 20.sp) })
        }
    ) { inner ->
        val top = inner.calculateTopPadding() + 16.dp
        val bot = inner.calculateBottomPadding() + 16.dp

        LazyColumn(
            state = rememberLazyListState(),
            contentPadding = PaddingValues(top = top, bottom = bot, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text("Velocidad = $speed", style = MaterialTheme.typography.titleLarge)
            }

            itemsIndexed(sliderStates) { i, s ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Paso ${i+1}: ${s.value}", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(8.dp))
                        Slider(
                            value = s.value.toFloat(),
                            onValueChange = { viewModel.onSliderChanged(i, it.toInt()) },
                            valueRange = 0f..254f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            item {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.sendContinuo() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow)
                ) {
                    Text("CONTINUO")
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { viewModel.sendArranque6P() }, modifier = Modifier.fillMaxWidth()) {
                    Text("ARRANQUE 6P")
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.sendParo() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("PARO")
                }
            }
            item {
                Spacer(Modifier.height(24.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Estado: $status", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.startDiscovery()
                            showDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Text("Conectar Bluetooth")
                    }
                }
            }
        }

        if (showDialog) {
            BluetoothDeviceDialog(
                devices = devices,
                scanning = scanning,
                onSelect = { dev ->
                    viewModel.connectDevice(dev)
                    showDialog = false
                },
                onDismiss = { showDialog = false },
                onScanAgain = { viewModel.startDiscovery() }
            )
        }
    }
}
