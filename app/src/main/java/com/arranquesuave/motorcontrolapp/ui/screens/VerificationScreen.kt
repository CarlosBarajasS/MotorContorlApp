package com.arranquesuave.motorcontrolapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arranquesuave.motorcontrolapp.viewmodel.AuthViewModel
import androidx.compose.ui.tooling.preview.Preview
import com.arranquesuave.motorcontrolapp.ui.theme.MotorControlAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationScreen(
    email: String,
    viewModel: AuthViewModel = viewModel(),
    onVerified: () -> Unit
) {
    var code by remember { mutableStateOf("") }

    // Observa el resultado de la verificación
    val verifyResult by viewModel.verifyState.collectAsState()




    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Verificación de correo", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        InputField(
            value = code,
            onValueChange = { code = it },
            placeholder = "Código de verificación",
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { viewModel.verify(email, code) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Verificar")
        }
                LaunchedEffect(verifyResult) {
            verifyResult?.onSuccess {
                onVerified()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VerificationScreenPreview() {
    MotorControlAppTheme {
        VerificationScreen(email = "test@example.com", onVerified = {})
    }
}
