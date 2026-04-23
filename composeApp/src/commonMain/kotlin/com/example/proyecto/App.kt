package com.example.proyecto

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import com.example.proyecto.di.appModule
import com.example.proyecto.ui.navigation.AppNavigation
import com.example.proyecto.ui.theme.AppTheme
import org.koin.compose.KoinApplication

@Composable
fun App() {
    KoinApplication(application = {
        modules(appModule)
    }) {
        // Detectamos el tema del sistema al inicio
        val systemDark = isSystemInDarkTheme()

        // FIX: Cambiado de 'val' a 'var' para que pueda modificarse
        var isDarkTheme by remember { mutableStateOf(systemDark) }

        AppTheme(darkTheme = isDarkTheme) {
            // Pasamos el estado y la función para modificarlo
            AppNavigation(
                isDarkTheme = isDarkTheme,
                onToggleTheme = { newTheme -> isDarkTheme = newTheme } // FIX: Lógica de cambio activada
            )
        }
    }
}