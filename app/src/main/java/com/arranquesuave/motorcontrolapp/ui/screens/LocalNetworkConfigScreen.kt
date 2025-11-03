// app/src/main/java/com/arranquesuave/motorcontrolapp/ui/screens/LocalNetworkConfigScreen.kt
package com.arranquesuave.motorcontrolapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arranquesuave.motorcontrolapp.utils.NetworkConfigManager
import com.arranquesuave.motorcontrolapp.viewmodel.NetworkConfigViewModel

/**
 * PANTALLA DE CONFIGURACIÓN DE RED LOCAL
 * 
 * Permite al usuario configurar su propia red WiFi local:
 * - IP del broker MQTT 
 * - Puerto MQTT
 * - IP del backend API
 * - Puerto API
 * - Nombre de la red
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalNetworkConfigScreen(
    onBack: () -> Unit,
    viewModel: NetworkConfigViewModel = viewModel()
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    // Estados del formulario
    var networkName by remember { mutableStateOf("") }
    var mqttIp by remember { mutableStateOf("") }
    var mqttPort by remember { mutableStateOf("") }
    var apiIp by remember { mutableStateOf("") }
    var apiPort by remember { mutableStateOf("") }
    var showPresets by remember { mutableStateOf(false) }
    
    // Observar configuración actual
    val currentConfig by viewModel.currentConfig.collectAsState()
    
    // Inicializar con configuración actual
    LaunchedEffect(currentConfig) {
        networkName = currentConfig.networkName
        mqttIp = currentConfig.mqttIp
        mqttPort = currentConfig.mqttPort
        apiIp = currentConfig.apiIp
        apiPort = currentConfig.apiPort
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Regresar")
            }
            
            Text(
                text = "Configurar WiFi Local",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(
                onClick = { showPresets = !showPresets }
            ) {
                Icon(Icons.Default.Settings, "Configuraciones predefinidas")
            }
        }
        
        // Información
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info, 
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Configuración de Red Local",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Configura la IP y puertos de tu Raspberry Pi local para control en tu red WiFi doméstica.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        // Configuraciones predefinidas
        if (showPresets) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Configuraciones Comunes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    viewModel.getCommonConfigurations().forEach { preset ->
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            onClick = {
                                networkName = preset.networkName
                                mqttIp = preset.mqttIp
                                mqttPort = preset.mqttPort
                                apiIp = preset.apiIp
                                apiPort = preset.apiPort
                                showPresets = false
                            }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = preset.networkName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "MQTT: ${preset.getMqttUrl()} | API: ${preset.getApiUrl()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Formulario de configuración
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Configuración Manual",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                // Nombre de red
                OutlinedTextField(
                    value = networkName,
                    onValueChange = { networkName = it },
                    label = { Text("Nombre de la Red") },
                    leadingIcon = { Icon(Icons.Default.Wifi, null) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ej: Mi Casa WiFi") }
                )
                
                // MQTT Configuration
                Text(
                    text = "Configuración MQTT",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = mqttIp,
                        onValueChange = { mqttIp = it },
                        label = { Text("IP MQTT") },
                        leadingIcon = { Icon(Icons.Default.Router, null) },
                        modifier = Modifier.weight(2f),
                        placeholder = { Text("192.168.1.100") }
                    )
                    
                    OutlinedTextField(
                        value = mqttPort,
                        onValueChange = { mqttPort = it },
                        label = { Text("Puerto") },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("1885") }
                    )
                }
                
                // API Configuration  
                Text(
                    text = "Configuración API Backend",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = apiIp,
                        onValueChange = { apiIp = it },
                        label = { Text("IP API") },
                        leadingIcon = { Icon(Icons.Default.Api, null) },
                        modifier = Modifier.weight(2f),
                        placeholder = { Text("192.168.1.100") }
                    )
                    
                    OutlinedTextField(
                        value = apiPort,
                        onValueChange = { apiPort = it },
                        label = { Text("Puerto") },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("3000") }
                    )
                }
                
                // Vista previa URLs
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Vista Previa",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "MQTT: tcp://$mqttIp:$mqttPort",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "API: http://$apiIp:$apiPort/",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        
        // Botones de acción
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    viewModel.resetToDefaults()
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Restablecer")
            }
            
            Button(
                onClick = {
                    val config = NetworkConfigManager.LocalNetworkConfig(
                        networkName = networkName,
                        mqttIp = mqttIp,
                        mqttPort = mqttPort,
                        apiIp = apiIp,
                        apiPort = apiPort
                    )
                    
                    if (config.isValidConfig()) {
                        viewModel.saveConfiguration(config)
                        onBack()
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = NetworkConfigManager.LocalNetworkConfig(
                    networkName = networkName,
                    mqttIp = mqttIp,
                    mqttPort = mqttPort,
                    apiIp = apiIp,
                    apiPort = apiPort
                ).isValidConfig()
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Guardar")
            }
        }
        
        // Información adicional
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Help,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ayuda",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• La IP debe ser la de tu Raspberry Pi en tu red local\n" +
                            "• Puertos comunes: MQTT (1883/1885), API (3000/8080)\n" +
                            "• Asegúrate que el firewall permita estas conexiones\n" +
                            "• Usa 'WiFi Remoto' para control desde Internet",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
