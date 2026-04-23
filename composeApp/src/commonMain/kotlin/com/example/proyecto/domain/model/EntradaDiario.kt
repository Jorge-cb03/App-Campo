package com.example.proyecto.domain.model

import kotlinx.datetime.LocalDate

data class EntradaDiario(
    val id: String,
    val fecha: LocalDate, // La fecha de la nota
    val titulo: String,   // Ej: "Sembrar"
    val descripcion: String, // Ej: "Sembrar manzanas"
    val jardineraNombre: String // Para saber en qué jardinera fue (ej: "jardinera 1")
)