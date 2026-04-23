package com.example.proyecto.util

actual class LocationProvider actual constructor() {
    actual suspend fun getCurrentLocation(): Pair<Double, Double>? {
        // Implementación real pendiente para iOS
        // Por ahora devolvemos null o una ubicación fija (ej. Madrid)
        return Pair(40.4168, -3.7038)
    }
}