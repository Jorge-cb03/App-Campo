package com.example.proyecto.ui.garden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyecto.data.database.entity.*
import com.example.proyecto.data.repository.AuthRepository
import com.example.proyecto.data.repository.JardineraRepository
import com.example.proyecto.data.repository.PerenualSpecies
import com.example.proyecto.domain.model.ProductType
import com.example.proyecto.util.LocationProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.collections.firstOrNull
import kotlin.collections.plus
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(val text: String, val isUser: Boolean)

@Serializable
data class GeminiRequest(val contents: List<GeminiContent>)

@Serializable
data class GeminiContent(val parts: List<Part>, val role: String = "user")

@Serializable
data class Part(val text: String? = null)

@Serializable
data class GeminiResponse(
    val candidates: List<Candidate>? = null,
    val error: ApiError? = null
)

@Serializable
data class Candidate(
    val content: GeminiContent? = null,
    val finishReason: String? = null
)

@Serializable
data class ApiError(val code: Int, val message: String, val status: String)

data class WeatherState(
    val temperature: Double = 0.0,
    val weatherCode: Int = 0,
    val isDay: Int = 1,
    val isLoading: Boolean = true,
    // Campos nuevos — tienen valor por defecto para no romper nada
    val feelsLike: Double = 0.0,
    val humidity: Int = 0,
    val precipitation: Double = 0.0,
    val uvIndex: Double = 0.0
)

@Serializable
data class Content(
    val parts: List<Part>,
    val role: String = "user"
)
@Serializable
data class WeatherResponse(val current: CurrentWeather)

@Serializable
data class CurrentWeather(
    val temperature_2m: Double,
    val weather_code: Int,
    val is_day: Int,
    val apparent_temperature: Double = 0.0,
    val relative_humidity_2m: Int = 0,
    val precipitation: Double = 0.0,
    val uv_index: Double = 0.0
)

@Serializable
data class GeminiPart(val text: String)

