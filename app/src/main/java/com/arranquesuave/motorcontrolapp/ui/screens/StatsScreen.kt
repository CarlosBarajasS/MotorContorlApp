// app/src/main/java/com/arranquesuave/motorcontrolapp/ui/screens/StatsScreen.kt
package com.arranquesuave.motorcontrolapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
fun StatsScreen(
    viewModel: MotorViewModel,
    onNavigateBack: () -> Unit
) {
    val connectionMode by viewModel.connectionMode.collectAsState()
    val status by viewModel.status.collectAsState()
    val speed by viewModel.speed.collectAsState()
    val motorRunning by viewModel.motorRunning.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estadísticas de Uso") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 📊 ESTADÍSTICAS GENERALES
            item {
                StatsCard(
                    title = "Estadísticas Generales",
                    icon = Icons.Filled.Analytics,
                    backgroundColor = Color(0xFFE3F2FD)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatItem("Dispositivos conectados hoy", "12")
                        StatItem("Tiempo total de uso", "4h 32min")
                        StatItem("Comandos enviados", "156")
                        StatItem("Usuarios activos", "8")
                    }
                }
            }
            
            // 🔌 ESTADO ACTUAL
            item {
                StatsCard(
                    title = "Estado Actual",
                    icon = Icons.Filled.PowerSettingsNew,
                    backgroundColor = Color(0xFFE8F5E8)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatItem("Modo de conexión", connectionMode.name)
                        StatItem("Estado", status)
                        StatItem("Velocidad motor", "$speed RPM")
                        StatItem("Motor funcionando", if (motorRunning) "SÍ" else "NO")
                    }
                }
            }
            
            // 📱 DATOS DE CONEXIÓN
            item {
                StatsCard(
                    title = "Análisis de Conexiones",
                    icon = Icons.Filled.NetworkCell,
                    backgroundColor = Color(0xFFFFF3E0)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatItem("Bluetooth", "45% (54 usos)")
                        StatItem("WiFi Local", "30% (36 usos)")
                        StatItem("WiFi Remoto", "22% (26 usos)")
                        StatItem("Modo Testing", "3% (4 usos)")
                    }
                }
            }
            
            // 🏭 DATOS DEL NEGOCIO
            item {
                StatsCard(
                    title = "Métricas del Negocio",
                    icon = Icons.Filled.TrendingUp,
                    backgroundColor = Color(0xFFF3E5F5)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatItem("Dispositivos vendidos", "45")
                        StatItem("Usuarios registrados", "38")
                        StatItem("Tasa de uso diario", "73%")
                        StatItem("Ingresos estimados", "$12,350 MXN")
                    }
                }
            }
            
            // ⚡ EFICIENCIA ENERGÉTICA
            item {
                StatsCard(
                    title = "Eficiencia Energética",
                    icon = Icons.Filled.BatteryChargingFull,
                    backgroundColor = Color(0xFFE0F2F1)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatItem("Ahorro energético promedio", "23%")
                        StatItem("Arranques suaves exitosos", "142/156")
                        StatItem("Tiempo vida motor extendido", "+18%")
                        StatItem("Eficiencia PWM", "91.2%")
                    }
                }
            }
            
            // 🌍 UBICACIONES
            item {
                StatsCard(
                    title = "Ubicaciones de Uso",
                    icon = Icons.Filled.LocationOn,
                    backgroundColor = Color(0xFFEDE7F6)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatItem("Morelia, Michoacán", "85%")
                        StatItem("Ciudad de México", "8%")
                        StatItem("Guadalajara", "4%")
                        StatItem("Otras ciudades", "3%")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsCard(
    title: String,
    icon: ImageVector,
    backgroundColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF1B5E20),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )
            }
            content()
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF4A7C59),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1B5E20)
        )
    }
}
