package com.example.proyecto.data.database.dao

import androidx.room.*
import com.example.proyecto.data.database.entity.AnimalEntity
import com.example.proyecto.data.database.entity.CercadoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimalDao {
    @Query("SELECT * FROM cercados ORDER BY numero ASC")
    fun getAllCercados(): Flow<List<CercadoEntity>>

    @Insert
    suspend fun insertCercado(cercado: CercadoEntity)

    @Query("SELECT * FROM animales")
    fun getAllAnimales(): Flow<List<AnimalEntity>>

    @Insert
    suspend fun insertAnimal(animal: AnimalEntity)

    @Update
    suspend fun updateAnimal(animal: AnimalEntity)

    @Delete
    suspend fun deleteAnimal(animal: AnimalEntity)
}