package com.example.proyecto.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diario_animales")
data class EntradaDiarioAnimalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cercadoId: Long,
    val animalTipo: String = "",
    val tipoAccion: String,
    val descripcion: String,
    val cantidad: Double,
    val fecha: Long = System.currentTimeMillis(),
    val usuarioId: String = "",
    val remoteId: String? = null,
    val sincronizado: Boolean = false,
    val foto: ByteArray? = null
) {
    // equals y hashCode necesarios porque ByteArray no los implementa bien por defecto
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EntradaDiarioAnimalEntity) return false
        return id == other.id &&
                cercadoId == other.cercadoId &&
                animalTipo == other.animalTipo &&
                tipoAccion == other.tipoAccion &&
                descripcion == other.descripcion &&
                cantidad == other.cantidad &&
                fecha == other.fecha &&
                usuarioId == other.usuarioId &&
                remoteId == other.remoteId &&
                sincronizado == other.sincronizado &&
                (foto == null && other.foto == null || foto != null && other.foto != null && foto.contentEquals(other.foto))
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + cercadoId.hashCode()
        result = 31 * result + animalTipo.hashCode()
        result = 31 * result + tipoAccion.hashCode()
        result = 31 * result + descripcion.hashCode()
        result = 31 * result + cantidad.hashCode()
        result = 31 * result + fecha.hashCode()
        result = 31 * result + usuarioId.hashCode()
        result = 31 * result + (remoteId?.hashCode() ?: 0)
        result = 31 * result + sincronizado.hashCode()
        result = 31 * result + (foto?.contentHashCode() ?: 0)
        return result
    }
}