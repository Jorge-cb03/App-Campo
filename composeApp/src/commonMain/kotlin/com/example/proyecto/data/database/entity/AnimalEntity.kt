package com.example.proyecto.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "animales")
data class AnimalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val tipo: String,
    val cercadoId: Long,
    val raza: String?,
    val fechaNacimiento: Long?,
    val fotoPerfil: ByteArray?,
    val esPonedora: Boolean = false,
    val notas: String? = null,
    val compatibilidad: String? = null
)