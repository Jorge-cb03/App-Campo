package com.example.proyecto.data.repository

import com.example.proyecto.data.database.AppDatabase
import com.example.proyecto.data.database.entity.AnimalEntity
import com.example.proyecto.data.database.entity.CercadoEntity
import com.example.proyecto.data.database.entity.EntradaDiarioAnimalEntity
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow

data class FichaAnimal(
    val id: Int, val nombre: String, val nombreCientifico: String,
    val imagenUrl: String, val esPonedora: Boolean,
    val alimentacion: String, val compatibilidad: String, val consejo: String
)

class AnimalRepository(
    private val db: AppDatabase,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
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

    // --- HELPER MULTI-TENANT (Misma lógica que la Huerta) ---
    private fun getUserCollection(collectionName: String): dev.gitlive.firebase.firestore.CollectionReference? {
        val currentUser = auth.currentUser
        return if (currentUser != null) {
            firestore.collection("usuarios").document(currentUser.uid).collection(collectionName)
        } else null
    }

    // ================= LECTURAS LOCALES (Rápidas) =================
    fun getAllAnimales() = animalDao.getAllAnimales()
    fun getAllCercados() = animalDao.getAllCercados()
    fun getDiarioAnimales(): Flow<List<EntradaDiarioAnimalEntity>> = diarioAnimalDao.getAllLogs()

    // ================= GESTIÓN DE CERCADOS =================
    suspend fun insertCercado(cercado: CercadoEntity) {
        val localId = animalDao.insertCercado(cercado)
        try {
            getUserCollection("cercados")?.let { col ->
                val docRef = col.add(cercado.copy(id = localId))
                animalDao.updateCercadoRemoteId(localId, docRef.id)
            }
        } catch (e: Exception) { println("Error Firebase Cercado: ${e.message}") }
    }

    suspend fun updateCercado(cercado: CercadoEntity) {
        animalDao.updateCercado(cercado)
        try {
            cercado.remoteId?.let { rId -> getUserCollection("cercados")?.document(rId)?.set(cercado) }
        } catch (e: Exception) { println("Error Firebase Cercado Update: ${e.message}") }
    }

    // ================= GESTIÓN DE ANIMALES =================
    suspend fun insertAnimal(animal: AnimalEntity) {
        val localId = animalDao.insertAnimal(animal)
        try {
            getUserCollection("animales")?.let { col ->
                val docRef = col.add(animal.copy(id = localId))
                animalDao.updateAnimalRemoteId(localId, docRef.id)
            }
        } catch (e: Exception) { println("Error Firebase Animal: ${e.message}") }
    }

    suspend fun updateAnimal(animal: AnimalEntity) {
        animalDao.updateAnimal(animal)
        try {
            animal.remoteId?.let { rId -> getUserCollection("animales")?.document(rId)?.set(animal) }
        } catch (e: Exception) { println("Error Firebase Animal Update: ${e.message}") }
    }

    suspend fun deleteAnimal(animal: AnimalEntity) {
        animalDao.deleteAnimal(animal)
        try {
            animal.remoteId?.let { rId -> getUserCollection("animales")?.document(rId)?.delete() }
        } catch (e: Exception) { println("Error Firebase Animal Delete: ${e.message}") }
    }

    // ================= GESTIÓN DEL DIARIO (GRANJA) =================
    suspend fun insertarDiarioAnimal(entrada: EntradaDiarioAnimalEntity) {
        val localId = diarioAnimalDao.insert(entrada)
        try {
            getUserCollection("diario_granja")?.let { col ->
                val docRef = col.add(entrada.copy(id = localId))
                diarioAnimalDao.updateRemoteId(localId, docRef.id)
            }
        } catch (e: Exception) { println("Error Firebase Diario Granja: ${e.message}") }
    }

    suspend fun getDiarioAnimalPorId(id: Long): EntradaDiarioAnimalEntity? = diarioAnimalDao.getById(id)

    suspend fun actualizarDiarioAnimal(entrada: EntradaDiarioAnimalEntity) {
        diarioAnimalDao.update(entrada)
        try {
            entrada.remoteId?.let { rId -> getUserCollection("diario_granja")?.document(rId)?.set(entrada) }
        } catch (e: Exception) { println("Error Firebase Diario Granja Update: ${e.message}") }
    }

    suspend fun eliminarDiarioAnimal(id: Long) {
        val entrada = diarioAnimalDao.getById(id)
        diarioAnimalDao.deleteById(id)
        try {
            entrada?.remoteId?.let { rId -> getUserCollection("diario_granja")?.document(rId)?.delete() }
        } catch (e: Exception) { println("Error Firebase Diario Granja Delete: ${e.message}") }
    }
}