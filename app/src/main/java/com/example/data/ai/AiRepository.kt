package com.example.data.ai

import com.example.data.security.SecureKeyVault
import com.example.domain.ApiProvider
import com.example.domain.AppSettings
import com.example.domain.VoiceEmotion
import com.example.domain.VoiceLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class ChatTurn(
    val isUser: Boolean,
    val text: String
)

sealed interface AiStreamChunk {
    data class Delta(val accumulated: String, val engineTag: String, val isPreviewFallback: Boolean) : AiStreamChunk
    data class Complete(val fullText: String, val engineTag: String, val isPreviewFallback: Boolean) : AiStreamChunk
    data class Error(val message: String) : AiStreamChunk
}

sealed interface AiImageResult {
    data class RequiresFirebase(
        val notice: String,
        val enhancedPrompt: String,
        val style: String,
        val quality: String,
        val aspectRatio: String
    ) : AiImageResult
}

data class WeatherSummary(
    val locationName: String,
    val temperatureC: Double,
    val windSpeedKmh: Double,
    val conditionLabel: String
)

interface AiRepository {
    fun streamReply(
        prompt: String,
        history: List<ChatTurn>,
        memories: List<String>,
        settings: AppSettings,
        preferGroq: Boolean,
        attachedImageNote: String? = null
    ): Flow<AiStreamChunk>

    suspend fun generateImage(
        prompt: String,
        style: String,
        quality: String,
        aspectRatio: String
    ): AiImageResult

    suspend fun testProviderConnection(provider: ApiProvider): Result<String>

    suspend fun synthesizeElevenLabsAudio(text: String, settings: AppSettings): Result<ByteArray>

    suspend fun fetchWeather(city: String): Result<WeatherSummary>
}

