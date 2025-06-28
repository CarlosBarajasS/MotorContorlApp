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
import com.arranquesuave.motorcontrolapp.ui.screens.LoginScreen
import com.arranquesuave.motorcontrolapp.ui.screens.SignUpScreen
import com.arranquesuave.motorcontrolapp.ui.screens.BluetoothControlScreen
import com.arranquesuave.motorcontrolapp.ui.screens.MotorControlScreen
import com.arranquesuave.motorcontrolapp.viewmodel.MotorViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MotorViewModel by viewModels()

    // 1) Preparamos la petición múltiple de permisos
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
            } else {
                // Aquí podrías mostrar un Toast indicando que faltan permisos
            }
        }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MotorControlAppTheme {
                Surface {
                    // 2) Pasamos el viewModel; la pantalla llamará a startDiscovery()
                    val navController = rememberNavController()
                    NavHost(navController, startDestination = "login") {
                        composable("login") {
                            LoginScreen(onLogin = { email, password ->
                                // TODO: Autenticación
                                navController.navigate("control")
                            }, onNavigateToSignUp = {
                                navController.navigate("signup")
                            })
                        }
                        composable("signup") {
                            SignUpScreen(onSignUp = { email, password, confirm ->
                                // TODO: Registro de usuario
                                navController.popBackStack("login", false)
                            }, onNavigateToLogin = {
                                navController.popBackStack()
                            })
                        }
                        composable("control") {
                            MotorControlScreen(viewModel)
                        }
                        composable("bluetooth") {
                            BluetoothControlScreen(viewModel, onBack = {
                                navController.popBackStack()
                            })
                        }
                    }
                }
            }
        }
        // 3) Pedimos permisos al iniciar la Activity
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
