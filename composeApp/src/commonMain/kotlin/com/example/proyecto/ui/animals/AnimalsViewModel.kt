package com.example.proyecto.ui.animals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyecto.data.database.entity.AnimalEntity
import com.example.proyecto.data.database.entity.CercadoEntity
import com.example.proyecto.data.database.entity.ProductoEntity
import com.example.proyecto.data.database.entity.EntradaDiarioAnimalEntity
import com.example.proyecto.data.repository.AnimalRepository
import com.example.proyecto.data.repository.FichaAnimal
import com.example.proyecto.data.repository.JardineraRepository
import huertomanager.composeapp.generated.resources.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

class AnimalsViewModel(
    private val animalRepository: AnimalRepository,
    private val jardineraRepository: JardineraRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            animalRepository.descargarDatosNube()
        }
    }

    val animales: Flow<List<AnimalEntity>> = animalRepository.getAllAnimales()
    val cercados: Flow<List<CercadoEntity>> = animalRepository.getAllCercados()
    val catalogo: List<FichaAnimal> = animalRepository.getCatalogo()
    val diarioAnimales: Flow<List<EntradaDiarioAnimalEntity>> = animalRepository.getDiarioAnimales()

    suspend fun getEntradaDiarioAnimalById(id: Long): EntradaDiarioAnimalEntity? =
        animalRepository.getDiarioAnimalPorId(id)

    fun eliminarEntradaDiarioAnimal(id: Long) {
        viewModelScope.launch { animalRepository.eliminarDiarioAnimal(id) }
    }

    fun getFichaPorNombre(tipo: String) = animalRepository.getFichaPorNombre(tipo)

    fun addCercado(numero: Int, nombre: String) = viewModelScope.launch {
        animalRepository.insertCercado(CercadoEntity(numero = numero, nombre = nombre))
    }

    // ==========================================
    // GESTIÓN DE CERCADOS
    // ==========================================

    fun editarCercado(cercado: CercadoEntity, nuevoNumero: Int, nuevoNombre: String) {
        viewModelScope.launch {
            animalRepository.updateCercado(cercado.copy(numero = nuevoNumero, nombre = nuevoNombre))
        }
    }

    /**
     * Elimina el cercado junto con TODOS sus animales.
     * Registra una entrada BAJA en el diario por cada animal eliminado,
     * y una entrada ELIMINACIÓN DE CERCADO al final.
     */
    fun eliminarCercadoConAnimales(cercado: CercadoEntity, animales: List<AnimalEntity>) {
        viewModelScope.launch {
            val nombreCercado = "${cercado.numero} - ${cercado.nombre}"

            // 1. Dar de baja cada animal y registrarlo en el diario
            animales.forEach { animal ->
                animalRepository.deleteAnimal(animal)
                animalRepository.insertarDiarioAnimal(
                    EntradaDiarioAnimalEntity(
                        cercadoId = cercado.id,
                        animalTipo = animal.tipo,
                        tipoAccion = "BAJA ANIMAL",
                        descripcion = "Baja de ${animal.nombre} (${animal.tipo}) por eliminación del Cercado $nombreCercado.",
                        cantidad = 1.0
                    )
                )
            }

            // 2. Registrar la eliminación del propio cercado en el diario
            animalRepository.insertarDiarioAnimal(
                EntradaDiarioAnimalEntity(
                    cercadoId = cercado.id,
                    animalTipo = "",
                    tipoAccion = "ELIMINACIÓN CERCADO",
                    descripcion = "Se eliminó el Cercado $nombreCercado con ${animales.size} animal(es).",
                    cantidad = animales.size.toDouble()
                )
            )

            // 3. Eliminar el cercado
            animalRepository.deleteCercado(cercado)
        }
    }

    /**
     * Mueve todos los animales de [origen] al [destino] y luego elimina el cercado origen.
     * Registra el traslado de cada animal en el diario.
     */
    fun moverAnimalesYEliminarCercado(
        origen: CercadoEntity,
        animales: List<AnimalEntity>,
        destino: CercadoEntity
    ) {
        viewModelScope.launch {
            val nombreOrigen = "${origen.numero} - ${origen.nombre}"
            val nombreDestino = "${destino.numero} - ${destino.nombre}"

            // 1. Mover todos los animales en bloque (una sola query UPDATE en Room)
            animalRepository.moverAnimalesACercado(origen.id, destino.id)

            // 2. Registrar el traslado de cada animal en el diario
            animales.forEach { animal ->
                animalRepository.insertarDiarioAnimal(
                    EntradaDiarioAnimalEntity(
                        cercadoId = destino.id,
                        animalTipo = animal.tipo,
                        tipoAccion = "TRASLADO",
                        descripcion = "${animal.nombre} (${animal.tipo}) trasladado de Cercado $nombreOrigen a Cercado $nombreDestino.",
                        cantidad = 1.0
                    )
                )
            }

            // 3. Registrar la eliminación del cercado origen en el diario
            animalRepository.insertarDiarioAnimal(
                EntradaDiarioAnimalEntity(
                    cercadoId = origen.id,
                    animalTipo = "",
                    tipoAccion = "ELIMINACIÓN CERCADO",
                    descripcion = "Cercado $nombreOrigen eliminado. ${animales.size} animal(es) trasladado(s) a Cercado $nombreDestino.",
                    cantidad = animales.size.toDouble()
                )
            )

            // 4. Eliminar el cercado origen
            animalRepository.deleteCercado(origen)
        }
    }

    // ==========================================
    // ACCIONES CON REGISTRO AUTOMÁTICO EN DIARIO
    // ==========================================

    fun registrarPuestaGrupo(cercado: CercadoEntity, tipo: String, cantidad: Double) {
        viewModelScope.launch {
            val (nombreRes, urlFoto) = when (tipo.uppercase().trim()) {
                "GALLINA" -> Res.string.product_egg_gallina to "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8a/Eggs_x6.jpg/320px-Eggs_x6.jpg"
                "OCA"     -> Res.string.product_egg_oca     to "https://upload.wikimedia.org/wikipedia/commons/thumb/3/36/Goose_egg_2.jpg/320px-Goose_egg_2.jpg"
                "PERDIZ"  -> Res.string.product_egg_perdiz  to "https://upload.wikimedia.org/wikipedia/commons/thumb/1/18/Partridge_eggs.jpg/320px-Partridge_eggs.jpg"
                "CODORNIZ"-> Res.string.product_egg_codorniz to "https://upload.wikimedia.org/wikipedia/commons/thumb/5/5c/Quail_eggs.jpg/320px-Quail_eggs.jpg"
                "PATO"    -> Res.string.product_egg_pato    to "https://upload.wikimedia.org/wikipedia/commons/thumb/6/6c/Duck_eggs.jpg/320px-Duck_eggs.jpg"
                else      -> Res.string.product_egg_gallina to ""
            }
            val nombreTraducido = getString(nombreRes)
            val productos = jardineraRepository.getProductos().first()
            val existe = productos.find { it.nombre == nombreTraducido }

            if (existe != null) {
                jardineraRepository.insertarProducto(existe.copy(stock = existe.stock + cantidad, imagenUrl = urlFoto))
            } else {
                jardineraRepository.insertarProducto(ProductoEntity(nombre = nombreTraducido, categoria = "ANIMAL_PROD", stock = cantidad, imagenUrl = urlFoto))
            }

            val desc = getString(Res.string.diary_collect_msg, cantidad, nombreTraducido)
            animalRepository.insertarDiarioAnimal(
                EntradaDiarioAnimalEntity(
                    cercadoId = cercado.id,
                    animalTipo = tipo,
                    tipoAccion = "PRODUCCIÓN",
                    descripcion = "$desc - Cercado ${cercado.numero} (${cercado.nombre})",
                    cantidad = cantidad
                )
            )
        }
    }

    fun alimentarGrupo(cercado: CercadoEntity, tipo: String, cantidadSacos: Double) {
        viewModelScope.launch {
            val nombrePienso = getString(Res.string.product_pienso)
            val productos = jardineraRepository.getProductos().first()
            val pienso = productos.find { it.categoria == "PIENSO" || it.nombre.contains(nombrePienso, true) }

            if (pienso != null) {
                jardineraRepository.insertarProducto(pienso.copy(stock = (pienso.stock - cantidadSacos).coerceAtLeast(0.0)))
                val desc = getString(Res.string.diary_feed_msg, cantidadSacos, tipo)
                animalRepository.insertarDiarioAnimal(
                    EntradaDiarioAnimalEntity(
                        cercadoId = cercado.id,
                        animalTipo = tipo,
                        tipoAccion = "ALIMENTACIÓN",
                        descripcion = "$desc - Cercado ${cercado.numero} (${cercado.nombre})",
                        cantidad = cantidadSacos
                    )
                )
            }
        }
    }

    fun addAnimal(nombre: String, tipo: String, cercadoId: Long, esPonedora: Boolean, foto: ByteArray?) {
        viewModelScope.launch {
            val ficha = getFichaPorNombre(tipo)
            animalRepository.insertAnimal(
                AnimalEntity(
                    nombre = nombre, tipo = tipo, cercadoId = cercadoId,
                    esPonedora = esPonedora, raza = null,
                    fechaNacimiento = System.currentTimeMillis(),
                    fotoPerfil = foto,
                    compatibilidad = ficha?.compatibilidad ?: "General"
                )
            )

            val cercadosList = animalRepository.getAllCercados().first()
            val cercado = cercadosList.find { it.id == cercadoId }
            val nombreCercado = cercado?.let { "${it.numero} - ${it.nombre}" } ?: "Desconocido"

            animalRepository.insertarDiarioAnimal(
                EntradaDiarioAnimalEntity(
                    cercadoId = cercadoId,
                    animalTipo = tipo,
                    tipoAccion = "ALTA ANIMAL",
                    descripcion = "Alta de animal: $nombre ($tipo) en Cercado $nombreCercado.",
                    cantidad = 1.0
                )
            )
        }
    }

    fun borrarAnimal(animal: AnimalEntity) {
        viewModelScope.launch {
            animalRepository.deleteAnimal(animal)
            val cercadosList = animalRepository.getAllCercados().first()
            val cercado = cercadosList.find { it.id == animal.cercadoId }
            val nombreCercado = cercado?.let { "${it.numero} - ${it.nombre}" } ?: "Desconocido"
            animalRepository.insertarDiarioAnimal(
                EntradaDiarioAnimalEntity(
                    cercadoId = animal.cercadoId,
                    animalTipo = animal.tipo,
                    tipoAccion = "BAJA ANIMAL",
                    descripcion = "Se ha eliminado a ${animal.nombre} (${animal.tipo}) del Cercado $nombreCercado.",
                    cantidad = 1.0
                )
            )
        }
    }

    fun editarAnimal(animal: AnimalEntity, nuevoNombre: String) = viewModelScope.launch {
        animalRepository.updateAnimal(animal.copy(nombre = nuevoNombre))
    }

    fun editarEntradaDiarioAnimal(id: Long, nuevaDescripcion: String, nuevaCantidad: Double) {
        viewModelScope.launch {
            val entrada = animalRepository.getDiarioAnimalPorId(id)
            if (entrada != null) {
                animalRepository.actualizarDiarioAnimal(
                    entrada.copy(descripcion = nuevaDescripcion, cantidad = nuevaCantidad)
                )
            }
        }
    }

    fun guardarEntradaDiarioAnimal(
        id: Long = 0L,
        cercadoId: Long,
        tipo: String,
        desc: String,
        fecha: Long,
        foto: ByteArray?,
        cantidad: Float
    ) {
        viewModelScope.launch {
            animalRepository.insertarDiarioAnimal(
                EntradaDiarioAnimalEntity(
                    id = id, cercadoId = cercadoId, animalTipo = "",
                    tipoAccion = tipo, descripcion = desc,
                    cantidad = cantidad.toDouble(), fecha = fecha, foto = foto
                )
            )
        }
    }
}