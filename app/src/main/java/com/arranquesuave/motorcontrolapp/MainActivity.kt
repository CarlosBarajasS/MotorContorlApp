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
import androidx.navigation.compose.composable
import com.arranquesuave.motorcontrolapp.ui.screens.BluetoothControlScreen
import com.arranquesuave.motorcontrolapp.ui.screens.MotorControlScreen
import com.arranquesuave.motorcontrolapp.viewmodel.MotorViewModel
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.currentBackStackEntryAsState
import android.app.Activity
import android.widget.Toast
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {

    private val viewModel: MotorViewModel by viewModels()

    // Preparamos la petición múltiple de permisos
    @RequiresApi(Build.VERSION_CODES.S)
    private val requestPerms =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            val okConnect = perms[Manifest.permission.BLUETOOTH_CONNECT] ?: false
            val okScan    = perms[Manifest.permission.BLUETOOTH_SCAN]   ?: false
            val okFineLoc = perms[Manifest.permission.ACCESS_FINE_LOCATION]   ?: false
            val okCoarse  = perms[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            if (okConnect && okScan && (okFineLoc || okCoarse)) {
                // Una vez tengamos permisos, lanzamos el escaneo
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
                    val context = LocalContext.current
                    val activity = context as Activity
                    
                    // Back button handling
                    val backStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = backStackEntry?.destination?.route
                    var lastBackPressTime by remember { mutableLongStateOf(0L) }
                    
                    BackHandler {
                        when (currentRoute) {
                            "bluetooth" -> navController.popBackStack()
                            "control" -> {
                                val now = System.currentTimeMillis()
                                if (now - lastBackPressTime < 2000L) {
                                    activity.finish()
                                } else {
                                    lastBackPressTime = now
                                    Toast.makeText(
                                        context, 
                                        "Presione 2 veces para confirmar la salida", 
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            else -> navController.popBackStack()
                        }
                    }
                    
                    // Navegación simplificada - inicia directamente en control
                    NavHost(navController, startDestination = "control") {
                        composable("control") {
                            MotorControlScreen(
                                viewModel = viewModel,
                                onNavigateHome = {},
                                onNavigateSettings = { navController.navigate("bluetooth") }
                            )
                        }
                        composable("bluetooth") {
                            BluetoothControlScreen(
                                viewModel = viewModel,
                                onNavigateHome = { navController.navigate("control") },
                                onNavigateSettings = {}
                            )
                        }
                    }
                }
            }
        }
        
        // Pedimos permisos al iniciar la Activity
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
        // Para discovery en Android <12
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
