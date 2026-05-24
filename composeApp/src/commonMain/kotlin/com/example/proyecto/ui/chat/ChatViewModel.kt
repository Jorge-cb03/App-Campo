package com.example.proyecto.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val GEMINI_API_KEY = "AIzaSyAXiVzLEXuMcvxxnrF-EevSw_FHLFcASPI"

private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"

// ── DTOs petición ────────────────────────────────────────────────────────────
@Serializable data class GeminiRequest(val contents: List<GeminiContent>)
@Serializable data class GeminiContent(val role: String, val parts: List<GeminiPart>)
@Serializable data class GeminiPart(val text: String)

// ── DTOs respuesta generateContent ───────────────────────────────────────────
@Serializable data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null,
    val error: GeminiError? = null
)
@Serializable data class GeminiCandidate(val content: GeminiContent)
@Serializable data class GeminiError(val message: String, val code: Int = 0)

// ── DTOs listModels ──────────────────────────────────────────────────────────
@Serializable data class ModelsResponse(val models: List<ModelInfo>? = null)
@Serializable data class ModelInfo(
    val name: String,                          // "models/gemini-1.5-flash"
    val supportedGenerationMethods: List<String> = emptyList()
)

class ChatViewModel : ViewModel() {

    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val conversationHistory = mutableListOf<GeminiContent>()
    private var detectedModel: String? = null   // se rellena al iniciar
    private var systemPromptSent = false

    private val systemPrompt =
        "Eres un asistente experto en jardinería y huertos. " +
                "Ayuda con consejos sobre plantas, riego, plagas y cuidado del huerto. " +
                "Responde siempre en el mismo idioma que use el usuario. " +
                "Sé breve, amigable y práctico."

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val httpClient = HttpClient {
        install(ContentNegotiation) { json(json) }
    }

    init {
        _chatHistory.value = listOf(
            ChatMessage("¡Hola! Soy tu asistente de huerto. ¿En qué puedo ayudarte hoy?", isUser = false)
        )
        // Detectar modelo disponible al arrancar (silencioso para el usuario)
        viewModelScope.launch { detectAvailableModel() }
    }

    // ── Llama a /v1beta/models y elige el primer modelo que soporte generateContent
    private suspend fun detectAvailableModel() {
        try {
            val raw = httpClient
                .get("$BASE_URL/models?key=$GEMINI_API_KEY")
                .bodyAsText()

            val parsed = json.decodeFromString<ModelsResponse>(raw)

            // Preferencia: flash > pro > cualquiera que soporte generateContent
            val candidates = parsed.models
                ?.filter { it.supportedGenerationMethods.contains("generateContent") }
                ?: emptyList()

            detectedModel = candidates
                .firstOrNull { it.name.contains("flash") }?.name
                ?: candidates.firstOrNull { it.name.contains("pro") }?.name
                        ?: candidates.firstOrNull()?.name

            if (detectedModel == null) {
                _chatHistory.value = _chatHistory.value + ChatMessage(
                    "⚠️ Tu clave no tiene acceso a ningún modelo de Gemini.\n\n" +
                            "Genera una nueva en: aistudio.google.com/app/apikey\n" +
                            "Pulsa Create API key → copia la clave → pégala en ChatViewModel.kt",
                    isUser = false
                )
            }
        } catch (e: Exception) {
            // Si no puede listar modelos, intentará con gemini-1.5-flash al enviar
            detectedModel = "models/gemini-1.5-flash"
        }
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank() || _isLoading.value) return

        _chatHistory.value = _chatHistory.value + ChatMessage(userText, isUser = true)

        val textToSend = if (!systemPromptSent) {
            systemPromptSent = true
            "$systemPrompt\n\nUsuario: $userText"
        } else {
            userText
        }

        conversationHistory.add(GeminiContent("user", listOf(GeminiPart(textToSend))))
        _isLoading.value = true

        viewModelScope.launch {
            // Si aún no tenemos modelo, esperamos la detección
            if (detectedModel == null) detectAvailableModel()

            val model = detectedModel
            if (model == null) {
                _isLoading.value = false
                return@launch
            }

            // El nombre viene como "models/gemini-1.5-flash", la URL necesita solo esa parte
            val url = "$BASE_URL/$model:generateContent?key=$GEMINI_API_KEY"

            try {
                val httpResponse: HttpResponse = httpClient.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(GeminiRequest(conversationHistory.toList()))
                }

                val rawBody = httpResponse.bodyAsText()

                if (httpResponse.status.value != 200) {
                    conversationHistory.removeLast()
                    _chatHistory.value = _chatHistory.value + ChatMessage(
                        "Error ${httpResponse.status.value}: $rawBody", isUser = false
                    )
                    return@launch
                }

                val response = json.decodeFromString<GeminiResponse>(rawBody)
                val replyText = response.candidates
                    ?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "Sin respuesta. Inténtalo de nuevo."

                conversationHistory.add(GeminiContent("model", listOf(GeminiPart(replyText))))
                _chatHistory.value = _chatHistory.value + ChatMessage(replyText, isUser = false)

            } catch (e: Exception) {
                conversationHistory.removeLast()
                _chatHistory.value = _chatHistory.value + ChatMessage(
                    when {
                        e.message?.contains("401") == true ->
                            "❌ API Key inválida."
                        e.message?.contains("UnknownHost") == true ->
                            "📡 Sin conexión a internet."
                        else -> "Error: ${e.message}"
                    },
                    isUser = false
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        httpClient.close()
    }
}