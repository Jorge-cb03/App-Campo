package com.example.proyecto.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.proyecto.data.database.dao.*
import com.example.proyecto.data.database.entity.*

@Database(
    entities = [
        JardineraEntity::class,
        BancalEntity::class,
        ProductoEntity::class,
        EntradaDiarioEntity::class,
        CercadoEntity::class,
        AlertaEntity::class,
        AnimalEntity::class,
        UsuarioEntity::class,
        EntradaDiarioAnimalEntity::class
    ],
    version = 18,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun jardineraDao(): JardineraDao
    abstract fun bancalDao(): BancalDao
    abstract fun productoDao(): ProductoDao
    abstract fun entradaDiarioDao(): EntradaDiarioDao
    abstract fun alertDao(): AlertDao
    abstract fun animalDao(): AnimalDao
    abstract fun usuarioDao(): UsuarioDao
    abstract fun diarioAnimalDao(): EntradaDiarioAnimalDao
}