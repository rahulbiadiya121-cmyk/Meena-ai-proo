package com.example.ui.state

import com.example.data.ai.WeatherSummary
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ConversationEntity
import com.example.data.local.GeneratedImageEntity
import com.example.data.local.MemoryEntity
import com.example.domain.ApiProvider
import com.example.domain.AppSettings
import com.example.domain.InstalledAppInfo
import com.example.domain.OrbState
import com.example.domain.ProviderConnectionUi
import com.example.domain.QuickActionConfig
import com.example.domain.VoicePipelineStage
import com.example.system.VoiceCommandIntent

sealed interface ImageGenUiState {
    data object Idle : ImageGenUiState
    data object Loading : ImageGenUiState
    data class PreviewNotice(
        val notice: String,
        val enhancedPrompt: String,
        val style: String,
        val quality: String,
        val aspectRatio: String
    ) : ImageGenUiState
    data class Error(val message: String) : ImageGenUiState
}

sealed interface WriteUiState {
    data object Idle : WriteUiState
    data object Loading : WriteUiState
    data class Streaming(val text: String, val engineTag: String, val isPreviewFallback: Boolean) : WriteUiState
    data class Success(val text: String, val engineTag: String, val isPreviewFallback: Boolean) : WriteUiState
    data class Error(val message: String) : WriteUiState
}

sealed interface WeatherUiState {
    data object Idle : WeatherUiState
    data object Loading : WeatherUiState
    data class Success(val summary: WeatherSummary) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}

data class VoiceUiState(
    val stage: VoicePipelineStage = VoicePipelineStage.IDLE,
    val orbState: OrbState = OrbState.IDLE,
    val isListening: Boolean = false,
    val liveTranscript: String = "",
    val detectedLanguage: String = "Hinglish",
    val detectedEmotion: String = "Warm & Friendly",
    val assistantReply: String = "",
    val isPreviewNotice: Boolean = false,
    val statusBanner: String = "Tap the orb or mic to start talking with Meena",
    val pendingCommand: VoiceCommandIntent? = null
)

data class ChatUiState(
    val currentConversationId: Long? = null,
    val isStreaming: Boolean = false,
    val streamingText: String = "",
    val streamingEngineTag: String = "",
    val streamingIsPreview: Boolean = false,
    val searchQuery: String = "",
    val attachedImageUri: String? = null,
    val preferGroqEngine: Boolean = true,
    val showPreviewNotice: Boolean = false
)

data class MeenaAppState(
    val settings: AppSettings = AppSettings(),
    val conversations: List<ConversationEntity> = emptyList(),
    val messages: List<ChatMessageEntity> = emptyList(),
    val memories: List<MemoryEntity> = emptyList(),
    val generatedImages: List<GeneratedImageEntity> = emptyList(),
    val quickActions: List<QuickActionConfig> = emptyList(),
    val connections: Map<ApiProvider, ProviderConnectionUi> = emptyMap(),
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val chat: ChatUiState = ChatUiState(),
    val voice: VoiceUiState = VoiceUiState(),
    val imageGen: ImageGenUiState = ImageGenUiState.Idle,
    val writeStudio: WriteUiState = WriteUiState.Idle,
    val weather: WeatherUiState = WeatherUiState.Idle,
    val isFlashlightOn: Boolean = false,
    val mediaVolume: Float = 0.6f
)
