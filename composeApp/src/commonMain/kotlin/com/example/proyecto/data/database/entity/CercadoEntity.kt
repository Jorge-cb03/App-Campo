package com.example.proyecto.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cercados")
data class CercadoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val numero: Int,
    val nombre: String
)