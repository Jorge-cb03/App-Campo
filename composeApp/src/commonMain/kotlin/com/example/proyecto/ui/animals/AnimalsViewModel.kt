package com.example.proyecto.ui.animals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
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

    val animales: Flow<List<AnimalEntity>> = animalRepository.getAllAnimales()
    val cercados: Flow<List<CercadoEntity>> = animalRepository.getAllCercados()
    val catalogo: List<FichaAnimal> = animalRepository.getCatalogo()

    // --- DIARIO DE ANIMALES ---
    val diarioAnimales: Flow<List<EntradaDiarioAnimalEntity>> = animalRepository.getDiarioAnimales()

    fun eliminarEntradaDiarioAnimal(id: Long) {
        viewModelScope.launch { animalRepository.eliminarDiarioAnimal(id) }
    }

    fun getFichaPorNombre(tipo: String) = animalRepository.getFichaPorNombre(tipo)

    fun addCercado(numero: Int, nombre: String) = viewModelScope.launch {
        animalRepository.insertCercado(CercadoEntity(numero = numero, nombre = nombre))
    }

    // --- ACCIONES CON REGISTRO AUTOMÁTICO EN DIARIO ---

    fun registrarPuestaGrupo(cercado: CercadoEntity, tipo: String, cantidad: Double) {
        viewModelScope.launch {
            val (nombreRes, urlFoto) = when (tipo.uppercase().trim()) {
                "GALLINA" -> Res.string.product_egg_gallina to "https://images.unsplash.com/photo-1582722872445-44dc5f7e3c8f?q=80&w=400"
                "OCA" -> Res.string.product_egg_oca to "https://plus.unsplash.com/premium_photo-1675731320300-34907106d64d?q=80&w=400"
                else -> Res.string.product_egg_gallina to ""
            }
            val nombreTraducido = getString(nombreRes)
            val productos = jardineraRepository.getProductos().first()
            val existe = productos.find { it.nombre == nombreTraducido }

            if (existe != null) {
                jardineraRepository.insertarProducto(
                    existe.copy(
                        stock = existe.stock + cantidad,
                        imagenUrl = urlFoto
                    )
                )
            } else {
                jardineraRepository.insertarProducto(
                    ProductoEntity(
                        nombre = nombreTraducido,
                        categoria = "ANIMAL_PROD",
                        stock = cantidad,
                        imagenUrl = urlFoto
                    )
                )
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
            val pienso = productos.find {
                it.categoria == "PIENSO" || it.nombre.contains(
                    nombrePienso,
                    true
                )
            }

            if (pienso != null) {
                jardineraRepository.insertarProducto(
                    pienso.copy(
                        stock = (pienso.stock - cantidadSacos).coerceAtLeast(
                            0.0
                        )
                    )
                )
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

    // ALTAS Y BAJAS CON REGISTRO EN DIARIO
    fun addAnimal(
        nombre: String,
        tipo: String,
        cercadoId: Long,
        esPonedora: Boolean,
        foto: ByteArray?
    ) {
        viewModelScope.launch {
            val ficha = getFichaPorNombre(tipo)
            animalRepository.insertAnimal(
                AnimalEntity(
                    nombre = nombre,
                    tipo = tipo,
                    cercadoId = cercadoId,
                    esPonedora = esPonedora,
                    raza = null,
                    fechaNacimiento = System.currentTimeMillis(),
                    fotoPerfil = foto,
                    compatibilidad = ficha?.compatibilidad ?: "General"
                )
            )

            // Registro de Alta en el diario
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

            // Registro de Baja en el diario
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

    fun editarCercado(cercado: CercadoEntity, nuevoNumero: Int, nuevoNombre: String) {
        viewModelScope.launch {
            animalRepository.updateCercado(cercado.copy(numero = nuevoNumero, nombre = nuevoNombre))
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
                    id = id,
                    cercadoId = cercadoId,
                    animalTipo = "",
                    tipoAccion = tipo,
                    descripcion = desc,
                    cantidad = cantidad.toDouble(),
                    fecha = fecha,
                    foto = foto
                )
            )
        }
    }
    suspend fun getEntradaDiarioAnimalById(id: Long): EntradaDiarioAnimalEntity? {
        return animalRepository.getDiarioAnimalPorId(id)
    }
}