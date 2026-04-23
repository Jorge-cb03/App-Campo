package com.example.proyecto.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkScheme = darkColorScheme(
    primary = GreenPrimary,
    background = DarkBg,
    surface = DarkSurface,
    onBackground = DarkTextMain,
    onSurface = DarkTextMain,
    secondary = DarkTextSec, // Usamos secondary para texto gris
    error = RedDanger,
    surfaceVariant = DarkInput
)

private val LightScheme = lightColorScheme(
    primary = GreenPrimary,
    background = LightBg,
    surface = LightSurface,
    onBackground = LightTextMain,
    onSurface = LightTextMain,
    secondary = LightTextSec,
    error = RedDanger,
    surfaceVariant = LightInput
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkScheme else LightScheme

    SystemAppearance(isDark = darkTheme)

    MaterialTheme(
        colorScheme = colors,
        typography = Typography(), // Usa la por defecto por ahora
        content = content
    )
}