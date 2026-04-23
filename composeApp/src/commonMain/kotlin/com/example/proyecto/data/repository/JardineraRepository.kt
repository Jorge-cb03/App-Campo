package com.example.proyecto.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.proyecto.data.database.AppDatabase
import com.example.proyecto.data.database.entity.*
import com.example.proyecto.util.NotificationManager
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.firestore.FirebaseFirestore
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.util.toByteArray
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import java.io.ByteArrayOutputStream

// --- CLASES LOCALES (INTACTAS) ---
data class PerenualImage(
    val regularUrl: String?,
    val mediumUrl: String? = null
)

data class PerenualSpecies(
    val id: Int,
    val commonName: String,
    val scientificName: List<String>,
    val defaultImage: PerenualImage?
)

class JardineraRepository(
    private val db: AppDatabase,
    private val firestore: FirebaseFirestore,
    // [NUEVO MULTI-TENANT] Inyectamos Auth para identificar al usuario actual
    private val auth: FirebaseAuth
) {
    private val httpClient = HttpClient()

    private val jardineraDao = db.jardineraDao()
    private val bancalDao = db.bancalDao()
    private val productoDao = db.productoDao()
    private val diarioDao = db.entradaDiarioDao()
    private val alertDao = db.alertDao()
    private val usuarioDao = db.usuarioDao()

    // [NUEVO MULTI-TENANT] Helper para obtener la colección privada del usuario
    // Si no hay usuario (invitado), devuelve null y las funciones remotas se saltan.
    private fun getUserCollection(collectionName: String): dev.gitlive.firebase.firestore.CollectionReference? {
        val currentUser = auth.currentUser
        return if (currentUser != null) {
            firestore.collection("usuarios").document(currentUser.uid).collection(collectionName)
        } else {
            null
        }
    }

    // [NUEVO HELPER] Compresión de imágenes para evitar crash en Room (>2MB)
    private fun comprimirImagen(bytes: ByteArray?): ByteArray? {
        if (bytes == null) return null
        return try {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            val outputStream = ByteArrayOutputStream()
            // 70% de calidad reduce una foto de 5MB a ~300KB
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            outputStream.toByteArray()
        } catch (e: Exception) {
            println("Error comprimiendo imagen: ${e.message}")
            null
        }
    }

    // --- SINCRONIZACIÓN DE ESTRUCTURA (JARDINERAS + BANCALES) ---
    suspend fun syncStructure() {
        try {
            // [NUEVO MULTI-TENANT] Usamos la colección privada. Si es null (invitado), salimos.
            val collection = getUserCollection("jardineras") ?: return
            val snapshot = collection.get()

            for (doc in snapshot.documents) {
                val remoteJardinera = doc.data<JardineraEntity>()
                val remoteId = doc.id

                // 2. Upsert Local Jardinera
                val existing = jardineraDao.getJardineraByRemoteId(remoteId)
                val localJardineraId = if (existing != null) {
                    val updated = remoteJardinera.copy(id = existing.id, remoteId = remoteId)
                    jardineraDao.updateJardinera(updated)
                    existing.id
                } else {
                    jardineraDao.insertJardinera(remoteJardinera.copy(remoteId = remoteId))
                }

                // 3. Descargar sus Bancales (Subcolección)
                val bancalesSnapshot = doc.reference.collection("bancales").get()
                for (bDoc in bancalesSnapshot.documents) {
                    val remoteBancal = bDoc.data<BancalEntity>()
                    val bRemoteId = bDoc.id

                    val existingBancal = bancalDao.getBancalByRemoteId(bRemoteId)

                    // CORRECCIÓN CLAVE: Mantenemos el ID local existente para que el NO_ACTION no borre el diario
                    val bancalToSave = remoteBancal.copy(
                        id = existingBancal?.id ?: 0L,
                        jardineraId = localJardineraId,
                        remoteId = bRemoteId
                    )

                    // Safe Upsert Manual
                    if (bancalDao.insertBancal(bancalToSave) == -1L) {
                        bancalDao.updateBancal(bancalToSave)
                    }
                }
            }
        } catch (e: Exception) {
            println("Error sync structure: ${e.message}")
        }
    }

    // --- NUEVO: SINCRONIZACIÓN DEL DIARIO ---
    suspend fun syncDiario() {
        try {
            // [NUEVO MULTI-TENANT] Sincronizamos solo el diario del usuario
            val collection = getUserCollection("diario") ?: return
            val snapshot = collection.get()
            for (doc in snapshot.documents) {
                val remoteEntry = doc.data<EntradaDiarioEntity>()
                // Verificamos si ya existe localmente para no duplicar (usando el margen de tiempo del DAO)
                val localExistente = diarioDao.getEntradaExistente(remoteEntry.bancalId, remoteEntry.tipoAccion, remoteEntry.fecha)
                if (localExistente == null) {
                    diarioDao.insertEntrada(remoteEntry.copy(id = 0))
                }
            }
        } catch (e: Exception) {
            println("Error sync diario: ${e.message}")
        }
    }

    // --- GESTIÓN DE USUARIO (PERFIL) ---
    fun getUsuarioActivo(): Flow<UsuarioEntity?> = usuarioDao.getUsuario()

    suspend fun registrarUsuarioLocal(nombre: String, email: String, foto: Any?) {
        var fotoBytes: ByteArray? = null

        // 1. Obtención de bytes (URL o Directo)
        val bytesOriginales = when (foto) {
            is ByteArray -> foto
            is String -> {
                try {
                    val response = httpClient.get(foto)
                    if (response.status.value in 200..299) {
                        response.bodyAsChannel().toByteArray()
                    } else null
                } catch (e: Exception) {
                    println("Error descargando foto: ${e.message}")
                    null
                }
            }
            else -> null
        }

        // Usamos el helper de compresión
        fotoBytes = comprimirImagen(bytesOriginales)

        usuarioDao.saveUsuario(
            UsuarioEntity(id = 1, nombre = nombre, email = email, fotoPerfil = fotoBytes)
        )
    }

    suspend fun getUsuario() = usuarioDao.getUsuario()

    suspend fun guardarUsuario(nombre: String, email: String, foto: ByteArray?) {
        // Mantenemos este método por compatibilidad, pero aplicamos compresión
        val comprimida = comprimirImagen(foto)
        usuarioDao.saveUsuario(UsuarioEntity(id = 1, nombre = nombre, email = email, fotoPerfil = comprimida))
    }

    // --- GESTIÓN DE ALERTAS ---
    fun getAlerts() = alertDao.getAllAlerts()

    suspend fun insertAlert(alert: AlertaEntity) {
        val localId = alertDao.insertAlert(alert)
        NotificationManager.scheduleNotification(
            title = alert.title,
            message = alert.description,
            epochSeconds = alert.dateTimeEpochMillis
        )
        try {
            // [NUEVO MULTI-TENANT] Guardado privado
            getUserCollection("alertas")?.let { collection ->
                val alertToRemote = alert.copy(id = localId)
                val docRef = collection.add(alertToRemote)
                alertDao.updateRemoteId(localId, docRef.id)
                alertDao.updateSyncStatus(localId, true)
            }
        } catch (e: Exception) {
            println("Error sync alert: ${e.message}")
        }
    }

    suspend fun updateAlert(alert: AlertaEntity) {
        alertDao.updateAlert(alert)
        NotificationManager.scheduleNotification(
            title = alert.title,
            message = alert.description,
            epochSeconds = alert.dateTimeEpochMillis
        )
        try {
            alert.remoteId?.let { rId ->
                // [NUEVO MULTI-TENANT] Update privado
                getUserCollection("alertas")?.document(rId)?.set(alert)
                alertDao.updateSyncStatus(alert.id, true)
            }
        } catch (e: Exception) {
            alertDao.updateSyncStatus(alert.id, false)
        }
    }

    suspend fun deleteAlert(alert: AlertaEntity) {
        alertDao.deleteAlert(alert)
        try {
            alert.remoteId?.let { rId ->
                // [NUEVO MULTI-TENANT] Borrado privado
                getUserCollection("alertas")?.document(rId)?.delete()
            }
        } catch (e: Exception) {
            println("Error deleting remote alert: ${e.message}")
        }
    }

    suspend fun syncAlerts() {
        try {
            // [NUEVO MULTI-TENANT] Sync privado
            val collection = getUserCollection("alertas") ?: return
            val snapshot = collection.get()
            for (doc in snapshot.documents) {
                val remoteAlert = doc.data<AlertaEntity>()
                val remoteId = doc.id
                val existing = alertDao.getAlertByRemoteId(remoteId)
                if (existing != null) {
                    alertDao.updateAlert(remoteAlert.copy(id = existing.id, remoteId = remoteId, isSynced = true))
                } else {
                    alertDao.insertAlert(remoteAlert.copy(remoteId = remoteId, isSynced = true))
                }
            }
        } catch (e: Exception) {
            println("Error syncing alerts: ${e.message}")
        }
    }

    // --- DIARIO ---
    suspend fun getEntradaDiarioById(id: Long) = diarioDao.getEntradaById(id)

    // Cambia o añade esta función para un borrado TOTAL
    suspend fun eliminarJardineraCompleta(jardineraId: Long) {
        try {
            val bancales = bancalDao.getBancalesByJardinera(jardineraId).first()
            bancales.forEach { bancal ->
                // 1. Borrado Local
                val historial = diarioDao.getDiarioByBancal(bancal.id).first()
                historial.forEach { entrada -> diarioDao.deleteById(entrada.id) }

                // 2. Borrado Remoto con sintaxis GitLive [NUEVO MULTI-TENANT]
                try {
                    val collection = getUserCollection("diario")
                    if (collection != null) {
                        val remoteEntries = collection.where { "bancalId" equalTo bancal.id }.get()
                        remoteEntries.documents.forEach { it.reference.delete() }
                    }
                } catch (e: Exception) { println("Error borrado remoto diario: ${e.message}") }
            }

            // 3. Borrar estructura
            bancalDao.deleteBancalesFueraDeRango(jardineraId, 0, 0)
            val j = jardineraDao.getJardineraById(jardineraId)
            if (j != null) {
                j.remoteId?.let { rId ->
                    // [NUEVO MULTI-TENANT]
                    try { getUserCollection("jardineras")?.document(rId)?.delete() } catch (e: Exception) {}
                }
                jardineraDao.deleteJardinera(j)
            }
        } catch (e: Exception) { println("Error borrado total: ${e.message}") }
    }

    suspend fun eliminarEntradaDiario(id: Long) = diarioDao.deleteById(id)
    fun getTodoElHistorial() = diarioDao.getAllEntradas()
    fun getHistorialBancal(id: Long) = diarioDao.getDiarioByBancal(id)

    // ===================================================================================
    // CATÁLOGO MAESTRO
    // ===================================================================================
    data class FichaCultivo(
        val id: Int,
        val nombre: String,
        val cientifico: String,
        val imagenUrl: String,
        val riegoDias: Int,
        val sol: String,
        val germinacion: String,
        val amigos: String,
        val enemigos: String,
        val consejo: String
    )

    private val catalogoMaestro = listOf(
        FichaCultivo(1, "Tomate", "Solanum lycopersicum", "https://image.tuasaude.com/media/article/cd/dd/beneficios-do-tomate_14243.jpg?width=686&height=487", 3, "Pleno Sol", "5-10 días", "Albahaca, Zanahoria", "Patata, Pepino", "Entutora la planta y quita los chupones axilares."),
        FichaCultivo(2, "Tomate Cherry", "S. lycopersicum var. cerasiforme", "https://www.infobae.com/resizer/v2/ZAVPRWEOAJDANN4D62IBYWGWBA.jpeg?auth=f9851cde38b4293eabd89f992cb7d58bb900669ba9f02a29e993cfb280e834a7&smart=true&width=1200&height=1200&quality=85", 2, "Pleno Sol", "5-8 días", "Albahaca, Ajo", "Patata", "Ideal macetas. No mojes las hojas al regar."),
        FichaCultivo(3, "Lechuga", "Lactuca sativa", "https://s1.abcstatics.com/media/bienestar/2020/09/01/lechuga-kSlD--1248x698@abc.jpg", 2, "Sombra Parcial", "4-10 días", "Fresa, Zanahoria", "Perejil", "Planta escalonada para tener siempre fresca."),
        FichaCultivo(4, "Pimiento", "Capsicum annuum", "https://corp.ametllerorigen.com/wp-content/uploads/2023/11/Blog_pebrot.jpg", 3, "Sol y Calor", "8-12 días", "Albahaca, Cebolla", "Judías", "Necesita mucho calor para germinar."),
        FichaCultivo(5, "Cebolla", "Allium cepa", "https://www.josebernad.com/wp-content/uploads/2019/07/tipos-cebollas-1024x577.jpg", 4, "Pleno Sol", "10-15 días", "Tomate, Zanahoria", "Guisantes", "Deja de regar cuando las hojas caigan para madurar."),
        FichaCultivo(6, "Zanahoria", "Daucus carota", "https://i.blogs.es/127977/carrots-2387394_1280-1-/1366_2000.jpg", 3, "Sol", "12-15 días", "Tomate, Puerro", "Eneldo", "Tierra muy suelta y sin piedras o saldrán deformes."),
        FichaCultivo(7, "Ajo", "Allium sativum", "https://scrippsamg.com/wp-content/uploads/2023/03/6_-_National_Garlic_Day_2.jpg", 5, "Pleno Sol", "10-15 días", "Fresa, Rosas", "Legumbres", "Planta el diente con la punta hacia arriba."),
        FichaCultivo(8, "Patata", "Solanum tuberosum", "https://bioky.es/wp-content/uploads/2023/12/planta-patata.jpg", 4, "Sol", "15-20 días", "Maíz, Judías", "Tomate", "Cubre con tierra la base (aporcar) conforme crezca."),
        FichaCultivo(9, "Berenjena", "Solanum melongena", "https://recetasdecocina.elmundo.es/wp-content/uploads/2022/03/berenjena.jpg", 3, "Mucho Sol", "10-15 días", "Judías", "Patata", "Consume muchos nutrientes, abona bien."),
        FichaCultivo(10, "Pepino", "Cucumis sativus", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQdjQU6-PKPgfIiUdvU5A_mlL1V6QCX2DI1zQ&s", 2, "Sol/Humedad", "3-10 días", "Maíz, Lechuga", "Patata", "Ponle una red para que trepe y los frutos no toquen suelo."),
        FichaCultivo(11, "Calabacín", "Cucurbita pepo", "https://www.frutas-hortalizas.com/img/fruites_verdures/presentacio/44.jpg", 2, "Sol", "5-10 días", "Maíz, Capuchina", "Patata", "Cosecha cuando sean pequeños (20cm)."),
        FichaCultivo(12, "Fresa", "Fragaria", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRiEqDgTImHMG_JCK0YuhyvzSaql86IlM4Saw&s", 2, "Sol/Sombra", "20-30 días", "Ajo, Espinaca", "Repollo", "Usa paja en el suelo para proteger el fruto."),
        FichaCultivo(13, "Albahaca", "Ocimum basilicum", "https://red-hill.es/wp-content/uploads/2024/06/7e2db098-albahaca-basil-adobestock_81129315-scaled-1.jpeg", 2, "Sol/Sombra", "5-10 días", "Tomate, Pimiento", "Ruda", "Repele la mosca blanca y mejora el sabor del tomate."),
        FichaCultivo(14, "Maíz", "Zea mays", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQs_O4asHhveKAdtu7UNyiJfbWocmTZ4x1u6g&s", 3, "Pleno Sol", "7-10 días", "Judías, Calabaza, Pepino", "Tomate", "Planta en bloques para mejorar la polinización."),
        FichaCultivo(15, "Judía Verde", "Phaseolus vulgaris", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRKIjiPNdEInSIm7wu4UzySp0A3A3zhpgg6ig&s", 3, "Sol", "7-10 días", "Patata, Maíz, Fresa", "Cebolla, Ajo", "Fija nitrógeno en el suelo. Necesita tutor si es de enrame."),
        FichaCultivo(16, "Puerro", "Allium ampeloprasum", "https://s2.abcstatics.com/media/bienestar/2020/09/22/puerros-knaC--1248x698@abc.jpg", 4, "Sol", "10-15 días", "Zanahoria, Apio, Fresa", "Judías, Guisantes", "Aporca (cubre) el tallo con tierra para blanquearlo."),
        FichaCultivo(17, "Espinaca", "Spinacia oleracea", "https://www.conasi.eu/blog/wp-content/uploads/2023/07/recetas-con-espinacas-1.jpg", 2, "Sombra/Sol", "10-15 días", "Fresa, Col, Judías", "Patata", "Crece muy rápido. Prefiere clima fresco, con calor se espiga."),
        FichaCultivo(18, "Guisante", "Pisum sativum", "https://petitfitbycris.com/wp-content/uploads/2020/02/guisantes.jpg", 3, "Fresco/Sol", "7-14 días", "Zanahoria, Rábano, Pepino", "Cebolla, Ajo", "Le gusta el clima fresco. Aporta nitrógeno a la tierra."),
        FichaCultivo(19, "Repollo", "Brassica oleracea", "https://www.supermasymas.com/blog/wp-content/uploads/2024/08/repollo-portada.jpg", 4, "Sol", "5-10 días", "Patata, Espinaca, Eneldo", "Fresa, Tomate", "Vigila las orugas y mariposas blancas."),
        FichaCultivo(20, "Calabaza", "Cucurbita moschata", "https://i.blogs.es/703eab/pumpkins-457716_1280/1366_2000.jpg", 3, "Pleno Sol", "5-10 días", "Maíz, Judías", "Patata", "Necesita mucho espacio y suelo rico en nutrientes."),
        FichaCultivo(21, "Rábano", "Raphanus sativus", "https://www.gob.mx/cms/uploads/article/main_image/44625/r_bano1.jpg", 2, "Sol/Sombra", "3-6 días", "Lechuga, Espinaca, Zanahoria", "Repollo, Coliflor", "El cultivo más rápido. Ideal para principiantes."),
        FichaCultivo(22, "Perejil", "Petroselinum crispum", "https://metode.es/wp-content/uploads/2017/06/106-91-julivert.jpg", 3, "Sombra/Sol", "15-25 días", "Tomate, Espárrago", "Lechuga", "Tarda mucho en germinar, ten paciencia y mantén la humedad.")
    )

    fun getFichaCompleta(id: Int): FichaCultivo? = catalogoMaestro.find { it.id == id }
    fun getFichaPorNombre(nombre: String): FichaCultivo? = catalogoMaestro.find { it.nombre.equals(nombre, ignoreCase = true) }

    suspend fun buscarCultivosOnline(query: String): List<PerenualSpecies> {
        val q = query.lowercase().trim()
        return catalogoMaestro
            .filter { it.nombre.lowercase().contains(q) }
            .map { PerenualSpecies(id = it.id, commonName = it.nombre, scientificName = listOf(it.cientifico), defaultImage = PerenualImage(it.imagenUrl, null)) }
    }

    // --- MÉTODOS CRUD ---
    fun getJardineras() = jardineraDao.getJardinerasActivas()
    fun getJardinerasArchivadas() = jardineraDao.getJardinerasArchivadas()
    fun getBancales(id: Long) = bancalDao.getBancalesByJardinera(id)
    suspend fun getBancalById(id: Long) = bancalDao.getBancalById(id)

    fun getProductos() = productoDao.getAllProductos()
    suspend fun getProductoById(id: Long) = productoDao.getProductoById(id)
    suspend fun eliminarProducto(id: Long) {
        try {
            val producto = productoDao.getProductoById(id)
            if (producto != null) {
                // 1. Borrado local
                productoDao.deleteProductoById(id)

                // 2. Borrado remoto en Firestore [NUEVO MULTI-TENANT]
                producto.remoteId?.let { rId ->
                    getUserCollection("productos")?.document(rId)?.delete()
                }
            }
        } catch (e: Exception) {
            println("Error al eliminar producto: ${e.message}")
        }
    }

    suspend fun insertarProducto(p: ProductoEntity) {
        val localId = productoDao.insertProducto(p)
        try {
            // [NUEVO MULTI-TENANT]
            getUserCollection("productos")?.let { collection ->
                if (p.remoteId != null) {
                    val productoConId = p.copy(id = localId)
                    collection.document(p.remoteId).set(productoConId)
                } else {
                    val productoConId = p.copy(id = localId)
                    val docRef = collection.add(productoConId)
                    productoDao.updateRemoteId(localId, docRef.id)
                }
            }
        } catch (e: Exception) {
            println("Error subida producto: ${e.message}")
        }
    }

    suspend fun syncProductos() {
        try {
            // [NUEVO MULTI-TENANT]
            val collection = getUserCollection("productos") ?: return
            val snapshot = collection.get()
            val listaParaGuardar = snapshot.documents.mapNotNull { doc ->
                val dataRemota = doc.data<ProductoEntity>()
                val remoteId = doc.id
                val existente = productoDao.getProductoByRemoteId(remoteId)
                if (existente != null) {
                    dataRemota.copy(id = existente.id, remoteId = remoteId)
                } else {
                    dataRemota.copy(remoteId = remoteId)
                }
            }
            if (listaParaGuardar.isNotEmpty()) {
                productoDao.insertAll(listaParaGuardar)
            }
        } catch (e: Exception) {
            println("Error sync productos: ${e.message}")
        }
    }

    // 1. CORRECCIÓN RIEGO: Eliminamos la comprobación de duplicados para permitir riegos seguidos
    suspend fun registrarRiego(id: Long, l: Double) {
        val nuevaEntrada = EntradaDiarioEntity(
            bancalId = id,
            tipoAccion = "RIEGO",
            descripcion = "Riego rápido: $l L.",
            fecha = System.currentTimeMillis() // Esto garantiza un ID temporal casi único
        )
        insertarEntradaDiario(nuevaEntrada)
    }

    // 2. CORRECCIÓN TRATAMIENTO: Aseguramos que se registre siempre con la hora actual
    suspend fun registrarTratamiento(id: Long, pId: Long, cant: Double, t: String) {
        val p = productoDao.getProductoById(pId) ?: return
        val nuevoStock = p.stock - cant

        if (nuevoStock <= 0) {
            productoDao.deleteProductoById(p.id)
        } else {
            productoDao.updateProducto(p.copy(stock = nuevoStock))
        }

        // Insertamos directamente usando la hora del sistema
        insertarEntradaDiario(EntradaDiarioEntity(
            bancalId = id,
            tipoAccion = t,
            descripcion = "$t: $cant de ${p.nombre}",
            fecha = System.currentTimeMillis()
        ))
    }

    // 3. [ARREGLO CRASH] INSERCIÓN SEGURA CON COMPRESIÓN DE FOTO
    suspend fun insertarEntradaDiario(e: EntradaDiarioEntity) {
        try {
            // Comprimimos la foto antes de guardar
            val fotoComprimida = comprimirImagen(e.foto)
            val entradaSegura = e.copy(foto = fotoComprimida)

            val localId = diarioDao.insertEntrada(entradaSegura)
            val entradaParaSubir = entradaSegura.copy(id = localId)

            getUserCollection("diario")?.add(entradaParaSubir)
        } catch (ex: Exception) {
            println("Error en persistencia rápida: ${ex.message}")
        }
    }

    // 3. SET ESTADO FUNCIONAL (El interruptor de "Bancal Activo")
    suspend fun setEstadoFuncionalBancal(id: Long, f: Boolean) {
        bancalDao.getBancalById(id)?.let { bancal ->
            val updatedBancal = bancal.copy(esFuncional = f)
            // Usamos la lógica de Safe Upsert para no romper el vínculo del diario
            if (bancalDao.insertBancal(updatedBancal) == -1L) {
                bancalDao.updateBancal(updatedBancal)
            }
            uploadBancalChange(updatedBancal)
        }
    }

    private suspend fun uploadBancalChange(bancal: BancalEntity) {
        try {
            val jardinera = jardineraDao.getJardineraById(bancal.jardineraId)
            val parentRemoteId = jardinera?.remoteId
            val bancalRemoteId = bancal.remoteId
            if (parentRemoteId != null && bancalRemoteId != null) {
                // [NUEVO MULTI-TENANT]
                getUserCollection("jardineras")
                    ?.document(parentRemoteId)
                    ?.collection("bancales")
                    ?.document(bancalRemoteId)
                    ?.set(bancal)
            }
        } catch (e: Exception) {
            println("Error subiendo bancal: ${e.message}")
        }
    }

    private suspend fun uploadJardineraChange(jardinera: JardineraEntity) {
        try {
            if (jardinera.remoteId != null) {
                // [NUEVO MULTI-TENANT]
                getUserCollection("jardineras")
                    ?.document(jardinera.remoteId)
                    ?.set(jardinera)
            }
        } catch (e: Exception) {
            println("Error subiendo jardinera: ${e.message}")
        }
    }

    suspend fun cosecharBancal(id: Long) {
        bancalDao.getBancalById(id)?.let { b ->
            val bancalVacio = b.copy(
                perenualId = null,
                nombreCultivo = null,
                imagenUrl = null,
                fechaSiembra = null,
                frecuenciaRiegoDias = null,
                necesidadSol = null
            )
            if (bancalDao.insertBancal(bancalVacio) == -1L) {
                bancalDao.updateBancal(bancalVacio)
            }
            uploadBancalChange(bancalVacio)
        }
    }

    suspend fun plantarEnBancal(bancalId: Long, localId: Int) {
        val productoLocal = productoDao.getProductoByPerenualId(localId) ?: return
        if (productoLocal.stock <= 0) return
        val ficha = catalogoMaestro.find { it.id == localId } ?: return

        bancalDao.getBancalById(bancalId)?.let { bancal ->
            val bancalPlantado = bancal.copy(
                perenualId = localId,
                nombreCultivo = ficha.nombre,
                imagenUrl = ficha.imagenUrl,
                frecuenciaRiegoDias = ficha.riegoDias,
                necesidadSol = ficha.sol,
                fechaSiembra = System.currentTimeMillis()
            )
            if (bancalDao.insertBancal(bancalPlantado) == -1L) {
                bancalDao.updateBancal(bancalPlantado)
            }
            uploadBancalChange(bancalPlantado)

            insertarEntradaDiario(EntradaDiarioEntity(
                bancalId = bancalId,
                tipoAccion = "SIEMBRA",
                descripcion = "Siembra: ${ficha.nombre}.\n💡 Consejo: ${ficha.consejo}",
                fecha = System.currentTimeMillis()
            ))
        }

        val nuevoStock = productoLocal.stock - 1.0
        if (nuevoStock <= 0) {
            productoDao.deleteProductoById(productoLocal.id)
        } else {
            insertarProducto(productoLocal.copy(stock = nuevoStock))
        }
    }

    suspend fun existeJardinera(nombre: String): Boolean {
        val activas = jardineraDao.getJardinerasActivas().first()
        return activas.any { it.nombre.trim().equals(nombre.trim(), ignoreCase = true) }
    }

    suspend fun crearJardineraConBancales(n: String, f: Int, c: Int) {
        val jardinera = JardineraEntity(nombre = n, filas = f, columnas = c)
        val localId = jardineraDao.insertJardinera(jardinera)

        val bancales = mutableListOf<BancalEntity>()
        for (r in 0 until f) {
            for (ci in 0 until c) {
                val bancal = BancalEntity(jardineraId = localId, fila = r, columna = ci)
                val bId = bancalDao.insertBancal(bancal)
                val finalId = if (bId == -1L) {
                    bancalDao.updateBancal(bancal)
                    bancalDao.getBancalesByJardinera(localId).first().find { it.fila == r && it.columna == ci }?.id ?: 0L
                } else bId

                bancales.add(bancal.copy(id = finalId))
            }
        }

        try {
            // [NUEVO MULTI-TENANT]
            getUserCollection("jardineras")?.let { collection ->
                val remoteJardinera = jardinera.copy(id = localId)
                val docRef = collection.add(remoteJardinera)
                jardineraDao.updateRemoteId(localId, docRef.id)

                val subCollection = docRef.collection("bancales")
                bancales.forEach { b ->
                    val bRef = subCollection.add(b)
                    bancalDao.updateRemoteId(b.id, bRef.id)
                }
            }
        } catch (e: Exception) {
            println("Error create jardinera: ${e.message}")
        }
    }

    suspend fun actualizarJardinera(j: JardineraEntity) {
        jardineraDao.updateJardinera(j)
        syncBancalesLocal(j.id, j.filas, j.columnas)
        uploadJardineraChange(j)
    }

    suspend fun archivarJardinera(j: JardineraEntity) {
        val archived = j.copy(estaArchivada = true)
        jardineraDao.updateJardinera(archived)
        uploadJardineraChange(archived)
    }

    suspend fun desarchivarJardinera(j: JardineraEntity) {
        val unarchived = j.copy(estaArchivada = false)
        jardineraDao.updateJardinera(unarchived)
        uploadJardineraChange(unarchived)
    }

    suspend fun regarTodaLaJardinera(jId: Long) {
        bancalDao.getBancalesByJardinera(jId).first().filter { it.perenualId != null }.forEach { registrarRiego(it.id, 1.0) }
    }

    private suspend fun syncBancalesLocal(id: Long, f: Int, c: Int) {
        bancalDao.deleteBancalesFueraDeRango(id, f, c)
        val actuales = bancalDao.getBancalesByJardinera(id).first()
        for (r in 0 until f) {
            for (ci in 0 until c) {
                if (actuales.none { it.fila == r && it.columna == ci }) {
                    val nuevoBancal = BancalEntity(jardineraId = id, fila = r, columna = ci)
                    if (bancalDao.insertBancal(nuevoBancal) == -1L) bancalDao.updateBancal(nuevoBancal)
                }
            }
        }
    }

    suspend fun limpiarAlertasAntiguas() {
        val unDiaEnMillis = 24 * 60 * 60 * 1000L
        val ahora = System.currentTimeMillis()
        val todas = alertDao.getAllAlerts().first()
        todas.forEach { alerta ->
            if (ahora - alerta.dateTimeEpochMillis > unDiaEnMillis) {
                deleteAlert(alerta)
            }
        }
    }

    suspend fun limpiarDatosUsuario() {
        usuarioDao.borrarTodo()
    }

    suspend fun borrarDatosLocales() {
        usuarioDao.borrarTodo()
    }
}