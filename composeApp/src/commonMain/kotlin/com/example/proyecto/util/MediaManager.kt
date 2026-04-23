package com.example.proyecto.util

import androidx.compose.runtime.Composable

expect object MediaManager {
    @Composable
    fun rememberLauncher(onResult: (ByteArray?) -> Unit): MediaLauncher
}

interface MediaLauncher {
    fun launchCamera()
    fun launchGallery()
}