package com.arranquesuave.motorcontrolapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arranquesuave.motorcontrolapp.network.ESP32Status
import com.arranquesuave.motorcontrolapp.network.WiFiNetwork
import com.arranquesuave.motorcontrolapp.viewmodel.ConfigurationStep
import com.arranquesuave.motorcontrolapp.viewmodel.WiFiSetupState
import com.arranquesuave.motorcontrolapp.viewmodel.WiFiSetupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WiFiSetupScreenReal(
    onNavigateBack: () -> Unit = {},
    onConfigurationComplete: () -> Unit = {},
    viewModel: WiFiSetupViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(state.isCompleted) {
        if (state.isCompleted) {
            onConfigurationComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Configuración WiFi",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sigue los pasos para vincular tu ESP32",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    when (state.configurationStep) {
                        ConfigurationStep.NETWORK_SELECTION -> {
                            IconButton(onClick = { viewModel.refreshNetworks() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Actualizar redes")
                            }
                            TextButton(onClick = { viewModel.skipToOperationalMode() }) {
                                Text("Usar configurado")
                            }
                        }
                        ConfigurationStep.ERROR -> {
                            TextButton(onClick = { viewModel.retryConfiguration() }) {
                                Text("Reintentar")
                            }
                        }
                        else -> {}
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StepProgressIndicator(state)

            if (state.progress > 0f && state.configurationStep != ConfigurationStep.COMPLETED) {
                LinearProgressIndicator(
                    progress = { state.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (state.statusMessage.isNotEmpty()) {
                StatusMessageCard(
                    message = state.statusMessage,
                    type = when (state.configurationStep) {
                        ConfigurationStep.ERROR -> StatusMessageType.Error
                        ConfigurationStep.COMPLETED -> StatusMessageType.Success
                        else -> StatusMessageType.Info
                    }
                )
            }

            when (state.configurationStep) {
                ConfigurationStep.SCANNING -> {
                    GuidanceCard(
                        title = "Estamos buscando tu ESP32",
                        tips = listOf(
                            "Mantén el módulo encendido y cerca de tu teléfono.",
                            "Si el LED azul está fijo, el dispositivo está listo para configurarse."
                        )
                    )
                    ScanningContent()
                }

                ConfigurationStep.NETWORK_SELECTION -> {
                    val hasNetworks = state.availableNetworks.isNotEmpty()

                    GuidanceCard(
                        title = if (hasNetworks) "Selecciona la red de tu hogar" else "No encontramos tu ESP32 automáticamente",
                        tips = if (hasNetworks) {
                            listOf(
                                "Elige la red WiFi a la que quieres conectar el ESP32.",
                                "Si no ves tu red, pulsa \"Actualizar\" o acércate al módem.",
                                "Si ya configuraste tu dispositivo antes, puedes usar \"Usar configurado\"."
                            )
                        } else {
                            listOf(
                                "Pulsa \"Reintentar\" para un nuevo escaneo.",
                                "Usa \"Configurar manualmente\" si conoces la IP actual del ESP32.",
                                "Verifica que la red \"ESP32-MotorSetup\" esté visible para entrar en modo configuración."
                            )
                        }
                    )

                    if (hasNetworks) {
                        ESP32AlreadyConfiguredCard(
                            onUseConfigured = { viewModel.skipToOperationalMode() },
                            onRefreshNetworks = { viewModel.refreshNetworks() }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        NetworkSelectionContent(
                            networks = state.availableNetworks,
                            onNetworkSelected = { viewModel.selectNetwork(it) }
                        )
                    } else {
                        ESP32NotDetectedCard(
                            onTryAgain = { viewModel.retryConfiguration() },
                            onManualConfig = { viewModel.beginManualConfiguration() },
                            onSkipToOperational = { viewModel.skipToOperationalMode() }
                        )
                    }
                }

                ConfigurationStep.PASSWORD_INPUT -> {
                    val isManual = state.selectedNetwork?.ssid == "CONFIGURACIÓN_MANUAL"
                    GuidanceCard(
                        title = if (isManual) "Ingresa la IP del ESP32" else "Introduce la contraseña",
                        tips = if (isManual) {
                            listOf(
                                "Confirma que el ESP32 ya está dentro de tu red local.",
                                "Escribe la IP asignada por tu router (ej. 192.168.1.120).",
                                "Si no conoces la IP, revisa la app del router o usa un escáner de red."
                            )
                        } else {
                            listOf(
                                "Utiliza la contraseña tal como la usas en otros dispositivos.",
                                "Las mayúsculas y caracteres especiales deben coincidir.",
                                "Puedes mostrar u ocultar la contraseña con el ícono del ojo."
                            )
                        }
                    )

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
                    GuidanceCard(
                        title = "Estamos preparando tu ESP32",
                        tips = listOf(
                            "No cierres la aplicación mientras terminamos.",
                            "Tu teléfono puede cambiar momentáneamente de red durante el proceso.",
                            "Si tarda demasiado, cancela y vuelve a intentarlo."
                        )
                    )
                    ConfiguringContent(
                        step = state.configurationStep,
                        canCancel = !state.isCompleted
                    ) {
                        viewModel.cancelConfiguration()
                    }
                }

                ConfigurationStep.COMPLETED -> {
                    StatusMessageCard(
                        message = "Tu ESP32 ya está listo y conectado a tu red.",
                        type = StatusMessageType.Success
                    )
                    CompletedContent(
                        esp32Status = state.esp32Status,
                        onFinish = onConfigurationComplete
                    )
                }

                ConfigurationStep.ERROR -> {
                    StatusMessageCard(
                        message = state.error ?: "Error desconocido. Revisa la conexión y vuelve a intentar.",
                        type = StatusMessageType.Error
                    )
                    ErrorContent(
                        error = state.error ?: "Error desconocido",
                        onRetry = { viewModel.retryConfiguration() },
                        onBack = { viewModel.goBack() }
                    )
                }
            }
        }
    }
}

private data class StepDescriptor(val step: ConfigurationStep, val label: String)

private val setupSteps = listOf(
    StepDescriptor(ConfigurationStep.SCANNING, "Detectar"),
    StepDescriptor(ConfigurationStep.NETWORK_SELECTION, "Seleccionar"),
    StepDescriptor(ConfigurationStep.PASSWORD_INPUT, "Contraseña"),
    StepDescriptor(ConfigurationStep.ESP32_CONNECTION, "Vincular"),
    StepDescriptor(ConfigurationStep.WIFI_CONFIG, "Enviar datos"),
    StepDescriptor(ConfigurationStep.WAITING_CONNECTION, "Esperar"),
    StepDescriptor(ConfigurationStep.NETWORK_DISCOVERY, "Verificar"),
    StepDescriptor(ConfigurationStep.COMPLETED, "Listo")
)

@Composable
private fun StepProgressIndicator(state: WiFiSetupState) {
    val displayStep = when (state.configurationStep) {
        ConfigurationStep.ERROR -> when {
            state.isCompleted -> ConfigurationStep.COMPLETED
            state.selectedNetwork != null -> ConfigurationStep.PASSWORD_INPUT
            else -> ConfigurationStep.NETWORK_SELECTION
        }
        else -> state.configurationStep
    }

    val currentIndex = setupSteps.indexOfFirst { it.step == displayStep }.let { index ->
        if (index == -1) 0 else index
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        setupSteps.forEachIndexed { index, descriptor ->
            StepNode(
                number = index + 1,
                label = descriptor.label,
                isCurrent = index == currentIndex && state.configurationStep != ConfigurationStep.COMPLETED,
                isComplete = index < currentIndex || (state.configurationStep == ConfigurationStep.COMPLETED && descriptor.step == ConfigurationStep.COMPLETED)
            )

            if (index < setupSteps.lastIndex) {
                StepDivider(
                    modifier = Modifier.weight(1f),
                    isActive = index < currentIndex
                )
            }
        }
    }
}

@Composable
private fun StepNode(
    number: Int,
    label: String,
    isCurrent: Boolean,
    isComplete: Boolean
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isCurrent || isComplete) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    color = when {
                        isComplete -> activeColor
                        isCurrent -> activeColor
                        else -> inactiveColor
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isComplete && !isCurrent) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Text(
                    text = number.toString(),
                    color = contentColor,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = if (isCurrent || isComplete) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.widthIn(min = 60.dp)
        )
    }
}

@Composable
private fun StepDivider(
    modifier: Modifier = Modifier,
    isActive: Boolean
) {
    Box(
        modifier = modifier
            .height(2.dp)
            .background(
                color = if (isActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                },
                shape = RoundedCornerShape(2.dp)
            )
    )
}

private enum class StatusMessageType { Info, Success, Error }

@Composable
private fun StatusMessageCard(
    message: String,
    type: StatusMessageType,
    modifier: Modifier = Modifier
) {
    val (containerColor, contentColor, icon) = when (type) {
        StatusMessageType.Success -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            Icons.Default.CheckCircle
        )

        StatusMessageType.Error -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            Icons.Default.Error
        )

        StatusMessageType.Info -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.Info
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor
            )
        }
    }
}

