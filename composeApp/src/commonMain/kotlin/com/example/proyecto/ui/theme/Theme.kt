package com.example.proyecto.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = MinimalAccent,
    onPrimary = LightSurface,
    background = LightBackground,
    surface = LightSurface,
    onBackground = LightText,
    onSurface = LightText,
    surfaceVariant = LightBackground,
    onSurfaceVariant = LightTextSecondary,
    outlineVariant = LightOutline,
    error = RedDanger
)

private val DarkColorScheme = darkColorScheme(
    primary = MinimalAccent,
    onPrimary = LightSurface,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = DarkText,
    onSurface = DarkText,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = DarkTextSecondary,
    outlineVariant = DarkOutline,
    error = RedDanger
)

@Composable
fun ProyectoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}