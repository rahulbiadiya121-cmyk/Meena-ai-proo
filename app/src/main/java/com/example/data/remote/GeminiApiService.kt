package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class PartDto(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class ContentDto(
    @Json(name = "role") val role: String? = null,
    @Json(name = "parts") val parts: List<PartDto>
)

@JsonClass(generateAdapter = true)
data class GenerationConfigDto(
    @Json(name = "temperature") val temperature: Double = 0.7,
    @Json(name = "maxOutputTokens") val maxOutputTokens: Int = 2048
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<ContentDto>,
    @Json(name = "systemInstruction") val systemInstruction: ContentDto? = null,
    @Json(name = "generationConfig") val generationConfig: GenerationConfigDto? = null
)

@JsonClass(generateAdapter = true)
data class CandidateDto(
    @Json(name = "content") val content: ContentDto? = null,
    @Json(name = "finishReason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiErrorDetail(
    @Json(name = "code") val code: Int? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "status") val status: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiErrorWrapper(
    @Json(name = "error") val error: GeminiErrorDetail? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<CandidateDto>? = null,
    @Json(name = "error") val error: GeminiErrorDetail? = null
)

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GenerateContentRequest
    ): Response<GenerateContentResponse>

    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com/"

        fun create(): GeminiApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
                redactHeader("x-goog-api-key")
            }
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(GeminiApiService::class.java)
        }
    }
}
