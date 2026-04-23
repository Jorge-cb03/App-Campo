package com.example.proyecto.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "bancales",
    foreignKeys = [ForeignKey(entity = JardineraEntity::class, parentColumns = ["id"], childColumns = ["jardineraId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("jardineraId"), Index("remoteId")] // AÑADIDO: Índice para remoteId
)
data class BancalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val jardineraId: Long,
    val fila: Int,
    val columna: Int,
    val perenualId: Int? = null,
    val nombreCultivo: String? = null,
    val imagenUrl: String? = null,
    val frecuenciaRiegoDias: Int? = null,
    val necesidadSol: String? = null,
    val fechaSiembra: Long? = null,
    val esFuncional: Boolean = true,
    val remoteId: String? = null
)