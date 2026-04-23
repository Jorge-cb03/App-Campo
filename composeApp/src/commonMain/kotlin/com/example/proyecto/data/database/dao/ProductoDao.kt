package com.example.proyecto.data.database.dao

import androidx.room.*
import com.example.proyecto.data.database.entity.ProductoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducto(producto: ProductoEntity): Long

    @Update
    suspend fun updateProducto(producto: ProductoEntity)

    @Query("SELECT * FROM productos ORDER BY nombre ASC")
    fun getAllProductos(): Flow<List<ProductoEntity>>

    @Query("SELECT * FROM productos WHERE id = :id")
    suspend fun getProductoById(id: Long): ProductoEntity?

    @Query("DELETE FROM productos WHERE id = :id")
    suspend fun deleteProductoById(id: Long)

    // CAMBIO: Buscar por Slug (String)
    @Query("SELECT * FROM productos WHERE perenualId = :id LIMIT 1")
    suspend fun getProductoByPerenualId(id: Int): ProductoEntity?

    @Query("UPDATE productos SET remoteId = :remoteId WHERE id = :localId")
    suspend fun updateRemoteId(localId: Long, remoteId: String)

    // Útil para la sincronización masiva
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(productos: List<ProductoEntity>)

    // Añade esto en ProductoDao.kt para buscar duplicados por ID de Firebase
    @Query("SELECT * FROM productos WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getProductoByRemoteId(remoteId: String): ProductoEntity?
}