package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.data.remote.ContentDto
import com.example.data.remote.GeminiApiService
import com.example.data.remote.GeminiErrorWrapper
import com.example.data.remote.GenerateContentRequest
import com.example.data.remote.GenerationConfigDto
import com.example.data.remote.PartDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

data class ChatMessage(
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

interface GeminiRepository {
    fun isApiKeyConfigured(): Boolean

    suspend fun generateContent(
        prompt: String,
        systemInstruction: String? = null,
        model: String = DEFAULT_MODEL
    ): Result<String>

    suspend fun generateChat(
        history: List<ChatMessage>,
        systemInstruction: String? = null,
        model: String = DEFAULT_MODEL
    ): Result<String>

    companion object {
        const val DEFAULT_MODEL = "gemini-3.8-flash"
    }
}

class NetworkGeminiRepository(
    private val apiService: GeminiApiService = GeminiApiService.create(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val apiKeyProvider: () -> String = { BuildConfig.GEMINI_API_KEY }
) : GeminiRepository {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    override fun isApiKeyConfigured(): Boolean {
        val key = apiKeyProvider().trim()
        return key.isNotEmpty() && key != PLACEHOLDER_KEY
    }

    override suspend fun generateContent(
        prompt: String,
        systemInstruction: String?,
        model: String
    ): Result<String> {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isEmpty()) {
            return Result.failure(IllegalArgumentException("Prompt cannot be empty."))
        }
        return generateChat(
            history = listOf(ChatMessage(isUser = true, text = cleanPrompt)),
            systemInstruction = systemInstruction,
            model = model
        )
    }

    override suspend fun generateChat(
        history: List<ChatMessage>,
        systemInstruction: String?,
        model: String
    ): Result<String> {
        val apiKey = apiKeyProvider().trim()
        if (apiKey.isEmpty() || apiKey == PLACEHOLDER_KEY) {
            return Result.failure(
                IllegalStateException(
                    "GEMINI_API_KEY is not configured. Add your key in the AI Studio Secrets panel."
                )
            )
        }

        val contents = history
            .filter { it.text.isNotBlank() }
            .map { msg ->
                ContentDto(
                    role = if (msg.isUser) "user" else "model",
                    parts = listOf(PartDto(text = msg.text))
                )
            }

        if (contents.isEmpty()) {
            return Result.failure(IllegalArgumentException("Conversation cannot be empty."))
        }

        val systemContent = systemInstruction
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { ContentDto(parts = listOf(PartDto(text = it))) }

        val request = GenerateContentRequest(
            contents = contents,
            systemInstruction = systemContent,
            generationConfig = GenerationConfigDto(temperature = 0.75, maxOutputTokens = 2048)
        )

        return try {
            withTimeout(REQUEST_TIMEOUT_MS) {
                withContext(ioDispatcher) {
                    val response = apiService.generateContent(
                        model = model,
                        apiKey = apiKey,
                        request = request
                    )
                    if (response.isSuccessful) {
                        val text = response.body()
                            ?.candidates
                            ?.firstOrNull()
                            ?.content
                            ?.parts
                            ?.mapNotNull { it.text }
                            ?.joinToString("")
                            ?.trim()
                        if (!text.isNullOrEmpty()) {
                            Result.success(text)
                        } else {
                            Result.failure(IllegalStateException("Gemini returned an empty response."))
                        }
                    } else {
                        val rawError = response.errorBody()?.string()
                        val parsedMessage = parseErrorMessage(rawError)
                            ?: "Gemini API error (HTTP ${response.code()})"
                        safeLogError("Gemini request failed: HTTP ${response.code()} - $parsedMessage")
                        Result.failure(IllegalStateException(parsedMessage))
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            safeLogError("Gemini API request timed out", e)
            Result.failure(IllegalStateException("Request timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            safeLogError("Gemini network request error", e)
            Result.failure(
                IllegalStateException(e.localizedMessage ?: "Network request to Gemini API failed.")
            )
        }
    }

    private fun safeLogError(message: String, throwable: Throwable? = null) {
        runCatching {
            if (throwable != null) Log.e(TAG, message, throwable) else Log.e(TAG, message)
        }
    }

    private fun parseErrorMessage(rawBody: String?): String? {
        if (rawBody.isNullOrBlank()) return null
        return try {
            val adapter = moshi.adapter(GeminiErrorWrapper::class.java)
            adapter.fromJson(rawBody)?.error?.message
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val TAG = "GeminiRepository"
        private const val PLACEHOLDER_KEY = "MY_GEMINI_API_KEY"
        private const val REQUEST_TIMEOUT_MS = 60_000L
    }
}
