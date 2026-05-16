package com.example.proyecto.util

// Aquí SOLO va la definición (expect), nada de código de Android.
expect class LocationProvider() {
    suspend fun getCurrentLocation(): Pair<Double, Double>?
}