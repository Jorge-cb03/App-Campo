package com.example.proyecto.util

object TranslationUtils {

    fun traducirRiego(watering: String?): String {
        return when (watering?.lowercase()?.trim()) {
            "frequent" -> "Frecuente (1-2 días)"
            "average" -> "Moderado (3-4 días)"
            "minimum" -> "Escaso (Semanal)"
            "none" -> "Solo lluvia"
            else -> "Moderado"
        }
    }

    fun traducirLuz(sunlight: List<String>?): String {
        if (sunlight.isNullOrEmpty()) return "Desconocido"
        // Lógica de prioridad: si acepta sombra, lo destacamos
        val text = sunlight.joinToString(", ").lowercase()
        return when {
            "full_shade" in text -> "Sombra Total"
            "part_shade" in text || "part_sun" in text -> "Sombra Parcial"
            "full_sun" in text -> "Sol Directo"
            else -> "Sol y Sombra"
        }
    }
}