package com.arranquesuave.motorcontrolapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.arranquesuave.motorcontrolapp.R

@Composable
fun InputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false
) {
    var passwordVisible by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .background(color = Color(0xFFE5F2EC), shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                        color = Color.Gray
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(color = Color.Black, fontSize = 16.sp),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (isPassword) {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = Color.Gray
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLogin: (email: String, password: String) -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.it_morelia_logo_sinfondo),
            contentDescription = "IT Morelia Logo",
            modifier = Modifier
                .size(128.dp)
                .align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Motor Control",
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Bienvenido",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(24.dp))

        InputField(
            value = email,
            onValueChange = { email = it },
            placeholder = "Correo",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        InputField(
            value = password,
            onValueChange = { password = it },
            placeholder = "Contraseña",
            modifier = Modifier.fillMaxWidth(),
            isPassword = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onLogin(email, password) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF29D07F))
        ) {
            Text(text = "Iniciar Sesión", color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "¿No tienes una cuenta? Regístrate",
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable { onNavigateToSignUp() },
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Image(
            painter = painterResource(R.drawable.dept_logo),
            contentDescription = "Department Logo",
            modifier = Modifier
                .size(64.dp)
                .align(Alignment.CenterHorizontally)
        )
    }
}
