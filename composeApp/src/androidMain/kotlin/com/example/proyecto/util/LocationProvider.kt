package com.example.proyecto.util

import android.annotation.SuppressLint
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await
import com.example.proyecto.data.database.appContext // <-- Este import es vital

// Aquí va la implementación real de Android (actual)
actual class LocationProvider actual constructor() {

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(appContext)
    }

    @SuppressLint("MissingPermission")
    actual suspend fun getCurrentLocation(): Pair<Double, Double>? {
        return try {
            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                null
            ).await()

            if (location != null) {
                Pair(location.latitude, location.longitude)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}