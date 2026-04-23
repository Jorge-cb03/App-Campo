package com.example.proyecto.util

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.ByteArrayOutputStream

actual object MediaManager {
    @Composable
    actual fun rememberLauncher(onResult: (ByteArray?) -> Unit): MediaLauncher {
        val context = LocalContext.current

        val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            try {
                uri?.let {
                    // El uso de .use { ... } cierra el stream automáticamente al terminar
                    val bytes = context.contentResolver.openInputStream(it)?.use { stream ->
                        stream.readBytes()
                    }
                    onResult(bytes)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(null)
            }
        }

        val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            try {
                bitmap?.let {
                    val stream = ByteArrayOutputStream()
                    it.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                    onResult(stream.toByteArray())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(null)
            }
        }

        return remember {
            object : MediaLauncher {
                override fun launchCamera() { cameraLauncher.launch() }
                override fun launchGallery() { galleryLauncher.launch("image/*") }
            }
        }
    }
}