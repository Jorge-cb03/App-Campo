package com.example.proyecto.data.repository

import com.example.proyecto.data.database.AppDatabase
import com.example.proyecto.data.database.entity.AnimalEntity
import com.example.proyecto.data.database.entity.CercadoEntity
import com.example.proyecto.data.database.entity.EntradaDiarioAnimalEntity
import kotlinx.coroutines.flow.Flow

data class FichaAnimal(
    val id: Int, val nombre: String, val nombreCientifico: String,
    val imagenUrl: String, val esPonedora: Boolean,
    val alimentacion: String, val compatibilidad: String, val consejo: String
)

class AnimalRepository(private val db: AppDatabase) {
    private val animalDao = db.animalDao()
    private val diarioAnimalDao = db.diarioAnimalDao()

    private val catalogoAnimales = listOf(
        FichaAnimal(1, "Gallina", "Gallus gallus", "https://images.unsplash.com/photo-1548550023-2bdb3c5beed7?q=80&w=500", true, "Granos", "Aves", "Lugar seco."),
        FichaAnimal(2, "Oca", "Anser anser", "https://images.unsplash.com/photo-1555610817-21443657374b?q=80&w=500", true, "Hierba", "Gallinas", "Son guardianas."),
        FichaAnimal(3, "Perdiz", "Alectoris rufa", "https://images.unsplash.com/photo-1628172151664-98442e2025df?q=80&w=500", true, "Semillas", "Aves pequeñas", "Necesitan limpieza."),
        FichaAnimal(4, "Perro Pastor", "Canis lupus", "https://images.unsplash.com/photo-1583511655857-d19b40a7a54e?q=80&w=500", false, "Pienso", "Ganado", "Entrenamiento vital."),
        FichaAnimal(5, "Pato", "Anas platyrhynchos", "https://images.unsplash.com/photo-1454561021425-4528f41121d5?q=80&w=500", true, "Plantas acuáticas", "Aves", "Necesitan agua.")
    )

    fun getCatalogo() = catalogoAnimales
    fun getFichaPorNombre(nombre: String) = catalogoAnimales.find { it.nombre.equals(nombre, ignoreCase = true) }

    fun getAllAnimales() = animalDao.getAllAnimales()
    fun getAllCercados() = animalDao.getAllCercados()

    suspend fun insertAnimal(animal: AnimalEntity) = animalDao.insertAnimal(animal)
    suspend fun updateAnimal(animal: AnimalEntity) = animalDao.updateAnimal(animal)
    suspend fun deleteAnimal(animal: AnimalEntity) = animalDao.deleteAnimal(animal)

    suspend fun insertCercado(cercado: CercadoEntity) = animalDao.insertCercado(cercado)

    // --- NUEVAS FUNCIONES DEL DIARIO (ESTO FALTABA) ---
    suspend fun insertarDiarioAnimal(entrada: EntradaDiarioAnimalEntity) = diarioAnimalDao.insert(entrada)
    fun getDiarioAnimales(): Flow<List<EntradaDiarioAnimalEntity>> = diarioAnimalDao.getAllLogs()
    suspend fun eliminarDiarioAnimal(id: Long) = diarioAnimalDao.deleteById(id)
}