package com.example.proyecto.data.repository

import com.example.proyecto.data.database.AppDatabase
import com.example.proyecto.data.database.entity.AnimalEntity
import com.example.proyecto.data.database.entity.CercadoEntity
import com.example.proyecto.data.database.entity.EntradaDiarioAnimalEntity
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow

data class FichaAnimal(
    val nombre: String,
    val esPonedora: Boolean,
    val compatibilidad: String,
    val imagenUrl: String
)

class AnimalRepository(
    private val db: AppDatabase,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val animalDao = db.animalDao()
    private val diarioDao = db.diarioAnimalDao()

    fun getCatalogo(): List<FichaAnimal> = listOf(
        FichaAnimal("Gallina", true, "Aves", "https://images.unsplash.com/photo-1548550023-2bdb3c5beed7?q=80&w=400"),
        FichaAnimal("Oca", true, "Aves", "https://plus.unsplash.com/premium_photo-1675731320300-34907106d64d?q=80&w=400"),
        FichaAnimal("Perdiz", true, "Aves", "https://images.unsplash.com/photo-1582722872445-44dc5f7e3c8f?q=80&w=400"),
        FichaAnimal("Codorniz", true, "Aves", "https://images.unsplash.com/photo-1582722872445-44dc5f7e3c8f?q=80&w=400"),
        FichaAnimal("Pato", true, "Aves", "https://images.unsplash.com/photo-1582722872445-44dc5f7e3c8f?q=80&w=400"),
        FichaAnimal("Perro Pastor", false, "Perros", "https://images.unsplash.com/photo-1517849845537-4d257902454a?q=80&w=400")
    )

    fun getFichaPorNombre(tipo: String) = getCatalogo().find { it.nombre.equals(tipo, ignoreCase = true) }

    // ==========================================
    // CERCADOS
    // ==========================================
    fun getAllCercados(): Flow<List<CercadoEntity>> = animalDao.getAllCercados()

    suspend fun insertCercado(cercado: CercadoEntity) {
        val id = animalDao.insertCercado(cercado)
        syncToFirebase("cercados", id.toString(), mapOf(
            "id" to id,
            "numero" to cercado.numero,
            "nombre" to cercado.nombre
        ))
    }

    suspend fun updateCercado(cercado: CercadoEntity) {
        animalDao.updateCercado(cercado)
        syncToFirebase("cercados", cercado.id.toString(), mapOf(
            "id" to cercado.id,
            "numero" to cercado.numero,
            "nombre" to cercado.nombre
        ))
    }

    /**
     * Elimina el cercado de Room y de Firebase.
     * No toca los animales: el llamador decide antes si los elimina o mueve.
     */
    suspend fun deleteCercado(cercado: CercadoEntity) {
        animalDao.deleteCercado(cercado)
        deleteFromFirebase("cercados", cercado.id.toString())
    }

    /**
     * Mueve todos los animales de [origenId] al [destinoId] en Room y en Firebase.
     */
    suspend fun moverAnimalesACercado(origenId: Long, destinoId: Long) {
        animalDao.moverAnimalesACercado(origenId, destinoId)

        // Reflejamos el cambio en Firebase para cada animal afectado
        // (actualizamos el campo cercadoId en cada documento)
        val user = auth.currentUser ?: return
        try {
            val animalesNube = firestore
                .collection("usuarios").document(user.uid)
                .collection("animales").get()

            animalesNube.documents
                .filter { (it.get("cercadoId") as? Number)?.toLong() == origenId }
                .forEach { doc ->
                    firestore.collection("usuarios").document(user.uid)
                        .collection("animales").document(doc.id)
                        .update(mapOf("cercadoId" to destinoId))
                }
        } catch (e: Exception) {
            println("Error actualizando cercadoId en Firebase: ${e.message}")
        }
    }

    // ==========================================
    // ANIMALES
    // ==========================================
    fun getAllAnimales(): Flow<List<AnimalEntity>> = animalDao.getAllAnimales()

    suspend fun insertAnimal(animal: AnimalEntity) {
        val id = animalDao.insertAnimal(animal)
        syncToFirebase("animales", id.toString(), mapOf(
            "id" to id,
            "nombre" to animal.nombre,
            "tipo" to animal.tipo,
            "cercadoId" to animal.cercadoId,
            "esPonedora" to animal.esPonedora,
            "compatibilidad" to animal.compatibilidad,
            "fechaNacimiento" to animal.fechaNacimiento
        ))
    }

    suspend fun updateAnimal(animal: AnimalEntity) {
        animalDao.updateAnimal(animal)
        syncToFirebase("animales", animal.id.toString(), mapOf(
            "id" to animal.id,
            "nombre" to animal.nombre,
            "tipo" to animal.tipo,
            "cercadoId" to animal.cercadoId,
            "esPonedora" to animal.esPonedora,
            "compatibilidad" to animal.compatibilidad,
            "fechaNacimiento" to animal.fechaNacimiento
        ))
    }

    suspend fun deleteAnimal(animal: AnimalEntity) {
        animalDao.deleteAnimal(animal)
        deleteFromFirebase("animales", animal.id.toString())
    }

    // ==========================================
    // DIARIO ANIMALES
    // ==========================================
    fun getDiarioAnimales(): Flow<List<EntradaDiarioAnimalEntity>> = diarioDao.getAllLogs()

    suspend fun getDiarioAnimalPorId(id: Long): EntradaDiarioAnimalEntity? = diarioDao.getById(id)

    suspend fun insertarDiarioAnimal(entrada: EntradaDiarioAnimalEntity) {
        val id = diarioDao.insert(entrada)
        syncToFirebase("diario_animales", id.toString(), mapOf(
            "id" to id,
            "cercadoId" to entrada.cercadoId,
            "animalTipo" to entrada.animalTipo,
            "tipoAccion" to entrada.tipoAccion,
            "descripcion" to entrada.descripcion,
            "cantidad" to entrada.cantidad,
            "fecha" to entrada.fecha
        ))
    }

    suspend fun actualizarDiarioAnimal(entrada: EntradaDiarioAnimalEntity) {
        diarioDao.update(entrada)
        syncToFirebase("diario_animales", entrada.id.toString(), mapOf(
            "id" to entrada.id,
            "cercadoId" to entrada.cercadoId,
            "animalTipo" to entrada.animalTipo,
            "tipoAccion" to entrada.tipoAccion,
            "descripcion" to entrada.descripcion,
            "cantidad" to entrada.cantidad,
            "fecha" to entrada.fecha
        ))
    }

    suspend fun eliminarDiarioAnimal(id: Long) {
        diarioDao.deleteById(id)
        deleteFromFirebase("diario_animales", id.toString())
    }

    // ==========================================
    // FUNCIONES INTERNAS DE FIREBASE
    // ==========================================
    private suspend fun syncToFirebase(coleccion: String, documentId: String, data: Map<String, Any?>) {
        val user = auth.currentUser
        if (user != null) {
            try {
                firestore.collection("usuarios").document(user.uid)
                    .collection(coleccion).document(documentId).set(data)
            } catch (e: Exception) {
                println("Error guardando en Firebase: ${e.message}")
            }
        }
    }

    private suspend fun deleteFromFirebase(coleccion: String, documentId: String) {
        val user = auth.currentUser
        if (user != null) {
            try {
                firestore.collection("usuarios").document(user.uid)
                    .collection(coleccion).document(documentId).delete()
            } catch (e: Exception) {
                println("Error borrando de Firebase: ${e.message}")
            }
        }
    }

    // ==========================================
    // SINCRONIZACIÓN AL INICIAR LA APP
    // ==========================================
    suspend fun descargarDatosNube() {
        val user = auth.currentUser ?: return
        try {
            val cercadosNube = firestore.collection("usuarios").document(user.uid).collection("cercados").get()
            cercadosNube.documents.forEach { doc ->
                val id = doc.get("id") as? Long ?: return@forEach
                val numero = (doc.get("numero") as? Number)?.toInt() ?: 0
                val nombre = doc.get("nombre") as? String ?: ""
                animalDao.insertCercado(CercadoEntity(id = id, numero = numero, nombre = nombre))
            }

            val animalesNube = firestore.collection("usuarios").document(user.uid).collection("animales").get()
            animalesNube.documents.forEach { doc ->
                val id = doc.get("id") as? Long ?: return@forEach
                val nombre = doc.get("nombre") as? String ?: ""
                val tipo = doc.get("tipo") as? String ?: ""
                val cercadoId = (doc.get("cercadoId") as? Number)?.toLong() ?: 0L
                val esPonedora = doc.get("esPonedora") as? Boolean ?: false
                val compatibilidad = doc.get("compatibilidad") as? String ?: "General"
                val fecha = (doc.get("fechaNacimiento") as? Number)?.toLong() ?: System.currentTimeMillis()
                animalDao.insertAnimal(
                    AnimalEntity(
                        id = id, nombre = nombre, tipo = tipo, cercadoId = cercadoId,
                        esPonedora = esPonedora, raza = null, fechaNacimiento = fecha,
                        fotoPerfil = null, compatibilidad = compatibilidad
                    )
                )
            }

            val diarioNube = firestore.collection("usuarios").document(user.uid).collection("diario_animales").get()
            diarioNube.documents.forEach { doc ->
                val id = doc.get("id") as? Long ?: return@forEach
                val cercadoId = (doc.get("cercadoId") as? Number)?.toLong() ?: 0L
                val animalTipo = doc.get("animalTipo") as? String ?: ""
                val tipoAccion = doc.get("tipoAccion") as? String ?: ""
                val descripcion = doc.get("descripcion") as? String ?: ""
                val cantidad = (doc.get("cantidad") as? Number)?.toDouble() ?: 0.0
                val fecha = (doc.get("fecha") as? Number)?.toLong() ?: System.currentTimeMillis()
                diarioDao.insert(
                    EntradaDiarioAnimalEntity(
                        id = id, cercadoId = cercadoId, animalTipo = animalTipo,
                        tipoAccion = tipoAccion, descripcion = descripcion,
                        cantidad = cantidad, fecha = fecha, foto = null
                    )
                )
            }
        } catch (e: Exception) {
            println("Error sincronizando de Firebase: ${e.message}")
        }
    }
}