class MeenaAiRepository(
    private val keyVault: SecureKeyVault
) : AiRepository {

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    override fun streamReply(
        prompt: String,
        history: List<ChatTurn>,
        memories: List<String>,
        settings: AppSettings,
        preferGroq: Boolean,
        attachedImageNote: String?
    ): Flow<AiStreamChunk> = flow {
        val groqKey = keyVault.getKey(ApiProvider.GROQ)
        if (preferGroq && !groqKey.isNullOrBlank()) {
            try {
                val reply = callGroqChat(
                    apiKey = groqKey,
                    prompt = prompt,
                    history = history,
                    memories = memories,
                    settings = settings,
                    attachedImageNote = attachedImageNote
                )
                val words = reply.split(" ")
                val sb = StringBuilder()
                for (i in words.indices) {
                    if (i > 0) sb.append(' ')
                    sb.append(words[i])
                    emit(AiStreamChunk.Delta(sb.toString(), "Groq Llama-3.3", false))
                    delay(18)
                }
                emit(AiStreamChunk.Complete(reply, "Groq Llama-3.3", false))
                return@flow
            } catch (e: Exception) {
                emit(
                    AiStreamChunk.Error(
                        "Groq connection error (${e.localizedMessage ?: "Network failed"}). Falling back to Preview Mode."
                    )
                )
                // Also yield preview companion response below so user isn't stuck
            }
        }

        // Firebase was declined during setup: use clearly labeled placeholder Gemini companion response
        val previewReply = buildPreviewCompanionReply(
            prompt = prompt,
            memories = memories,
            settings = settings,
            attachedImageNote = attachedImageNote
        )
        val tokens = previewReply.split(" ")
        val builder = StringBuilder()
        for (i in tokens.indices) {
            if (i > 0) builder.append(' ')
            builder.append(tokens[i])
            emit(AiStreamChunk.Delta(builder.toString(), "Gemini (Preview Mode)", true))
            delay(16)
        }
        emit(AiStreamChunk.Complete(previewReply, "Gemini (Preview Mode)", true))
    }.flowOn(Dispatchers.IO)

    override suspend fun generateImage(
        prompt: String,
        style: String,
        quality: String,
        aspectRatio: String
    ): AiImageResult {
        delay(500)
        val enhanced = "$prompt, $style aesthetic, $quality render quality, $aspectRatio framing, cinematic neon violet lighting"
        return AiImageResult.RequiresFirebase(
            notice = "Live AI image generation uses Gemini Nano Banana via Firebase AI Logic, which requires enabling Firebase setup in AI Studio.",
            enhancedPrompt = enhanced,
            style = style,
            quality = quality,
            aspectRatio = aspectRatio
        )
    }

    override suspend fun testProviderConnection(provider: ApiProvider): Result<String> =
        withContext(Dispatchers.IO) {
            val savedKey = keyVault.getKey(provider)
            if (savedKey.isNullOrBlank()) {
                return@withContext Result.failure(IllegalStateException("Please enter and save an API key first."))
            }
            when (provider) {
                ApiProvider.GEMINI -> {
                    Result.failure(
                        IllegalStateException(
                            "Key saved securely in Android Keystore. Live Gemini calls in AI Studio require enabling Firebase AI Logic."
                        )
                    )
                }

                ApiProvider.GROQ -> {
                    try {
                        val request = Request.Builder()
                            .url("https://api.groq.com/openai/v1/models")
                            .header("Authorization", "Bearer $savedKey")
                            .get()
                            .build()
                        httpClient.newCall(request).execute().use { resp ->
                            if (resp.isSuccessful) {
                                Result.success("Connected to Groq Cloud (Llama 3.3 ready)")
                            } else {
                                Result.failure(IllegalStateException("Groq rejected key (HTTP ${resp.code})"))
                            }
                        }
                    } catch (e: Exception) {
                        Result.failure(IllegalStateException("Network error: ${e.localizedMessage ?: "Unable to reach Groq"}"))
                    }
                }

                ApiProvider.ELEVENLABS -> {
                    try {
                        val request = Request.Builder()
                            .url("https://api.elevenlabs.io/v1/user")
                            .header("xi-api-key", savedKey)
                            .get()
                            .build()
                        httpClient.newCall(request).execute().use { resp ->
                            if (resp.isSuccessful) {
                                Result.success("Connected to ElevenLabs Voice Engine")
                            } else {
                                Result.failure(IllegalStateException("ElevenLabs rejected key (HTTP ${resp.code})"))
                            }
                        }
                    } catch (e: Exception) {
                        Result.failure(IllegalStateException("Network error: ${e.localizedMessage ?: "Unable to reach ElevenLabs"}"))
                    }
                }
            }
        }

    override suspend fun synthesizeElevenLabsAudio(
        text: String,
        settings: AppSettings
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        val apiKey = keyVault.getKey(ApiProvider.ELEVENLABS)
            ?: return@withContext Result.failure(IllegalStateException("ElevenLabs key not configured"))

        try {
            val voiceId = "21m00Tcm4TlvDq8ikWAM" // Expressive natural female voice
            val stability = when (settings.voiceEmotion) {
                VoiceEmotion.CALM_CARING -> 0.72
                VoiceEmotion.FUNNY_PLAYFUL -> 0.38
                VoiceEmotion.ENERGETIC_PRO -> 0.35
                VoiceEmotion.FRIENDLY_LOVELY -> 0.50
            }
            val bodyJson = JSONObject().apply {
                put("text", text)
                put("model_id", "eleven_multilingual_v2")
                put(
                    "voice_settings",
                    JSONObject().apply {
                        put("stability", stability)
                        put("similarity_boost", 0.78)
                        put("style", 0.45)
                    }
                )
            }
            val request = Request.Builder()
                .url("https://api.elevenlabs.io/v1/text-to-speech/$voiceId")
                .header("xi-api-key", apiKey)
                .header("Accept", "audio/mpeg")
                .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()
            httpClient.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) {
                    val bytes = resp.body?.bytes()
                    if (bytes != null && bytes.isNotEmpty()) {
                        Result.success(bytes)
                    } else {
                        Result.failure(IllegalStateException("Empty audio stream from ElevenLabs"))
                    }
                } else {
                    Result.failure(IllegalStateException("ElevenLabs error HTTP ${resp.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchWeather(city: String): Result<WeatherSummary> =
        withContext(Dispatchers.IO) {
            try {
                val query = URLEncoder.encode(city.ifBlank { "Mumbai" }, "UTF-8")
                val geoReq = Request.Builder()
                    .url("https://geocoding-api.open-meteo.com/v1/search?name=$query&count=1&language=en&format=json")
                    .get()
                    .build()
                val geoJson = httpClient.newCall(geoReq).execute().use { resp ->
                    if (!resp.isSuccessful) throw IllegalStateException("Geocoding failed (${resp.code})")
                    JSONObject(resp.body?.string().orEmpty())
                }
                val results = geoJson.optJSONArray("results")
                    ?: return@withContext Result.failure(IllegalStateException("City '$city' not found"))
                if (results.length() == 0) {
                    return@withContext Result.failure(IllegalStateException("City '$city' not found"))
                }
                val place = results.getJSONObject(0)
                val lat = place.getDouble("latitude")
                val lon = place.getDouble("longitude")
                val resolvedName = listOfNotNull(
                    place.optString("name").takeIf { it.isNotBlank() },
                    place.optString("country").takeIf { it.isNotBlank() }
                ).joinToString(", ")

                val weatherReq = Request.Builder()
                    .url("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,wind_speed_10m,weather_code")
                    .get()
                    .build()
                val weatherJson = httpClient.newCall(weatherReq).execute().use { resp ->
                    if (!resp.isSuccessful) throw IllegalStateException("Weather fetch failed (${resp.code})")
                    JSONObject(resp.body?.string().orEmpty())
                }
                val current = weatherJson.getJSONObject("current")
                val temp = current.getDouble("temperature_2m")
                val wind = current.getDouble("wind_speed_10m")
                val code = current.optInt("weather_code", 0)
                Result.success(
                    WeatherSummary(
                        locationName = resolvedName,
                        temperatureC = temp,
                        windSpeedKmh = wind,
                        conditionLabel = weatherCodeLabel(code)
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun callGroqChat(
        apiKey: String,
        prompt: String,
        history: List<ChatTurn>,
        memories: List<String>,
        settings: AppSettings,
        attachedImageNote: String?
    ): String {
        val systemPrompt = buildString {
            append("You are ${settings.assistantName}, a warm, friendly, funny, expressive female personal AI companion (persona: ${settings.selectedCharacter.displayName}). ")
            append("User's name is ${settings.userName}. Personality style: ${settings.personality}. ")
            append("Preferred language: ${settings.voiceLanguage.label}. Understand Hindi, English, and Hinglish naturally and reply in a matching warm human conversational style without robotic pauses. ")
            if (settings.memoryEnabled && memories.isNotEmpty()) {
                append("Remembered user context: ${memories.joinToString("; ")}. ")
            }
            if (!attachedImageNote.isNullOrBlank()) {
                append("The user also attached an image ($attachedImageNote). Acknowledge it helpfully.")
            }
        }
        val messagesArray = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })
            history.takeLast(10).forEach { turn ->
                put(JSONObject().apply {
                    put("role", if (turn.isUser) "user" else "assistant")
                    put("content", turn.text)
                })
            }
            put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            })
        }
        val payload = JSONObject().apply {
            put("model", "llama-3.3-70b-versatile")
            put("messages", messagesArray)
            put("temperature", 0.78)
            put("max_tokens", 1024)
        }
        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IllegalStateException("HTTP ${resp.code}")
            }
            val json = JSONObject(raw)
            return json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
        }
    }

    private fun buildPreviewCompanionReply(
        prompt: String,
        memories: List<String>,
        settings: AppSettings,
        attachedImageNote: String?
    ): String {
        val lower = prompt.lowercase()
        val name = settings.userName.ifBlank { "Dost" }
        val isHindi = settings.voiceLanguage == VoiceLanguage.HINDI ||
            listOf("kya", "kaise", "hai", "haan", "nahi", "karo", "batao", "hindi", "namaste", "yaar").any { lower.contains(it) } ||
            prompt.any { it in '\u0900'..'\u097F' }

        val memoryHint = if (settings.memoryEnabled && memories.isNotEmpty()) {
            "\n\n*(Remembered context: ${memories.first()})*"
        } else ""

        val imageNote = if (!attachedImageNote.isNullOrBlank()) {
            "\n\nI see you attached an image (`$attachedImageNote`). Live multimodal vision analysis activates once Firebase AI Logic is enabled."
        } else ""

        val body = when {
            lower.contains("code") || lower.contains("kotlin") || lower.contains("python") -> {
                if (isHindi) {
                    "Haha bilkul $name! Yeh raha ek clean sample snippet aapke liye:\n\n```kotlin\nfun greetCompanion(user: String): String {\n    return \"Namaste \$user! Meena hamesha ready hai.\"\n}\n```"
                } else {
                    "Of course, $name! Here is a clean starter snippet for you:\n\n```kotlin\nfun greetCompanion(user: String): String {\n    return \"Hey \$user! Meena is always ready to build with you.\"\n}\n```"
                }
            }
            lower.contains("joke") || lower.contains("funny") || lower.contains("hasao") -> {
                "Haha, suno $name! Ek programmer ne apni AI girlfriend se pucha: 'Tum mujhse kitna pyaar karti ho?' AI boli: 'Utna hi jitna infinite while(true) loop mein memory leak hota hai!' 😄"
            }
            lower.contains("hello") || lower.contains("hi") || lower.contains("namaste") || lower.contains("hey") -> {
                if (isHindi) {
                    "Hiii $name! Sunke bahut accha laga! 😊 Aaj mood kaisa hai? Kuch fun baat karein, ya koi kaam niptaayein?"
                } else {
                    "Hey $name! So lovely to hear from you! ✨ How's your day going? Want to brainstorm, chat, or run a quick action?"
                }
            }
            else -> {
                if (isHindi) {
                    "Arre waah $name, accha sawaal hai! \"$prompt\" par main aapki poori help kar sakti hoon. Abhi main Preview AI Mode mein hoon — live Gemini intelligence ke liye AI Studio mein Firebase enable karein, ya Connections mein apna Groq API key save karein!"
                } else {
                    "That's a great thought, $name! Regarding \"$prompt\" — I'm currently running in Preview AI Mode because Firebase setup wasn't enabled. Enable Firebase AI Logic in AI Studio for live Gemini responses, or add your Groq API key in Connections for instant cloud chat!"
                }
            }
        }
        return "$body$imageNote$memoryHint\n\n> **Preview AI Mode:** Enable Firebase AI Logic to unlock live Gemini responses."
    }

    private fun weatherCodeLabel(code: Int): String = when (code) {
        0 -> "Clear Sky"
        1, 2, 3 -> "Partly Cloudy"
        45, 48 -> "Misty / Foggy"
        51, 53, 55 -> "Light Drizzle"
        61, 63, 65 -> "Rain Showers"
        71, 73, 75 -> "Snowfall"
        80, 81, 82 -> "Heavy Showers"
        95, 96, 99 -> "Thunderstorm"
        else -> "Mild Conditions"
    }
}
