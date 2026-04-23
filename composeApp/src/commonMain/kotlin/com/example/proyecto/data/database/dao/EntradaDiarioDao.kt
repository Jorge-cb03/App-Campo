package com.example.proyecto.data.database.dao

import androidx.room.*
import com.example.proyecto.data.database.entity.EntradaDiarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EntradaDiarioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntrada(entrada: EntradaDiarioEntity): Long

    @Query("SELECT * FROM entradas_diario WHERE bancalId = :bancalId ORDER BY fecha DESC")
    fun getDiarioByBancal(bancalId: Long): Flow<List<EntradaDiarioEntity>>

    @Query("SELECT * FROM entradas_diario ORDER BY fecha DESC")
    fun getAllEntradas(): Flow<List<EntradaDiarioEntity>>

    @Query("SELECT * FROM entradas_diario WHERE id = :id")
    suspend fun getEntradaById(id: Long): EntradaDiarioEntity?

    @Query("DELETE FROM entradas_diario WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Verifica si ya existe una acción idéntica en un margen de 1 minuto.
     * Esto evita que al guardar manualmente varias tareas el mismo día (que comparten fecha 00:00)
     * o al pulsar varias veces el botón, se bloqueen o se dupliquen erróneamente.
     */
    @Query("""
        SELECT * FROM entradas_diario 
        WHERE bancalId = :bancalId 
        AND tipoAccion = :tipo 
        AND fecha BETWEEN :fecha - 60000 AND :fecha + 60000 
        LIMIT 1
    """)
    suspend fun getEntradaExistente(bancalId: Long, tipo: String, fecha: Long): EntradaDiarioEntity?

    @Query("UPDATE entradas_diario SET descripcion = :desc WHERE id = :id")
    suspend fun updateDescripcion(id: Long, desc: String)
}