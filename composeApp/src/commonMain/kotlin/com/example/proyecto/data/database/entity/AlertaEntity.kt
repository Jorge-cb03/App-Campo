package com.example.proyecto.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "alertas")
@Serializable
data class AlertaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val dateTimeEpochMillis: Long,
    val isUrgent: Boolean = false,

    // --- CAMPOS PARA SINCRONIZACIÓN ---
    // ID único global (String) para que Firebase/Supabase no duplique registros
    val remoteId: String? = null,

    // Flag para saber si está pendiente de subir a la nube
    val isSynced: Boolean = false,

    // Timestamp de última modificación para resolver conflictos
    val lastModified: Long = 0
)