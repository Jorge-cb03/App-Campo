package com.example.proyecto.data.database

import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
object DatabaseProvider {
    private var instance: AppDatabase? = null

    fun getDatabase(): AppDatabase {
        return instance ?: getRoomDatabase(getDatabaseBuilder()).also { instance = it }
    }
}