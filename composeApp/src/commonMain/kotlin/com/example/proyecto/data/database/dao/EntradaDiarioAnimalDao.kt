package com.example.proyecto.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.proyecto.data.database.entity.EntradaDiarioAnimalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EntradaDiarioAnimalDao {
    @Insert
    suspend fun insert(entrada: EntradaDiarioAnimalEntity)

    @Query("SELECT * FROM diario_animales ORDER BY fecha DESC")
    fun getAllLogs(): Flow<List<EntradaDiarioAnimalEntity>>

    @Query("SELECT * FROM diario_animales WHERE cercadoId = :cercadoId ORDER BY fecha DESC")
    fun getLogsByCercado(cercadoId: Long): Flow<List<EntradaDiarioAnimalEntity>>

    @Query("DELETE FROM diario_animales WHERE id = :id")
    suspend fun deleteById(id: Long)
}