package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ChatMessage
import com.example.data.repository.GeminiRepository
import com.example.data.repository.NetworkGeminiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface GeminiUiState {
    data object Idle : GeminiUiState
    data object Loading : GeminiUiState
    data class Success(val lastReply: String) : GeminiUiState
    data class Error(val message: String) : GeminiUiState
}

data class GeminiScreenState(
    val isKeyConfigured: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val uiState: GeminiUiState = GeminiUiState.Idle,
    val selectedModel: String = GeminiRepository.DEFAULT_MODEL
)

class GeminiViewModel(
    private val repository: GeminiRepository = NetworkGeminiRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(
        GeminiScreenState(isKeyConfigured = repository.isApiKeyConfigured())
    )
    val state: StateFlow<GeminiScreenState> = _state.asStateFlow()

    fun sendPrompt(rawPrompt: String) {
        val prompt = rawPrompt.trim()
        if (prompt.isEmpty() || _state.value.uiState is GeminiUiState.Loading) return

        val updatedHistory = _state.value.messages + ChatMessage(isUser = true, text = prompt)
        _state.update {
            it.copy(
                messages = updatedHistory,
                uiState = GeminiUiState.Loading
            )
        }

        viewModelScope.launch {
            val result = repository.generateChat(
                history = updatedHistory,
                systemInstruction = COMPANION_SYSTEM_PROMPT,
                model = _state.value.selectedModel
            )
            result.fold(
                onSuccess = { reply ->
                    val aiMsg = ChatMessage(isUser = false, text = reply)
                    _state.update {
                        it.copy(
                            messages = it.messages + aiMsg,
                            uiState = GeminiUiState.Success(reply)
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            uiState = GeminiUiState.Error(
                                error.message ?: "Unable to get response from Gemini."
                            )
                        )
                    }
                }
            )
        }
    }

    fun setModel(model: String) {
        _state.update { it.copy(selectedModel = model) }
    }

    fun clearConversation() {
        _state.update { it.copy(messages = emptyList(), uiState = GeminiUiState.Idle) }
    }

    companion object {
        private const val COMPANION_SYSTEM_PROMPT =
            "You are MEENA, a warm, intelligent, expressive personal AI companion. Respond helpfully and concisely."
    }
}
