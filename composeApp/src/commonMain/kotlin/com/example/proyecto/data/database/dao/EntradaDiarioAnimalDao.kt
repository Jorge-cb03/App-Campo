package com.example.proyecto.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.proyecto.data.database.entity.EntradaDiarioAnimalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EntradaDiarioAnimalDao {

    @Query("SELECT * FROM diario_animales ORDER BY fecha DESC")
    fun getAllLogs(): Flow<List<EntradaDiarioAnimalEntity>>

    @Query("SELECT * FROM diario_animales WHERE cercadoId = :cercadoId ORDER BY fecha DESC")
    fun getLogsByCercado(cercadoId: Long): Flow<List<EntradaDiarioAnimalEntity>>

    @Query("DELETE FROM diario_animales WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM diario_animales WHERE id = :id")
    suspend fun getById(id: Long): EntradaDiarioAnimalEntity?

    @Update
    suspend fun update(entrada: EntradaDiarioAnimalEntity)
    @Query("SELECT * FROM diario_animales WHERE sincronizado = 0")
    suspend fun getLogsNoSincronizados(): List<EntradaDiarioAnimalEntity>

    @Query("UPDATE diario_animales SET remoteId = :remoteId WHERE id = :id")
    suspend fun updateRemoteId(id: Long, remoteId: String)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entrada: EntradaDiarioAnimalEntity): Long
}