// app/src/main/java/com/arranquesuave/motorcontrolapp/MainActivity.kt
package com.arranquesuave.motorcontrolapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.material3.Surface
import androidx.core.content.ContextCompat
import com.arranquesuave.motorcontrolapp.ui.theme.MotorControlAppTheme
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.navigation.compose.composable
import com.arranquesuave.motorcontrolapp.ui.screens.BluetoothControlScreen
import com.arranquesuave.motorcontrolapp.ui.screens.MotorControlScreen
import com.arranquesuave.motorcontrolapp.ui.screens.StatsScreen
import com.arranquesuave.motorcontrolapp.ui.screens.SettingsScreen
import com.arranquesuave.motorcontrolapp.ui.screens.WiFiSetupScreenReal
import com.arranquesuave.motorcontrolapp.viewmodel.MotorViewModel
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.currentBackStackEntryAsState
import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {

    private val viewModel: MotorViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.S)
    private val requestPerms =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            val okConnect = perms[Manifest.permission.BLUETOOTH_CONNECT] ?: false
            val okScan    = perms[Manifest.permission.BLUETOOTH_SCAN]   ?: false
            val okFineLoc = perms[Manifest.permission.ACCESS_FINE_LOCATION]   ?: false
            val okCoarse  = perms[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            if (okConnect && okScan && (okFineLoc || okCoarse)) {
                viewModel.startDiscovery()
            }
        }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MotorControlAppTheme {
                Surface {
                    val navController = rememberNavController()
                    val backStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = backStackEntry?.destination?.route
                    val context = LocalContext.current
                    val activity = context as Activity
                    var lastBackPressTime by remember { mutableStateOf(0L) }

                    BackHandler {
                        when (currentRoute) {
                            "bluetooth" -> navController.popBackStack()
                            "wifi_setup" -> navController.popBackStack()
                            "main" -> {
                                val now = System.currentTimeMillis()
                                if (now - lastBackPressTime < 2000L) {
                                    activity.finish()
                                } else {
                                    lastBackPressTime = now
                                    Toast.makeText(context, "Presione 2 veces para confirmar la salida", Toast.LENGTH_SHORT).show()
                                }
                            }
                            else -> navController.popBackStack()
                        }
                    }

                    NavHost(navController, startDestination = "main") {
                        composable("main") {
                            MainScreenWithTabs(viewModel = viewModel)
                        }

                        composable("bluetooth") {
                            BluetoothControlScreen(
                                viewModel = viewModel,
                                onNavigateHome = { navController.navigate("main") },
                                onNavigateSettings = { navController.navigate("main") }
                            )
                        }

                        composable("wifi_setup") {
                            WiFiSetupScreenReal(
                                onNavigateBack = { navController.popBackStack() },
                                onConfigurationComplete = {
                                    viewModel.onWiFiSetupCompleted()
                                    Toast.makeText(
                                        context,
                                        "✅ Configuración WiFi completada exitosamente",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
        ensurePermissions()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun ensurePermissions() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED) {
            needed += Manifest.permission.BLUETOOTH_CONNECT
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED) {
            needed += Manifest.permission.BLUETOOTH_SCAN
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            needed += Manifest.permission.ACCESS_FINE_LOCATION
            needed += Manifest.permission.ACCESS_COARSE_LOCATION
        }
        if (needed.isNotEmpty()) {
            requestPerms.launch(needed.toTypedArray())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenWithTabs(viewModel: MotorViewModel) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Control") },
                    label = { Text("Control") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Stats") },
                    label = { Text("Stats") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> MotorControlScreen(viewModel = viewModel)
                1 -> SettingsScreen(viewModel = viewModel)
                2 -> StatsScreen(
                    viewModel = viewModel,
                    onNavigateBack = {}
                )
            }
        }
    }
}
