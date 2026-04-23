package com.example.proyecto.data.database.dao

import androidx.room.*
import com.example.proyecto.data.database.entity.JardineraEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JardineraDao {
    @Query("SELECT * FROM jardineras WHERE estaArchivada = 0")
    fun getJardinerasActivas(): Flow<List<JardineraEntity>>

    @Query("SELECT * FROM jardineras WHERE estaArchivada = 1")
    fun getJardinerasArchivadas(): Flow<List<JardineraEntity>>

    @Query("SELECT * FROM jardineras WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getJardineraByRemoteId(remoteId: String): JardineraEntity?

    @Query("SELECT * FROM jardineras WHERE id = :id")
    suspend fun getJardineraById(id: Long): JardineraEntity?

    @Query("UPDATE jardineras SET remoteId = :remoteId WHERE id = :localId")
    suspend fun updateRemoteId(localId: Long, remoteId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJardinera(jardinera: JardineraEntity): Long

    @Update
    suspend fun updateJardinera(jardinera: JardineraEntity)

    @Delete
    suspend fun deleteJardinera(jardinera: JardineraEntity)
}