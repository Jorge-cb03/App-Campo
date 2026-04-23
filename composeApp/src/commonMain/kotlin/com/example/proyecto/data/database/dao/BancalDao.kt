package com.example.proyecto.data.database.dao

import androidx.room.*
import com.example.proyecto.data.database.entity.BancalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BancalDao {
    // Cambiamos la estrategia para no destruir la fila existente
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBancal(bancal: BancalEntity): Long

    @Update
    suspend fun updateBancal(bancal: BancalEntity)

    @Query("SELECT * FROM bancales WHERE jardineraId = :jardineraId")
    fun getBancalesByJardinera(jardineraId: Long): Flow<List<BancalEntity>>

    @Query("SELECT * FROM bancales WHERE id = :id")
    suspend fun getBancalById(id: Long): BancalEntity?

    @Query("SELECT * FROM bancales WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getBancalByRemoteId(remoteId: String): BancalEntity?

    @Query("UPDATE bancales SET remoteId = :remoteId WHERE id = :localId")
    suspend fun updateRemoteId(localId: Long, remoteId: String)

    @Query("DELETE FROM bancales WHERE jardineraId = :jardineraId AND (fila >= :maxFilas OR columna >= :maxCols)")
    suspend fun deleteBancalesFueraDeRango(jardineraId: Long, maxFilas: Int, maxCols: Int)

    @Query("SELECT COUNT(*) FROM entradas_diario WHERE bancalId = :bancalId AND tipoAccion = 'SIEMBRA'")
    suspend fun getCicloActual(bancalId: Long): Int
}