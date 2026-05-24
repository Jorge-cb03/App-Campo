package com.example.proyecto.data.database.dao

import androidx.room.*
import com.example.proyecto.data.database.entity.AnimalEntity
import com.example.proyecto.data.database.entity.CercadoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimalDao {
    @Query("SELECT * FROM cercados ORDER BY numero ASC")
    fun getAllCercados(): Flow<List<CercadoEntity>>

    @Query("SELECT * FROM animales")
    fun getAllAnimales(): Flow<List<AnimalEntity>>

    @Update
    suspend fun updateAnimal(animal: AnimalEntity)

    @Delete
    suspend fun deleteAnimal(animal: AnimalEntity)

    @Update
    suspend fun updateCercado(cercado: CercadoEntity)
    @Query("SELECT * FROM cercados WHERE sincronizado = 0")
    suspend fun getCercadosNoSincronizados(): List<CercadoEntity>

    @Query("SELECT * FROM animales WHERE sincronizado = 0")
    suspend fun getAnimalesNoSincronizados(): List<AnimalEntity>

    @Query("UPDATE cercados SET remoteId = :remoteId WHERE id = :id")
    suspend fun updateCercadoRemoteId(id: Long, remoteId: String)

    @Query("UPDATE animales SET remoteId = :remoteId WHERE id = :id")
    suspend fun updateAnimalRemoteId(id: Long, remoteId: String)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCercado(cercado: CercadoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnimal(animal: AnimalEntity): Long
    @Delete
    suspend fun deleteCercado(cercado: CercadoEntity)

    // 2. Mover todos los animales de un cercado a otro (una sola query)
    @Query("UPDATE animales SET cercadoId = :destinoId WHERE cercadoId = :origenId")
    suspend fun moverAnimalesACercado(origenId: Long, destinoId: Long)
}