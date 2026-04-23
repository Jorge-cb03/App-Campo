// src/commonMain/kotlin/com/example/proyecto/ui/login/LoginScreen.kt
package com.example.proyecto.ui.login

import androidx.compose.runtime.Composable
import com.example.proyecto.data.repository.AuthRepository
import com.example.proyecto.ui.garden.GardenViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
expect fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onGoogleLoginClick: () -> Unit,
    onGuestLogin: () -> Unit,
    authRepository: AuthRepository = koinInject(), // Añadido
    viewModel: GardenViewModel = koinViewModel()   // Añadido
)