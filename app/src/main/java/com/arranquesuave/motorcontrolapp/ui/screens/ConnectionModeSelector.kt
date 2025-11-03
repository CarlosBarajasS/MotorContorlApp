// app/src/main/java/com/arranquesuave/motorcontrolapp/ui/screens/ConnectionModeSelector.kt
package com.arranquesuave.motorcontrolapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arranquesuave.motorcontrolapp.viewmodel.MotorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionModeSelector(
    currentMode: MotorViewModel.ConnectionMode,
    onModeChanged: (MotorViewModel.ConnectionMode) -> Unit,
    onNavigateToWiFiSetup: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE5F2EC).copy(alpha = 0.8f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Modo de Conexión",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20)
            )
            
            // Bluetooth Mode
            ConnectionModeChip(
                icon = Icons.Filled.Bluetooth,
                title = "Bluetooth",
                subtitle = "Conexión directa al ESP32",
                selected = currentMode == MotorViewModel.ConnectionMode.BLUETOOTH,
                onClick = { onModeChanged(MotorViewModel.ConnectionMode.BLUETOOTH) }
            )
            
            // Remote WiFi Mode
            ConnectionModeChip(
                icon = Icons.Filled.Cloud,
                title = "WiFi Remoto",
                subtitle = "Internet (motor-control-itm.duckdns.org)",
                selected = currentMode == MotorViewModel.ConnectionMode.MQTT_REMOTE,
                onClick = { onModeChanged(MotorViewModel.ConnectionMode.MQTT_REMOTE) }
            )
            
            // WiFi Setup Mode - Para conectarse a red local
            ConnectionModeChip(
                icon = Icons.Filled.Settings,
                title = "Configurar WiFi",
                subtitle = "Conectar a red WiFi local",
                selected = currentMode == MotorViewModel.ConnectionMode.WIFI_SETUP,
                onClick = { 
                    onModeChanged(MotorViewModel.ConnectionMode.WIFI_SETUP)
                    // Navegar automáticamente a la pantalla de configuración WiFi
                    onNavigateToWiFiSetup?.invoke()
                }
            )
            
            // Test Mode - Para desarrollo desde casa
            ConnectionModeChip(
                icon = Icons.Filled.DeveloperMode,
                title = "Modo Testing",
                subtitle = "Desarrollo (test.mosquitto.org)",
                selected = currentMode == MotorViewModel.ConnectionMode.MQTT_TEST,
                onClick = { onModeChanged(MotorViewModel.ConnectionMode.MQTT_TEST) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConnectionModeChip(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (selected) Color.White else Color(0xFF1B5E20)
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (selected) Color.White else Color(0xFF1B5E20)
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = if (selected) Color.White.copy(alpha = 0.8f) else Color(0xFF4A7C59)
                    )
                }
            }
        },
        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFF29D07F),
            selectedLabelColor = Color.White,
            containerColor = Color.White.copy(alpha = 0.5f),
            labelColor = Color(0xFF1B5E20)
        )
    )
}
