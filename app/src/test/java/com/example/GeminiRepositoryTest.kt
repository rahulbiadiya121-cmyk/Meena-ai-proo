package com.example

import com.example.data.remote.CandidateDto
import com.example.data.remote.ContentDto
import com.example.data.remote.GeminiApiService
import com.example.data.remote.GenerateContentRequest
import com.example.data.remote.GenerateContentResponse
import com.example.data.remote.PartDto
import com.example.data.repository.NetworkGeminiRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class GeminiRepositoryTest {

    @Test
    fun `returns failure when api key is placeholder or blank`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val fakeService = object : GeminiApiService {
            override suspend fun generateContent(
                model: String,
                apiKey: String,
                request: GenerateContentRequest
            ): Response<GenerateContentResponse> {
                throw AssertionError("Network should not be called when API key is missing")
            }
        }

        val repoBlank = NetworkGeminiRepository(fakeService, dispatcher) { "" }
        assertFalse(repoBlank.isApiKeyConfigured())
        assertTrue(repoBlank.generateContent("Hello").isFailure)

        val repoPlaceholder = NetworkGeminiRepository(fakeService, dispatcher) { "MY_GEMINI_API_KEY" }
        assertFalse(repoPlaceholder.isApiKeyConfigured())
        assertTrue(repoPlaceholder.generateContent("Hello").isFailure)
    }

    @Test
    fun `returns model text when response succeeds`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        var capturedKey = ""
        val fakeService = object : GeminiApiService {
            override suspend fun generateContent(
                model: String,
                apiKey: String,
                request: GenerateContentRequest
            ): Response<GenerateContentResponse> {
                capturedKey = apiKey
                return Response.success(
                    GenerateContentResponse(
                        candidates = listOf(
                            CandidateDto(
                                content = ContentDto(
                                    role = "model",
                                    parts = listOf(PartDto(text = "Hello from Gemini!"))
                                )
                            )
                        )
                    )
                )
            }
        }

        val repo = NetworkGeminiRepository(fakeService, dispatcher) { "test-key-123" }
        assertTrue(repo.isApiKeyConfigured())

        val result = repo.generateContent("Hi")
        assertTrue(result.isSuccess)
        assertEquals("Hello from Gemini!", result.getOrNull())
        assertEquals("test-key-123", capturedKey)
    }

    @Test
    fun `parses API error body cleanly on failure`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val fakeService = object : GeminiApiService {
            override suspend fun generateContent(
                model: String,
                apiKey: String,
                request: GenerateContentRequest
            ): Response<GenerateContentResponse> {
                val errorJson = """{"error":{"code":401,"message":"API key not valid."}}"""
                return Response.error(
                    401,
                    errorJson.toResponseBody("application/json".toMediaType())
                )
            }
        }

        val repo = NetworkGeminiRepository(fakeService, dispatcher) { "bad-key" }
        val result = repo.generateContent("Hi")
        assertTrue(result.isFailure)
        assertEquals("API key not valid.", result.exceptionOrNull()?.message)
    }
}
