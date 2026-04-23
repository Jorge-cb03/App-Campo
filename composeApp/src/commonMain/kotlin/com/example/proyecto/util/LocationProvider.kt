package com.example.proyecto.util

expect class LocationProvider() {
    suspend fun getCurrentLocation(): Pair<Double, Double>?
}