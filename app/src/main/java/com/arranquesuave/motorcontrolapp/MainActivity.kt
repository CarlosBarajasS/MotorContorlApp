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
import androidx.navigation.navArgument
import androidx.navigation.NavType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arranquesuave.motorcontrolapp.viewmodel.AuthViewModel
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import com.arranquesuave.motorcontrolapp.ui.screens.VerificationScreen
import androidx.compose.runtime.collectAsState
import androidx.navigation.compose.composable
import com.arranquesuave.motorcontrolapp.ui.screens.LoginScreen
import com.arranquesuave.motorcontrolapp.ui.screens.SignUpScreen
import com.arranquesuave.motorcontrolapp.ui.screens.BluetoothControlScreen
import com.arranquesuave.motorcontrolapp.ui.screens.MotorControlScreen
import com.arranquesuave.motorcontrolapp.viewmodel.MotorViewModel
import com.arranquesuave.motorcontrolapp.utils.SessionManager
import com.arranquesuave.motorcontrolapp.BuildConfig
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.currentBackStackEntryAsState
import android.app.Activity
import android.widget.Toast

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
        val sessionManager = SessionManager(this)
        val startDestination = if (BuildConfig.NO_AUTH) "control" else if (sessionManager.getToken() != null) "control" else "login"
        setContent {
            MotorControlAppTheme {
                Surface {
                    // 2) Configuración de autenticación y navegación usando AuthViewModel
        
                            val authViewModel: AuthViewModel = viewModel()
                            var signupEmail by remember { mutableStateOf("") }
val loginResult by authViewModel.loginState.collectAsState(initial = authViewModel.loginState.value)
val signupResult by authViewModel.signupState.collectAsState(initial = authViewModel.signupState.value)
        
                    val navController = rememberNavController()
                    // Back button handling
                    val backStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = backStackEntry?.destination?.route
                    val context = LocalContext.current
                    val activity = context as Activity
                    var lastBackPressTime by remember { mutableStateOf(0L) }
                    BackHandler {
                        when (currentRoute) {
                            "bluetooth" -> navController.popBackStack()
                            "control", "login" -> {
                                val now = System.currentTimeMillis()
                                if (now - lastBackPressTime < 2000L) {
                                    activity.finish()
                                } else {
                                    lastBackPressTime = now
                                    Toast.makeText(context, "Presione 2 veces para confirmar la salida", Toast.LENGTH_SHORT).show()
                                }
                            }
                            "signup" -> navController.navigate("login")
                            else -> navController.popBackStack()
                        }
                    }
                    NavHost(navController, startDestination = startDestination) {
                        composable("login") {
                                LoginScreen(onLogin = { email, password ->
                                    authViewModel.login(email, password)
                                }, onNavigateToSignUp = {
                                    navController.navigate("signup")
                                })
                                LaunchedEffect(loginResult) {
                                loginResult?.onSuccess { response -> sessionManager.saveToken(response.token)
                                navController.navigate("control") {
                                popUpTo("login") { inclusive = true }
                                }
                                    authViewModel.loginState.value = null
    }
}
                            
                        }
                        composable("signup") {
                                SignUpScreen(onSignUp = { email, password, confirm ->
                                    signupEmail = email
                                    authViewModel.signup(email, password, confirm)
                                }, onNavigateToLogin = {
                                    navController.popBackStack()
                                })
                                LaunchedEffect(signupResult) {
                                    signupResult?.onSuccess {
                                        navController.navigate("verify?email=$signupEmail")
                                    }
                                }
                            
                        }
                        composable("verify?email={email}",
                                    arguments = listOf(navArgument("email") { type = NavType.StringType })
                                ) { backStackEntry ->
                                    val emailArg = backStackEntry.arguments?.getString("email") ?: ""
                                    VerificationScreen(email = emailArg, onVerified = {
                                        navController.navigate("control") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    })
                                }
                                composable("control") {
                            MotorControlScreen(
    viewModel = viewModel,
    onLogout = { authViewModel.logout() },
    onNavigateHome = {},
    onNavigateSettings = { navController.navigate("bluetooth") }
)
val logoutResult by authViewModel.logoutState.collectAsState(initial = authViewModel.logoutState.value)
LaunchedEffect(logoutResult) {
    logoutResult?.onSuccess { sessionManager.clearToken()
        authViewModel.loginState.value = null
        authViewModel.logoutState.value = null
        navController.navigate("login") {
            popUpTo("control") { inclusive = true }
        }
    }
}
                        }
                        composable("bluetooth") {
                            BluetoothControlScreen(
    viewModel = viewModel,
    onLogout = { authViewModel.logout() },
    onNavigateHome = { navController.navigate("control") },
    onNavigateSettings = {}
)
                                
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