@Composable
private fun GuidanceCard(
    title: String,
    tips: List<String>,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Lightbulb
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            tips.forEachIndexed { index, tip ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = if (index == tips.lastIndex) 0.dp else 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Redes disponibles",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            networks.forEach { network ->
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
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SignalStrengthIndicator(strength = network.signalStrength)
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            }
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
    
    // ✅ DETECTAR SI ES CONFIGURACIÓN MANUAL DE IP
    val isManualIPConfig = selectedNetwork?.ssid == "CONFIGURACIÓN_MANUAL"
    
    Column {
        if (selectedNetwork != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isManualIPConfig) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (isManualIPConfig) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Configuración Manual ESP32",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ingresa la IP del ESP32 en tu red local",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    } else {
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
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        if (isManualIPConfig) {
            // ✅ CAMPO PARA IP MANUAL
            OutlinedTextField(
                value = password,
                onValueChange = { 
                    // Filtrar solo números, puntos y limitar longitud
                    if (it.length <= 15 && it.all { char -> char.isDigit() || char == '.' }) {
                        password = it
                    }
                },
                label = { Text("IP del ESP32") },
                placeholder = { Text("Ej: 192.168.1.100") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Computer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = {
                    Text(
                        text = "Verifica que el ESP32 esté encendido y conectado a tu red WiFi",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            )
        } else {
            // ✅ CAMPO PARA CONTRASEÑA NORMAL
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
                onClick = { onPasswordSubmit(password) },
                enabled = if (isManualIPConfig) {
                    password.isNotBlank() && password.contains(".")
                } else {
                    password.isNotBlank()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    if (isManualIPConfig) "Conectar" else "Conectar"
                )
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
    esp32Status: ESP32Status?,
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
                    if (!esp32Status.deviceName.isNullOrBlank()) {
                        Text("ID: ${esp32Status.deviceName}")
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

/**
 * ✅ NUEVO: Componente cuando ESP32 no se detectó automáticamente
 */
@Composable
private fun ESP32NotDetectedCard(
    onTryAgain: () -> Unit,
    onManualConfig: () -> Unit,
    onSkipToOperational: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "ESP32 No Detectado",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "No se pudo detectar automáticamente el ESP32. ¿Qué quieres hacer?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Primera fila de botones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSkipToOperational,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Usar Configurado",
                        fontSize = 12.sp
                    )
                }
                
                OutlinedButton(
                    onClick = onTryAgain,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Reintentar",
                        fontSize = 12.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Segunda fila
            Button(
                onClick = onManualConfig,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Configurar Manualmente")
            }
        }
    }
}

/**
 * ✅ NUEVO: Componente para mostrar opción de usar ESP32 ya configurado
 */
@Composable
private fun ESP32AlreadyConfiguredCard(
    onUseConfigured: () -> Unit,
    onRefreshNetworks: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Wifi,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "ESP32 Detectado",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "¿Tu ESP32 ya está configurado y conectado a tu red WiFi?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onUseConfigured,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Usar Configurado")
                }
                
                OutlinedButton(
                    onClick = onRefreshNetworks,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Actualizar lista")
                }
            }
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