class GardenViewModel(
    private val repository: JardineraRepository,
    private val authRepository: AuthRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {

    init {
        viewModelScope.launch {
            // Esto descarga los datos en segundo plano al abrir la app
            repository.syncProductos()
            repository.syncStructure() // Blindado
            repository.syncDiario()    // Recupera historial
            repository.syncAlerts()
            repository.limpiarAlertasAntiguas()
        }
    }

    val jardineras: StateFlow<List<JardineraEntity>> = repository.getJardineras()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historialGeneral: StateFlow<List<EntradaDiarioEntity>> = repository.getTodoElHistorial()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Estado del Usuario
    val usuarioActivo: StateFlow<UsuarioEntity?> = repository.getUsuarioActivo()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val alerts: StateFlow<List<AlertaEntity>> = repository.getAlerts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- FUNCIONES PARA DIARIO (Get y Delete) ---
    suspend fun getEntradaDiarioById(id: Long): EntradaDiarioEntity? {
        return repository.getEntradaDiarioById(id)
    }

    fun eliminarEntradaDiario(id: Long) {
        viewModelScope.launch {
            repository.eliminarEntradaDiario(id)
        }
    }

    fun guardarPerfil(nombre: String, email: String, foto: Any?) {
        viewModelScope.launch {
            repository.registrarUsuarioLocal(nombre, email, foto)
        }
    }

    // --- FUNCIÓN GUARDAR (SOPORTA EDICIÓN) ---
    fun guardarEntradaDiario(
        bancalId: Long,
        tipo: String,
        desc: String,
        fecha: Long,
        foto: ByteArray? = null,
        id: Long = 0L
    ) {
        viewModelScope.launch {
            repository.insertarEntradaDiario(
                EntradaDiarioEntity(
                    id = id,
                    bancalId = bancalId,
                    tipoAccion = tipo,
                    descripcion = desc,
                    fecha = fecha,
                    foto = foto
                )
            )
        }
    }

    // Guarda múltiples entradas de forma secuencial y avisa al terminar
    fun guardarMultiplesEntradas(
        bancalIds: List<Long>,
        tipo: String,
        desc: String,
        fecha: Long,
        foto: ByteArray?,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                bancalIds.forEach { id ->
                    repository.insertarEntradaDiario(
                        EntradaDiarioEntity(
                            id = 0L,
                            bancalId = id,
                            tipoAccion = tipo,
                            descripcion = desc,
                            fecha = fecha,
                            foto = foto
                        )
                    )
                }
                onComplete() // Callback para cerrar el diálogo en la UI de forma segura
            } catch (e: Exception) {
                println("Error en guardado múltiple: ${e.message}")
            }
        }
    }

    // --- FILTRO DE SEMILLAS PARA PLANTAR ---
    val semillasDisponibles: Flow<List<ProductoEntity>> = repository.getProductos().map { list ->
        list.filter { p ->
            val esSemilla = try { ProductType.valueOf(p.categoria) == ProductType.SEED } catch (e: Exception) { false }
            esSemilla && p.stock > 0
        }
    }

    // --- TIEMPO Y GEMINI ---
    private val _weatherState = MutableStateFlow(WeatherState())
    val weatherState = _weatherState.asStateFlow()

    private val _geminiChatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val geminiChatHistory = _geminiChatHistory.asStateFlow()

    private val _isGeminiLoading = MutableStateFlow(false)
    val isGeminiLoading = _isGeminiLoading.asStateFlow()

    // CLIENTE HTTP
    private val client = HttpClient {
        expectSuccess = false
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    init {
        fetchWeather()
        _geminiChatHistory.value = listOf(ChatMessage("¡Hola! Soy tu asistente de huerto. ¿En qué puedo ayudarte hoy?", false))
    }

    private fun fetchWeather() {
        viewModelScope.launch {
            try {
                // 1. Obtenemos la ubicación (es suspendida, debe ir dentro de la corrutina)
                val location = locationProvider.getCurrentLocation()

                // 2. Accedemos a las coordenadas.
                // IMPORTANTE: .first y .second son PROPIEDADES, no funciones (sin paréntesis)
                val lat = location?.first ?: 40.4168
                val lon = location?.second ?: -3.7038

                val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                        "&current=temperature_2m,weather_code,is_day,apparent_temperature,relative_humidity_2m,precipitation,uv_index"

                // Asegúrate de que 'client' también esté definido en tu clase (HttpClient)
                val response: WeatherResponse = client.get(url).body()

                _weatherState.value = WeatherState(
                    temperature   = response.current.temperature_2m,
                    weatherCode   = response.current.weather_code,
                    isDay         = response.current.is_day,
                    feelsLike     = response.current.apparent_temperature,
                    humidity      = response.current.relative_humidity_2m,
                    precipitation = response.current.precipitation,
                    uvIndex       = response.current.uv_index,
                    isLoading     = false
                )
            } catch (e: Exception) {
                e.printStackTrace()
                // En caso de error, quitamos el loading
                _weatherState.value = _weatherState.value.copy(isLoading = false)
            }
        }
    }

    private val GEMINI_API_KEY get() = com.example.proyecto.GEMINI_API_KEY

    fun sendMessageToGemini(prompt: String) {
        _isGeminiLoading.value = true
        _geminiChatHistory.value += ChatMessage(prompt, true)

        viewModelScope.launch {
            try {
                val apiKey = GEMINI_API_KEY // Tu clave API aquí

                // 1. Limpiamos el texto por si tu padre pone comillas o saltos de línea
                val promptSeguro = prompt.replace("\"", "\\\"").replace("\n", " ")

                // 2. CONSTRUIMOS EL JSON A MANO (A prueba de fallos de Ktor)
                val jsonBodyString = """
                    {
                      "contents": [
                        {
                          "parts": [
                            {"text": "$promptSeguro"}
                          ]
                        }
                      ]
                    }
                """.trimIndent()

                // 3. Usamos el modelo 1.5-flash-latest que es el estable y gratuito
                // 3. Usamos el modelo 1.5-flash-latest que es el estable y gratuito
                val response = client.post("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key=$apiKey") {
                    // Usamos exactamente las funciones que ya te compilaban antes
                    contentType(io.ktor.http.ContentType.Application.Json)
                    setBody(jsonBodyString)
                }

                val responseBodyString = response.bodyAsText()

                val geminiResponse = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }.decodeFromString<GeminiResponse>(responseBodyString)

                if (geminiResponse.error != null) {
                    _geminiChatHistory.value += ChatMessage("⚠️ Google dice: ${geminiResponse.error.message}", false)
                } else if (geminiResponse.candidates != null && geminiResponse.candidates.isNotEmpty()) {
                    val candidate = geminiResponse.candidates[0]
                    val responseText = candidate.content?.parts?.get(0)?.text ?: "Respuesta en blanco."
                    _geminiChatHistory.value += ChatMessage(responseText, false)
                } else {
                    _geminiChatHistory.value += ChatMessage("⚠️ La IA no devolvió nada.", false)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _geminiChatHistory.value += ChatMessage("📡 Fallo local: ${e.message}", false)
            } finally {
                _isGeminiLoading.value = false
            }
        }
    }

    // --- COSECHA INTELIGENTE ---
    fun cosecharConCantidad(bancal: BancalEntity, cantidad: Double) {
        viewModelScope.launch {
            val nombre = bancal.nombreCultivo ?: "Cosecha"

            // 1. Primero actualizamos el stock del producto
            val productos = repository.getProductos().first()
            val prodExistente = productos.find { it.nombre.equals(nombre, true) && it.categoria != ProductType.SEED.name }

            if (prodExistente != null) {
                repository.insertarProducto(prodExistente.copy(
                    stock = prodExistente.stock + cantidad
                ))
            } else {
                repository.insertarProducto(ProductoEntity(
                    nombre = nombre,
                    categoria = ProductType.VEGETABLE.name,
                    stock = cantidad,
                    imagenUrl = bancal.imagenUrl,
                    perenualId = bancal.perenualId
                ))
            }

            // 2. Registramos la cosecha en el diario ANTES de limpiar el bancal
            guardarEntradaDiario(bancal.id, "COSECHA", "Recolectado: $cantidad de $nombre", System.currentTimeMillis())

            // 3. Por último, vaciamos el bancal
            repository.cosecharBancal(bancal.id)
        }
    }

    fun eliminarPlanta(bancalId: Long) {
        viewModelScope.launch {
            repository.cosecharBancal(bancalId)
            guardarEntradaDiario(bancalId, "ELIMINACION", "Planta eliminada sin producción", System.currentTimeMillis())
        }
    }

    // --- ALERTAS ---
    fun addAlert(title: String, desc: String, epochMillis: Long) = viewModelScope.launch {
        repository.insertAlert(AlertaEntity(title = title, description = desc, dateTimeEpochMillis = epochMillis))
    }

    fun updateAlert(id: Long, title: String, desc: String, epochMillis: Long) = viewModelScope.launch {
        alerts.value.find { it.id == id }?.let { existing ->
            repository.updateAlert(existing.copy(title = title, description = desc, dateTimeEpochMillis = epochMillis))
        }
    }

    fun deleteAlert(id: Long) = viewModelScope.launch {
        alerts.value.find { it.id == id }?.let { existing ->
            repository.deleteAlert(existing)
        }
    }

    val productosFertilizante: Flow<List<ProductoEntity>> = repository.getProductos().map { list -> list.filter { it.categoria == ProductType.FERTILIZER.name } }
    val productosQuimicos: Flow<List<ProductoEntity>> = repository.getProductos().map { list -> list.filter { it.categoria == ProductType.CHEMICAL.name } }

    private val _apiSearchResults = MutableStateFlow<List<PerenualSpecies>>(emptyList())
    val apiSearchResults = _apiSearchResults.asStateFlow()

    fun buscarCultivoApi(query: String) { viewModelScope.launch { _apiSearchResults.value = repository.buscarCultivosOnline(query) } }
    fun limpiarResultadosBusqueda() { _apiSearchResults.value = emptyList() }

    fun crearNuevaJardinera(n: String, f: Int, c: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (repository.existeJardinera(n)) {
                onResult(false)
            } else {
                repository.crearJardineraConBancales(n, f, c)
                onResult(true)
            }
        }
    }

    fun archivar(j: JardineraEntity) = viewModelScope.launch { repository.archivarJardinera(j) }
    fun desarchivar(j: JardineraEntity) = viewModelScope.launch { repository.desarchivarJardinera(j) }
    fun actualizarJardinera(j: JardineraEntity, n: String, f: Int, c: Int) = viewModelScope.launch { repository.actualizarJardinera(j.copy(nombre = n, filas = f, columnas = c)) }
    fun regarJardineraCompleta(jId: Long) = viewModelScope.launch { repository.regarTodaLaJardinera(jId) }
    fun getBancales(id: Long) = repository.getBancales(id)
    suspend fun getBancalById(id: Long) = repository.getBancalById(id)
    fun toggleBancal(id: Long, f: Boolean) = viewModelScope.launch { repository.setEstadoFuncionalBancal(id, f) }
    fun plantar(bancalId: Long, productoId: Long) {
        viewModelScope.launch {
            repository.plantarEnBancal(bancalId, productoId) // Long directo, sin toInt()
        }
    }
    fun cosechar(bId: Long) = viewModelScope.launch { repository.cosecharBancal(bId) }
    fun registrarRiego(bId: Long, litros: Double) = viewModelScope.launch { repository.registrarRiego(bId, litros) }
    fun aplicarTratamiento(bId: Long, p: ProductoEntity, cant: Double, tipo: String) = viewModelScope.launch { repository.registrarTratamiento(bId, p.id, cant, tipo) }
    fun getProductos() = repository.getProductos()
    suspend fun getProductoById(id: Long) = repository.getProductoById(id)
    fun eliminarProducto(id: Long) = viewModelScope.launch { repository.eliminarProducto(id) }
    fun updateStock(id: Long, n: Double) = viewModelScope.launch { repository.getProductoById(id)?.let { repository.insertarProducto(it.copy(stock = n)) } }
    fun guardarProducto(id: Long, n: String, c: String, s: Double, perenualId: Int? = null, imagenUrl: String? = null, nombreCientifico: String? = null, notas: String? = null) {
        viewModelScope.launch { repository.insertarProducto(ProductoEntity(id = id, nombre = n, categoria = c, stock = s, perenualId = perenualId, imagenUrl = imagenUrl, nombreCientifico = nombreCientifico, notasCultivo = notas)) }
    }
    fun getHistorial(id: Long) = repository.getHistorialBancal(id)

    fun getInfoExtendida(perenualId: Int?, nombre: String? = null): JardineraRepository.FichaCultivo? {
        if (perenualId != null) {
            val porId = repository.getFichaCompleta(perenualId)
            if (porId != null) return porId
        }
        if (!nombre.isNullOrBlank()) {
            return repository.getFichaPorNombre(nombre)
        }
        return null
    }

    fun toggleFavorito(jardinera: JardineraEntity) = viewModelScope.launch {
        repository.actualizarJardinera(jardinera.copy(esFavorita = !jardinera.esFavorita))
    }

    fun eliminarJardineraDefinitivamente(id: Long) {
        viewModelScope.launch {
            repository.eliminarJardineraCompleta(id)
        }
    }

    // --- GESTIÓN DE AUTENTICACIÓN Y PERFIL ---

    fun cerrarSesion(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout() // Cierra Firebase
            repository.limpiarDatosUsuario() // Borra rastro local
            onComplete()
        }
    }
    fun entrarComoInvitado(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                authRepository.signInAnonymously()
                // Guardamos el perfil local de invitado
                repository.guardarUsuario("Invitado", "invitado@huertomanager.com", null)
                onSuccess()
            } catch (e: Exception) {
                println("Error login anónimo: ${e.message}")
            }
        }
    }
}