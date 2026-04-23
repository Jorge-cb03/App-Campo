package com.example.proyecto.data.database.dao

import androidx.room.*
import com.example.proyecto.data.database.entity.AlertaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {
    @Query("SELECT * FROM alertas ORDER BY dateTimeEpochMillis ASC")
    fun getAllAlerts(): Flow<List<AlertaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertaEntity): Long

    @Update
    suspend fun updateAlert(alert: AlertaEntity)

    @Delete
    suspend fun deleteAlert(alert: AlertaEntity)

    // --- MÉTODOS PARA SINCRONIZACIÓN ---

    @Query("SELECT * FROM alertas WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getAlertByRemoteId(remoteId: String): AlertaEntity?

    @Query("UPDATE alertas SET remoteId = :remoteId WHERE id = :localId")
    suspend fun updateRemoteId(localId: Long, remoteId: String)

    @Query("UPDATE alertas SET isSynced = :status WHERE id = :localId")
    suspend fun updateSyncStatus(localId: Long, status: Boolean)

    @Query("SELECT * FROM alertas WHERE isSynced = 0")
    suspend fun getUnsyncedAlerts(): List<AlertaEntity>
}