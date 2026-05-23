package com.example.proyecto

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.proyecto.di.appModule
import com.example.proyecto.ui.navigation.AppNavigation 
import com.example.proyecto.ui.theme.ProyectoTheme
import org.koin.compose.KoinApplication

@Composable
fun App() {
    // Inicializamos Koin para la inyección de dependencias
    KoinApplication(application = {
        modules(appModule)
    }) {
        // 1. Detectamos el tema del sistema (oscuro o claro)
        val systemDark = isSystemInDarkTheme()

        // 2. Creamos un estado para poder cambiar el tema manualmente si queremos
        var isDarkTheme by rememberSaveable { mutableStateOf(false) }

        ProyectoTheme(darkTheme = isDarkTheme) {
            AppNavigation(
                isDarkTheme = isDarkTheme,
                onToggleTheme = { newTheme: Boolean -> isDarkTheme = newTheme } // Especificamos el tipo Boolean
            )
        }
    }
}