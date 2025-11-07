// app/src/main/java/com/arranquesuave/motorcontrolapp/ui/screens/MotorControlScreen.kt
package com.arranquesuave.motorcontrolapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.arranquesuave.motorcontrolapp.R
import com.arranquesuave.motorcontrolapp.viewmodel.MotorViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotorControlScreen(
    viewModel: MotorViewModel,
    onLogout: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateToWiFiSetup: () -> Unit = {} // ✅ NUEVO PARÁMETRO
) {
    val sliderStates = viewModel.sliders.map { it.collectAsState() }
    val motorRunning by viewModel.motorRunning.collectAsState()
    val connectionMode by viewModel.connectionMode.collectAsState()
    val status by viewModel.status.collectAsState()
    val motorMode by viewModel.motorMode.collectAsState()
    val currentUrl by viewModel.currentUrl.collectAsState()
    val connectedAddress by viewModel.connectedDeviceAddress.collectAsState()
    val localIp by viewModel.localEsp32Ip.collectAsState()
    val esp32Status by viewModel.esp32Status.collectAsState()
    val normalizedMode = motorMode?.lowercase(Locale.getDefault())
    val isStoppedMode = normalizedMode == "paro" || normalizedMode == "stop" || normalizedMode == "stopped"
    val canSendStart = !motorRunning || isStoppedMode
    val canSendStop = motorRunning && !isStoppedMode

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
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            topBar = {
                TopAppBar(
                    title = { Text("Control de Motor", fontSize = 20.sp) },
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
                        selected = true,
                        onClick = onNavigateHome
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                        selected = false,
                        onClick = onNavigateSettings
                    )
                }
            }
        ) { inner ->
            val topPadding = inner.calculateTopPadding() + 16.dp
            val botPadding = inner.calculateBottomPadding() + 16.dp

            LazyColumn(
                state = rememberLazyListState(),
                contentPadding = PaddingValues(top = topPadding, bottom = botPadding, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // ✅ SELECTOR DE MODO DE CONEXIÓN
                item {
                    ConnectionModeSelector(
                        currentMode = connectionMode,
                        localIp = localIp,
                        onModeChanged = { newMode ->
                            viewModel.switchConnectionMode(newMode)
                        },
                        onNavigateToWiFiSetup = onNavigateToWiFiSetup // ✅ PASAR NAVEGACIÓN
                    )
                }

                // ✅ PANEL DE CONEXIÓN Y ESTADO
                item {
                    ConnectionPanel(
                        viewModel = viewModel,
                        onOpenBluetoothDialog = {
                            // Navegar a la pantalla de Bluetooth para discovery
                            onNavigateSettings()
                        },
                        esp32Status = esp32Status,
                        onRefreshEsp32Status = { viewModel.refreshEsp32Status() }
                    )
                }

                // ✅ CONTROL DE MOTOR (solo si está conectado)
                if (connectedAddress != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE5F2EC).copy(alpha = 0.8f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Control PWM - Arranque Suave",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Configura los 6 valores PWM para el arranque suave del motor",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF4A7C59)
                                )
                            }
                        }
                    }

                    // PWM Sliders
                    itemsIndexed(sliderStates) { i, s ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE5F2EC).copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("PWM ${i+1}", style = MaterialTheme.typography.bodyLarge)
                                    Text("${s.value}", style = MaterialTheme.typography.bodyLarge)
                                }
                                Spacer(Modifier.height(8.dp))
                                Slider(
                                    value = s.value.toFloat(),
                                    onValueChange = { viewModel.onSliderChanged(i, it.toInt()) },
                                    valueRange = 0f..254f,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = SliderDefaults.colors(
                                        activeTrackColor = Color(0xFF29D07F),
                                        inactiveTrackColor = Color(0xFFE5F2EC),
                                        thumbColor = Color(0xFF29D07F)
                                    )
                                )
                            }
                        }
                    }

                    // Botones de control
                    item {
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.sendArranque6P() },
                            enabled = canSendStart,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF29D07F))
                        ) {
                            Text("🚀 Enviar Arranque Suave", color = Color.White)
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.sendContinuo() },
                            enabled = canSendStart,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5F2EC))
                        ) {
                            Text("⚡ Arranque Continuo", color = Color.Black)
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.sendParo() },
                            enabled = canSendStop,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("🛑 Paro de Emergencia", color = MaterialTheme.colorScheme.onError)
                        }
                    }
                } else {
                    // ✅ MENSAJE CUANDO NO HAY CONEXIÓN
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0).copy(alpha = 0.8f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "🔌 Sin Conexión",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Selecciona un modo de conexión y conecta tu dispositivo para comenzar a controlar el motor.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF8D6E63)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

    
