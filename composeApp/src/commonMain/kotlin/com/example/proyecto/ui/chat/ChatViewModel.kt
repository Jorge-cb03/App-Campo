package com.example.proyecto.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyecto.GEMINI_API_KEY
import io.ktor.client.*
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

private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"

@Serializable data class GeminiRequest(val contents: List<GeminiContent>)
@Serializable data class GeminiContent(val role: String, val parts: List<GeminiPart>)
@Serializable data class GeminiPart(val text: String)

@Serializable data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null,
    val error: GeminiError? = null
)
@Serializable data class GeminiCandidate(val content: GeminiContent)
@Serializable data class GeminiError(val message: String, val code: Int = 0)

@Serializable data class ModelsResponse(val models: List<ModelInfo>? = null)
@Serializable data class ModelInfo(
    val name: String,
    val supportedGenerationMethods: List<String> = emptyList()
)

class ChatViewModel : ViewModel() {

    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val conversationHistory = mutableListOf<GeminiContent>()
    private var detectedModel: String? = null
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
        viewModelScope.launch { detectAvailableModel() }
    }

    private suspend fun detectAvailableModel() {
        try {
            val raw = httpClient
                .get("$BASE_URL/models?key=$GEMINI_API_KEY")
                .bodyAsText()
            val parsed = json.decodeFromString<ModelsResponse>(raw)
            val candidates = parsed.models
                ?.filter { it.supportedGenerationMethods.contains("generateContent") }
                ?: emptyList()
            detectedModel = candidates.firstOrNull { it.name.contains("flash") }?.name
                ?: candidates.firstOrNull { it.name.contains("pro") }?.name
                        ?: candidates.firstOrNull()?.name

            if (detectedModel == null) {
                _chatHistory.value = _chatHistory.value + ChatMessage(
                    "⚠️ Sin acceso a modelos Gemini. Revisa la API key en aistudio.google.com",
                    isUser = false
                )
            }
        } catch (e: Exception) {
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
            if (detectedModel == null) detectAvailableModel()
            val model = detectedModel ?: run { _isLoading.value = false; return@launch }
            val url = "$BASE_URL/$model:generateContent?key=$GEMINI_API_KEY"

            try {
                val httpResponse: io.ktor.client.statement.HttpResponse = httpClient.post(url) {
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
                        e.message?.contains("401") == true -> "❌ API Key inválida o revocada."
                        e.message?.contains("UnknownHost") == true -> "📡 Sin conexión a internet."
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