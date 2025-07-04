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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arranquesuave.motorcontrolapp.R
import com.arranquesuave.motorcontrolapp.viewmodel.MotorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotorControlScreen(
    viewModel: MotorViewModel,
    onLogout: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    val sliderStates = viewModel.sliders.map { it.collectAsState() }

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
                item {
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.sendArranque6P() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF29D07F))
                    ) {
                        Text("Enviar Valores", color = Color.White)
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.sendContinuo() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5F2EC))
                    ) {
                        Text("Arranque Continuo", color = Color.Black)
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.sendParo() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Paro de Emergencia", color = MaterialTheme.colorScheme.onError)
                    }
                }
            }
        }
    }
}

    