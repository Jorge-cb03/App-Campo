package com.example.proyecto.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.proyecto.data.database.entity.UsuarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {
    @Query("SELECT * FROM usuario WHERE id = 1 LIMIT 1")
    fun getUsuario(): Flow<UsuarioEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUsuario(usuario: UsuarioEntity)

    // NUEVO: Método para eliminar el rastro del usuario al cerrar sesión
    @Query("DELETE FROM usuario")
    suspend fun borrarTodo()
}