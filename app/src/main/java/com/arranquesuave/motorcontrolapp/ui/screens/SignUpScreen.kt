// app/src/main/java/com/arranquesuave/motorcontrolapp/ui/screens/SignUpScreen.kt
package com.arranquesuave.motorcontrolapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onSignUp: (email: String, password: String, confirm: String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var email   by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm  by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Crear Cuenta", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(24.dp))

        InputField(
            value = email,
            onValueChange = { email = it },
            placeholder = "Correo",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        InputField(
            value = password,
            onValueChange = { password = it },
            placeholder = "Contraseña",
            modifier = Modifier.fillMaxWidth(),
            isPassword = true
        )

        Spacer(Modifier.height(16.dp))

        InputField(
            value = confirm,
            onValueChange = { confirm = it },
            placeholder = "Confirmar Contraseña",
            modifier = Modifier.fillMaxWidth(),
            isPassword = true
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { onSignUp(email, password, confirm) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF29D07F))
        ) {
            Text("Registrarse", color = Color.White)
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "¿Ya tienes una cuenta? Iniciar Sesión",
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable { onNavigateToLogin() },
            color = MaterialTheme.colorScheme.primary
        )
    }
}
