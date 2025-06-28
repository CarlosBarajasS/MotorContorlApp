// app/src/main/java/com/arranquesuave/motorcontrolapp/ui/theme/Theme.kt
package com.arranquesuave.motorcontrolapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1E88E5),
    onPrimary = Color.White,
    secondary = Color(0xFF43A047),
    onSecondary = Color.White,
    surfaceVariant = Color(0xFFE3F2FD),
    onSurfaceVariant = Color(0xFF000000)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color.Black,
    secondary = Color(0xFF80E27E),
    onSecondary = Color.Black,
    surfaceVariant = Color(0xFF263238),
    onSurfaceVariant = Color(0xFFFFFFFF)
)

@Composable
fun MotorControlAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable ()->Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        shapes = Shapes(),
        content = content
    )
}
