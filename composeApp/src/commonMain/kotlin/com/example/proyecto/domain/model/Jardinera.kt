package com.example.proyecto.domain.model

enum class EstadoJardinera {
    VACIO,      // No hay nada plantado
    OCUPADO,    // Hay un cultivo creciendo
    ENFERMO     // Hay plaga (se mostrará en rojo o con alerta)
}

data class Jardinera(
    val id: String,
    val nombre: String,     // Ej: "Jardinera A1"
    val cultivo: String?,   // Ej: "Tomates", puede ser null si está vacía
    val estado: EstadoJardinera,
    val icon: String = "🌱" // Usaremos emojis por ahora para ver qué es cada cosa rápido
)