package com.arranquesuave.motorcontrolapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arranquesuave.motorcontrolapp.network.WiFiNetwork
import com.arranquesuave.motorcontrolapp.viewmodel.ConfigurationStep
import com.arranquesuave.motorcontrolapp.viewmodel.WiFiSetupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WiFiSetupScreenReal(
    onNavigateBack: () -> Unit = {},
    onConfigurationComplete: () -> Unit = {},
    viewModel: WiFiSetupViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    
    // Navegar automáticamente cuando se complete la configuración
    LaunchedEffect(state.isCompleted) {
        if (state.isCompleted) {
            onConfigurationComplete()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
            }
            
            Text(
                text = "Configuración WiFi",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (state.configurationStep == ConfigurationStep.NETWORK_SELECTION) {
                IconButton(onClick = { viewModel.refreshNetworks() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Indicador de progreso
        if (state.progress > 0f) {
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Mensaje de estado
        if (state.statusMessage.isNotEmpty()) {
            Text(
                text = state.statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Contenido según el paso de configuración
        when (state.configurationStep) {
            ConfigurationStep.SCANNING -> {
                ScanningContent()
            }
            
            ConfigurationStep.NETWORK_SELECTION -> {
                NetworkSelectionContent(
                    networks = state.availableNetworks,
                    onNetworkSelected = { viewModel.selectNetwork(it) }
                )
            }
            
            ConfigurationStep.PASSWORD_INPUT -> {
                PasswordInputContent(
                    selectedNetwork = state.selectedNetwork,
                    onPasswordSubmit = { viewModel.configureWiFi(it) },
                    onBack = { viewModel.goBack() }
                )
            }
            
            ConfigurationStep.ESP32_CONNECTION,
            ConfigurationStep.WIFI_CONFIG,
            ConfigurationStep.WAITING_CONNECTION,
            ConfigurationStep.NETWORK_DISCOVERY -> {
                ConfiguringContent(
                    step = state.configurationStep,
                    canCancel = !state.isCompleted
                ) {
                    viewModel.cancelConfiguration()
                }
            }
            
            ConfigurationStep.COMPLETED -> {
                CompletedContent(
                    esp32Status = state.esp32Status,
                    onFinish = onConfigurationComplete
                )
            }
            
            ConfigurationStep.ERROR -> {
                ErrorContent(
                    error = state.error ?: "Error desconocido",
                    onRetry = { viewModel.retryConfiguration() },
                    onBack = { viewModel.goBack() }
                )
            }
        }
    }
}

@Composable
private fun ScanningContent() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Buscando redes WiFi disponibles...",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun NetworkSelectionContent(
    networks: List<WiFiNetwork>,
    onNetworkSelected: (WiFiNetwork) -> Unit
) {
    if (networks.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.WifiOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No se encontraron redes WiFi",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    } else {
        LazyColumn {
            items(networks) { network ->
                WiFiNetworkItem(
                    network = network,
                    onClick = { onNetworkSelected(network) }
                )
            }
        }
    }
}

@Composable
private fun WiFiNetworkItem(
    network: WiFiNetwork,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = network.ssid,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (network.isSecure) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Red segura",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = if (network.isSecure) "Segura" else "Abierta",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            
            // Indicador de señal
            SignalStrengthIndicator(strength = network.signalStrength)
        }
    }
}

@Composable
private fun SignalStrengthIndicator(strength: Int) {
    val color = when (strength) {
        4 -> Color(0xFF4CAF50) // Verde
        3 -> Color(0xFF8BC34A) // Verde claro
        2 -> Color(0xFFFF9800) // Naranja
        1 -> Color(0xFFFF5722) // Rojo claro
        else -> Color(0xFFF44336) // Rojo
    }
    
    // Usar icono WiFi simple y texto para indicar intensidad
    val icon = if (strength > 0) Icons.Default.Wifi else Icons.Default.WifiOff
    
    Icon(
        icon,
        contentDescription = "Señal: $strength/4",
        tint = color,
        modifier = Modifier.size(24.dp)
    )
}

@Composable
private fun PasswordInputContent(
    selectedNetwork: WiFiNetwork?,
    onPasswordSubmit: (String) -> Unit,
    onBack: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    Column {
        if (selectedNetwork != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Red seleccionada:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = selectedNetwork.ssid,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña WiFi") },
            placeholder = { Text("Ingresa la contraseña") },
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = if (passwordVisible) {
                    Icons.Default.Visibility
                } else {
                    Icons.Default.VisibilityOff
                }
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = null)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Volver")
            }
            
            Button(
                onClick = { onPasswordSubmit(password) },
                enabled = password.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text("Conectar")
            }
        }
    }
}

@Composable
private fun ConfiguringContent(
    step: ConfigurationStep,
    canCancel: Boolean,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(modifier = Modifier.size(64.dp))
        
        Spacer(modifier = Modifier.height(24.dp))
        
        val stepMessage = when (step) {
            ConfigurationStep.ESP32_CONNECTION -> "Conectando al ESP32..."
            ConfigurationStep.WIFI_CONFIG -> "Configurando WiFi en ESP32..."
            ConfigurationStep.WAITING_CONNECTION -> "Esperando conexión del ESP32..."
            ConfigurationStep.NETWORK_DISCOVERY -> "Buscando ESP32 en la red..."
            else -> "Configurando..."
        }
        
        Text(
            text = stepMessage,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (canCancel) {
            OutlinedButton(onClick = onCancel) {
                Text("Cancelar")
            }
        }
    }
}

@Composable
private fun CompletedContent(
    esp32Status: com.arranquesuave.motorcontrolapp.network.ESP32Status?,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = "Completado",
            modifier = Modifier.size(80.dp),
            tint = Color(0xFF4CAF50)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "¡Configuración Exitosa!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CAF50)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (esp32Status != null && esp32Status.connected) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Información de conexión:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Red: ${esp32Status.ssid ?: "N/A"}")
                    Text("IP: ${esp32Status.ip ?: "N/A"}")
                    if (esp32Status.signal != null) {
                        Text("Señal: ${esp32Status.signal}%")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continuar")
        }
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = "Error",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Error en la configuración",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Text(
                text = error,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Volver")
            }
            
            Button(
                onClick = onRetry,
                modifier = Modifier.weight(1f)
            ) {
                Text("Reintentar")
            }
        }
    }
}
