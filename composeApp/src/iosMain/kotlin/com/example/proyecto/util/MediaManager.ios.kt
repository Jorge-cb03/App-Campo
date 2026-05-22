package com.example.proyecto.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.darwin.NSObject

actual class MediaManager(
    private val viewController: UIViewController,
    private val onMediaResult: (ByteArray?) -> Unit
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    private val picker = UIImagePickerController()

    init {
        picker.delegate = this
    }

    actual fun launchGallery() {
        picker.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
        viewController.presentViewController(picker, true, null)
    }

    actual fun launchCamera() {
        picker.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
        viewController.presentViewController(picker, true, null)
    }

    // Aquí irían las funciones de delegado para procesar la imagen seleccionada...
}

@Composable
actual fun rememberMediaManager(onMediaResult: (ByteArray?) -> Unit): MediaManager {
    // En iOS necesitamos el ViewController actual de Compose
    // Esto requiere una integración con el MainViewController.
    return remember { MediaManager(UIViewController(), onMediaResult) }
}