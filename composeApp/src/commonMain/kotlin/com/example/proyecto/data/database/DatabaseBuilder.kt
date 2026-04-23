package com.example.proyecto.data.database

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

// Definimos que cada plataforma debe proveer su propio builder
expect fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>

// Función común para construir la base de datos una vez obtenido el builder
fun getRoomDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase {
    return builder
        .setDriver(BundledSQLiteDriver()) // Necesario para KMP
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}