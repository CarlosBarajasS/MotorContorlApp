// app/src/main/java/com/arranquesuave/motorcontrolapp/ui/screens/WiFiSetupScreen.kt
package com.arranquesuave.motorcontrolapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WiFiSetupScreen(
    onNavigateBack: () -> Unit,
    onConfigureWiFi: (ssid: String, password: String) -> Unit
) {
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var scanResults by remember { mutableStateOf(listOf<String>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración WiFi") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 📋 INSTRUCCIONES
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD).copy(alpha = 0.8f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = Color(0xFF1976D2)
                        )
                        Text(
                            text = "Configuración Inicial",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )
                    }
                    Text(
                        text = "1. Asegúrate de estar conectado a la red \"MotorControl_Setup_XXXX\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF424242)
                    )
                    Text(
                        text = "2. Ingresa tus credenciales WiFi de casa",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF424242)
                    )
                    Text(
                        text = "3. El dispositivo se conectará automáticamente",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF424242)
                    )
                }
            }

            // 📶 ESCANEO DE REDES
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8).copy(alpha = 0.8f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Redes WiFi Disponibles",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                        IconButton(
                            onClick = { 
                                // TODO: Implementar escaneo real
                                scanResults = listOf("Mi_WiFi_Casa", "INFINITUM_12AB", "Telmex_7890")
                            }
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Escanear")
                        }
                    }

                    // Lista de redes encontradas
                    scanResults.forEach { network ->
                        Card(
                            onClick = { ssid = network },
                            colors = CardDefaults.cardColors(
                                containerColor = if (ssid == network) Color(0xFF29D07F).copy(alpha = 0.3f) 
                                                else Color.White.copy(alpha = 0.7f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.Wifi, contentDescription = null)
                                Text(network)
                                if (ssid == network) {
                                    Spacer(Modifier.weight(1f))
                                    Icon(
                                        Icons.Filled.Check, 
                                        contentDescription = null,
                                        tint = Color(0xFF29D07F)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 🔐 CREDENCIALES
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0).copy(alpha = 0.8f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Credenciales de Red",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20)
                    )

                    OutlinedTextField(
                        value = ssid,
                        onValueChange = { ssid = it },
                        label = { Text("Nombre de Red (SSID)") },
                        leadingIcon = { Icon(Icons.Filled.Wifi, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña WiFi") },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // 🚀 BOTÓN DE CONFIGURACIÓN
            Button(
                onClick = {
                    isLoading = true
                    onConfigureWiFi(ssid, password)
                },
                enabled = ssid.isNotBlank() && password.isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF29D07F))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Configurando...", color = Color.White)
                } else {
                    Icon(Icons.Filled.Router, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("🔧 Configurar Dispositivo", color = Color.White)
                }
            }

            // 💡 NOTA IMPORTANTE
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5).copy(alpha = 0.8f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFF7B1FA2)
                        )
                        Text(
                            text = "Importante",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7B1FA2)
                        )
                    }
                    Text(
                        text = "Después de la configuración, tu dispositivo aparecerá en \"WiFi Local\" con su nueva dirección IP.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF424242)
                    )
                }
            }
        }
    }
}
