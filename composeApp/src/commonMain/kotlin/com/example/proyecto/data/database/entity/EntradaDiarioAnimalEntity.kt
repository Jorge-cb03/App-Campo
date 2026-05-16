package com.example.proyecto.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diario_animales")
data class EntradaDiarioAnimalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cercadoId: Long,
    val animalTipo: String,
    val tipoAccion: String,
    val descripcion: String,
    val cantidad: Double,
    val fecha: Long = System.currentTimeMillis()
)