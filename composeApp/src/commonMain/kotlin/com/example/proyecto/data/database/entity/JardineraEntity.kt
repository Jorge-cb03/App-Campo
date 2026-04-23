package com.example.proyecto.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "jardineras")
data class JardineraEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val filas: Int = 4,
    val columnas: Int = 2,
    val estaArchivada: Boolean = false,
    val esFavorita: Boolean = false,
    val remoteId: String? = null
)