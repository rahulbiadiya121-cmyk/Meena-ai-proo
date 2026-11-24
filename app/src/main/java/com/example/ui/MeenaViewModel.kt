package com.example.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiImageResult
import com.example.data.ai.AiRepository
import com.example.data.ai.AiStreamChunk
import com.example.data.ai.ChatTurn
import com.example.data.ai.MeenaAiRepository
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ConversationEntity
import com.example.data.local.GeneratedImageEntity
import com.example.data.local.MeenaDatabase
import com.example.data.local.MemoryEntity
import com.example.data.local.QuickActionEntity
import com.example.data.local.SettingsRepository
import com.example.data.security.SecureKeyVault
import com.example.domain.ApiProvider
import com.example.domain.AppSettings
import com.example.domain.ConnectionState
import com.example.domain.OrbState
import com.example.domain.ProviderConnectionUi
import com.example.domain.QuickActionConfig
import com.example.domain.QuickActionType
import com.example.domain.VoicePipelineStage
import com.example.system.PhoneControlManager
import com.example.system.VoiceCommandIntent
import com.example.system.VoicePipelineEngine
import com.example.ui.state.ChatUiState
import com.example.ui.state.ImageGenUiState
import com.example.ui.state.MeenaAppState
import com.example.ui.state.VoiceUiState
import com.example.ui.state.WeatherUiState
import com.example.ui.state.WriteUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MeenaViewModel(application: Application) : AndroidViewModel(application) {

    private val database = MeenaDatabase.getInstance(application)
    private val conversationDao = database.conversationDao()
    private val memoryDao = database.memoryDao()
    private val imageDao = database.generatedImageDao()
    private val quickActionDao = database.quickActionDao()
    private val settingsRepo = SettingsRepository(application)
    private val keyVault = SecureKeyVault(application)
    private val aiRepository: AiRepository = MeenaAiRepository(keyVault)
    val phoneControl = PhoneControlManager(application)
    val voiceEngine = VoicePipelineEngine(application)

    private val _state = MutableStateFlow(MeenaAppState())
    val state: StateFlow<MeenaAppState> = _state.asStateFlow()

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    private var messagesObserverJob: Job? = null
    private var activeChatJob: Job? = null
    private var activeVoiceJob: Job? = null
    private var activeWriteJob: Job? = null

    init {
        seedQuickActionsIfEmpty()
        refreshConnectionsFromVault()
        refreshInstalledApps()
        _state.update { it.copy(mediaVolume = phoneControl.getMediaVolumeFraction()) }
        observeCoreFlows()
    }

    private fun observeCoreFlows() {
        viewModelScope.launch {
            combine(
                settingsRepo.settingsFlow,
                conversationDao.observeConversations(),
                memoryDao.observeMemories(),
                imageDao.observeImages(),
                quickActionDao.observeQuickActions()
            ) { settings, conversations, memories, images, quickEntities ->
                val mappedActions = quickEntities.mapNotNull { entity ->
                    QuickActionType.fromId(entity.actionId)?.let { type ->
                        QuickActionConfig(
                            type = type,
                            enabled = entity.enabled,
                            pinned = entity.pinned,
                            orderIndex = entity.orderIndex
                        )
                    }
                }
                _state.value.copy(
                    settings = settings,
                    conversations = conversations,
                    memories = memories,
                    generatedImages = images,
                    quickActions = mappedActions
                )
            }.collect { merged ->
                val shouldAutoSelect = merged.chat.currentConversationId == null && merged.conversations.isNotEmpty()
                _state.value = merged
                if (shouldAutoSelect) {
                    selectConversation(merged.conversations.first().id)
                }
            }
        }

        viewModelScope.launch {
            voiceEngine.isSpeaking.collect { speaking ->
                _state.update { current ->
                    val currentVoice = current.voice
                    val newOrb = when {
                        speaking -> OrbState.SPEAKING
                        currentVoice.isListening -> OrbState.LISTENING
                        currentVoice.stage == VoicePipelineStage.UNDERSTAND ||
                            currentVoice.stage == VoicePipelineStage.RESPOND -> OrbState.PROCESSING
                        else -> OrbState.IDLE
                    }
                    val newStage = if (!speaking && currentVoice.stage == VoicePipelineStage.SPEAK) {
                        VoicePipelineStage.IDLE
                    } else currentVoice.stage
                    current.copy(voice = currentVoice.copy(orbState = newOrb, stage = newStage))
                }
            }
        }
    }

    private fun seedQuickActionsIfEmpty() {
        viewModelScope.launch(Dispatchers.IO) {
            if (quickActionDao.countQuickActions() == 0) {
                val defaults = QuickActionType.entries.mapIndexed { idx, type ->
                    QuickActionEntity(
                        actionId = type.id,
                        enabled = true,
                        pinned = idx < 4,
                        orderIndex = idx
                    )
                }
                quickActionDao.upsertAll(defaults)
            }
        }
    }

    fun refreshInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = phoneControl.getInstalledLauncherApps()
            _state.update { it.copy(installedApps = apps) }
        }
    }

    fun refreshConnectionsFromVault() {
        val map = ApiProvider.entries.associateWith { provider ->
            val hasKey = keyVault.hasKey(provider)
            val preview = keyVault.getMaskedPreview(provider)
            val existing = _state.value.connections[provider]
            ProviderConnectionUi(
                provider = provider,
                maskedPreview = preview,
                hasSavedKey = hasKey,
                status = when {
                    existing?.status == ConnectionState.CONNECTED && hasKey -> ConnectionState.CONNECTED
                    hasKey -> ConnectionState.SAVED
                    else -> ConnectionState.NOT_CONFIGURED
                },
                statusMessage = when {
                    existing?.status == ConnectionState.CONNECTED && hasKey -> existing.statusMessage
                    provider == ApiProvider.GEMINI && !hasKey -> "Requires Firebase AI Logic (or store key securely)"
                    hasKey -> "Encrypted in Android Keystore"
                    else -> "Not configured"
                }
            )
        }
        _state.update { it.copy(connections = map) }
    }

    fun saveApiKey(provider: ApiProvider, rawKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            keyVault.saveKey(provider, rawKey)
            refreshConnectionsFromVault()
            _toastEvents.emit("${provider.title} saved securely in Android Keystore")
        }
    }

    fun clearApiKey(provider: ApiProvider) {
        viewModelScope.launch(Dispatchers.IO) {
            keyVault.clearKey(provider)
            _state.update { st ->
                val updated = st.connections.toMutableMap()
                updated[provider] = ProviderConnectionUi(provider = provider)
                st.copy(connections = updated)
            }
            refreshConnectionsFromVault()
            _toastEvents.emit("${provider.title} removed")
        }
    }

    fun testConnection(provider: ApiProvider) {
        viewModelScope.launch {
            _state.update { st ->
                val updated = st.connections.toMutableMap()
                val cur = updated[provider] ?: ProviderConnectionUi(provider)
                updated[provider] = cur.copy(
                    status = ConnectionState.TESTING,
                    statusMessage = "Testing secure handshake…"
                )
                st.copy(connections = updated)
            }

            val result = aiRepository.testProviderConnection(provider)
            _state.update { st ->
                val updated = st.connections.toMutableMap()
                val cur = updated[provider] ?: ProviderConnectionUi(provider)
                updated[provider] = result.fold(
                    onSuccess = { msg ->
                        cur.copy(status = ConnectionState.CONNECTED, statusMessage = msg)
                    },
                    onFailure = { err ->
                        cur.copy(
                            status = ConnectionState.ERROR,
                            statusMessage = err.message ?: "Connection failed"
                        )
                    }
                )
                st.copy(connections = updated)
            }
        }
    }

    // --- Settings ---
    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            settingsRepo.updateSettings(transform)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            voiceEngine.stopSpeaking()
            voiceEngine.stopListening()
            settingsRepo.updateSettings { it.copy(isSignedIn = false) }
        }
    }

    fun signInProfile(name: String, handle: String, email: String) {
        viewModelScope.launch {
            settingsRepo.updateSettings {
                it.copy(
                    isSignedIn = true,
                    userName = name.ifBlank { "Friend" },
                    userHandle = handle.ifBlank { "@meena_explorer" },
                    accountEmail = email.ifBlank { "local.companion@meena.pro" }
                )
            }
        }
    }

    fun deleteAccountAndWipeData() {
        viewModelScope.launch(Dispatchers.IO) {
            voiceEngine.stopSpeaking()
            voiceEngine.stopListening()
            conversationDao.clearAllConversations()
            memoryDao.clearAllMemories()
            imageDao.clearAllImages()
            keyVault.clearAll()
            settingsRepo.clearAll()
            settingsRepo.updateSettings { it.copy(isSignedIn = false) }
            refreshConnectionsFromVault()
            _state.update {
                it.copy(
                    messages = emptyList(),
                    chat = ChatUiState(),
                    imageGen = ImageGenUiState.Idle,
                    writeStudio = WriteUiState.Idle
                )
            }
            _toastEvents.emit("Account & all local encrypted data deleted")
        }
    }

    // --- Conversations & Chat ---
    fun selectConversation(conversationId: Long) {
        messagesObserverJob?.cancel()
        _state.update { it.copy(chat = it.chat.copy(currentConversationId = conversationId)) }
        messagesObserverJob = viewModelScope.launch {
            conversationDao.observeMessages(conversationId).collectLatest { msgs ->
                _state.update { it.copy(messages = msgs) }
            }
        }
    }

    fun startNewConversation() {
        messagesObserverJob?.cancel()
        activeChatJob?.cancel()
        _state.update {
            it.copy(
                messages = emptyList(),
                chat = it.chat.copy(
                    currentConversationId = null,
                    isStreaming = false,
                    streamingText = "",
                    attachedImageUri = null
                )
            )
        }
    }

    fun deleteConversation(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            conversationDao.deleteConversationById(id)
            if (_state.value.chat.currentConversationId == id) {
                startNewConversation()
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            conversationDao.clearAllConversations()
            startNewConversation()
            _toastEvents.emit("Conversation history cleared")
        }
    }

    fun clearCurrentChat() {
        val convId = _state.value.chat.currentConversationId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            conversationDao.clearMessagesForConversation(convId)
        }
    }

    fun setChatSearchQuery(query: String) {
        _state.update { it.copy(chat = it.chat.copy(searchQuery = query)) }
    }

    fun setAttachedImageUri(uri: String?) {
        _state.update { it.copy(chat = it.chat.copy(attachedImageUri = uri)) }
    }

    fun toggleGroqPreferred(preferGroq: Boolean) {
        _state.update { it.copy(chat = it.chat.copy(preferGroqEngine = preferGroq)) }
    }

    fun sendChatMessage(rawPrompt: String) {
        val prompt = rawPrompt.trim()
        if (prompt.isEmpty() || _state.value.chat.isStreaming) return

        val attachedUri = _state.value.chat.attachedImageUri
        _state.update { it.copy(chat = it.chat.copy(attachedImageUri = null)) }

        activeChatJob?.cancel()
        activeChatJob = viewModelScope.launch {
            val currentSettings = _state.value.settings
            val persistHistory = currentSettings.saveHistoryEnabled && !currentSettings.privateModeEnabled

            val convId = if (persistHistory) {
                val existing = _state.value.chat.currentConversationId
                if (existing != null) existing else {
                    val newId = conversationDao.insertConversation(
                        ConversationEntity(
                            title = prompt.take(38),
                            lastPreview = prompt.take(80),
                            engineUsed = "Meena AI"
                        )
                    )
                    selectConversation(newId)
                    newId
                }
            } else {
                -1L
            }

            val userMsg = ChatMessageEntity(
                conversationId = convId.coerceAtLeast(0L),
                isUser = true,
                text = prompt,
                imageUri = attachedUri,
                engineTag = "You"
            )
            if (persistHistory && convId > 0L) {
                conversationDao.insertMessage(userMsg)
                extractMemoryIfRelevant(prompt, currentSettings)
            } else {
                _state.update { it.copy(messages = it.messages + userMsg) }
            }

            val historyTurns = _state.value.messages.map { ChatTurn(it.isUser, it.text) }
            val memories = if (currentSettings.memoryEnabled) {
                memoryDao.getTopMemoriesOnce().map { it.factText }
            } else emptyList()

            _state.update {
                it.copy(
                    chat = it.chat.copy(
                        isStreaming = true,
                        streamingText = "",
                        streamingEngineTag = "Thinking…"
                    )
                )
            }

            try {
                aiRepository.streamReply(
                    prompt = prompt,
                    history = historyTurns,
                    memories = memories,
                    settings = currentSettings,
                    preferGroq = _state.value.chat.preferGroqEngine && currentSettings.useGroqFastChat,
                    attachedImageNote = attachedUri
                ).collect { chunk ->
                    when (chunk) {
                        is AiStreamChunk.Delta -> {
                            _state.update {
                                it.copy(
                                    chat = it.chat.copy(
                                        streamingText = chunk.accumulated,
                                        streamingEngineTag = chunk.engineTag,
                                        streamingIsPreview = chunk.isPreviewFallback,
                                        showPreviewNotice = it.chat.showPreviewNotice || chunk.isPreviewFallback
                                    )
                                )
                            }
                        }

                        is AiStreamChunk.Complete -> {
                            val aiMsg = ChatMessageEntity(
                                conversationId = convId.coerceAtLeast(0L),
                                isUser = false,
                                text = chunk.fullText,
                                engineTag = chunk.engineTag
                            )
                            if (persistHistory && convId > 0L) {
                                conversationDao.insertMessage(aiMsg)
                                conversationDao.updateConversation(
                                    ConversationEntity(
                                        id = convId,
                                        title = _state.value.conversations.firstOrNull { it.id == convId }?.title
                                            ?: prompt.take(38),
                                        lastPreview = chunk.fullText.take(90),
                                        engineUsed = chunk.engineTag,
                                        updatedAt = System.currentTimeMillis()
                                    )
                                )
                            } else {
                                _state.update { it.copy(messages = it.messages + aiMsg) }
                            }
                            _state.update {
                                it.copy(
                                    chat = it.chat.copy(
                                        isStreaming = false,
                                        streamingText = "",
                                        showPreviewNotice = it.chat.showPreviewNotice || chunk.isPreviewFallback
                                    )
                                )
                            }
                        }

                        is AiStreamChunk.Error -> {
                            _toastEvents.emit(chunk.message)
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(chat = it.chat.copy(isStreaming = false, streamingText = "")) }
                _toastEvents.emit(e.localizedMessage ?: "Failed to generate reply")
            }
        }
    }

    fun regenerateLastResponse() {
        val lastUserMsg = _state.value.messages.lastOrNull { it.isUser } ?: return
        sendChatMessage(lastUserMsg.text)
    }

    // --- AI Memory ---
    private suspend fun extractMemoryIfRelevant(prompt: String, settings: AppSettings) {
        if (!settings.memoryEnabled || settings.privateModeEnabled) return
        val lower = prompt.lowercase()
        val triggers = listOf("my name is ", "call me ", "i like ", "i love ", "remember that ", "mera naam ", "mujhe pasand ")
        val matched = triggers.firstOrNull { lower.contains(it) } ?: return
        val category = when {
            matched.contains("name") || matched.contains("naam") || matched.contains("call me") -> "Identity"
            matched.contains("like") || matched.contains("love") || matched.contains("pasand") -> "Preference"
            else -> "Context"
        }
        memoryDao.insertMemory(MemoryEntity(factText = prompt.take(180), category = category))
    }

    fun addManualMemory(text: String, category: String = "Preference") {
        val clean = text.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            memoryDao.insertMemory(MemoryEntity(factText = clean, category = category))
            _toastEvents.emit("Saved to Meena's memory")
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            memoryDao.deleteMemoryById(id)
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch(Dispatchers.IO) {
            memoryDao.clearAllMemories()
            _toastEvents.emit("All AI memories cleared")
        }
    }

    // --- 4-Stage Voice Assistant Pipeline ---
    fun startVoiceListening() {
        voiceEngine.stopSpeaking()
        _state.update {
            it.copy(
                voice = it.voice.copy(
                    stage = VoicePipelineStage.LISTEN,
                    orbState = OrbState.LISTENING,
                    isListening = true,
                    liveTranscript = "",
                    pendingCommand = null,
                    statusBanner = "Listening… speak naturally in Hindi, English, or Hinglish"
                )
            )
        }
        voiceEngine.startListening(
            language = _state.value.settings.voiceLanguage,
            onPartialResult = { partial ->
                _state.update {
                    it.copy(voice = it.voice.copy(liveTranscript = partial))
                }
            },
            onFinalResult = { finalTranscript ->
                _state.update {
                    it.copy(
                        voice = it.voice.copy(
                            isListening = false,
                            liveTranscript = finalTranscript
                        )
                    )
                }
                processVoiceTranscript(finalTranscript)
            },
            onError = { errMsg ->
                _state.update {
                    it.copy(
                        voice = it.voice.copy(
                            isListening = false,
                            stage = VoicePipelineStage.IDLE,
                            orbState = OrbState.IDLE,
                            statusBanner = errMsg
                        )
                    )
                }
            }
        )
    }

    fun stopVoiceListening() {
        voiceEngine.stopListening()
        _state.update {
            it.copy(
                voice = it.voice.copy(
                    isListening = false,
                    stage = VoicePipelineStage.IDLE,
                    orbState = OrbState.IDLE,
                    statusBanner = "Tap to talk with Meena"
                )
            )
        }
    }

    fun interruptVoice() {
        activeVoiceJob?.cancel()
        voiceEngine.stopSpeaking()
        voiceEngine.stopListening()
        _state.update {
            it.copy(
                voice = it.voice.copy(
                    isListening = false,
                    stage = VoicePipelineStage.IDLE,
                    orbState = OrbState.IDLE,
                    statusBanner = "Interrupted — tap mic to speak"
                )
            )
        }
    }

    fun processVoiceTranscript(transcript: String) {
        val text = transcript.trim()
        if (text.isEmpty()) return

        activeVoiceJob?.cancel()
        activeVoiceJob = viewModelScope.launch {
            // Stage 2: UNDERSTAND
            val (lang, emotion) = VoicePipelineEngine.detectLanguageAndEmotion(text)
            val voiceCommand = VoicePipelineEngine.detectVoiceCommand(text)

            _state.update {
                it.copy(
                    voice = it.voice.copy(
                        stage = VoicePipelineStage.UNDERSTAND,
                        orbState = OrbState.PROCESSING,
                        liveTranscript = text,
                        detectedLanguage = lang,
                        detectedEmotion = emotion,
                        pendingCommand = voiceCommand,
                        statusBanner = "Understood $lang ($emotion)…"
                    )
                )
            }

            if (voiceCommand != null) {
                val cmdReply = when (voiceCommand) {
                    is VoiceCommandIntent.OpenApp -> "Sure! Tap Confirm below to open ${voiceCommand.appQuery}."
                    is VoiceCommandIntent.Flashlight -> {
                        val res = phoneControl.setFlashlight(voiceCommand.on)
                        _state.update { st -> st.copy(isFlashlightOn = res.getOrDefault(voiceCommand.on)) }
                        if (voiceCommand.on) "Flashlight turned on for you!" else "Flashlight turned off!"
                    }
                    is VoiceCommandIntent.Call -> "Ready to call ${voiceCommand.target}. Please confirm below."
                    is VoiceCommandIntent.OpenAlarm -> "I've prepared your alarm shortcut — confirm to open Clock."
                    is VoiceCommandIntent.CheckWeather -> "Opening weather forecast for you!"
                }
                speakAssistantReply(cmdReply, isPreview = false)
                return@launch
            }

            // Stage 3: RESPOND
            _state.update {
                it.copy(
                    voice = it.voice.copy(
                        stage = VoicePipelineStage.RESPOND,
                        orbState = OrbState.PROCESSING,
                        statusBanner = "Meena is thinking…"
                    )
                )
            }

            val currentSettings = _state.value.settings
            val memories = if (currentSettings.memoryEnabled) {
                memoryDao.getTopMemoriesOnce().map { it.factText }
            } else emptyList()

            val turns = _state.value.messages.takeLast(6).map { ChatTurn(it.isUser, it.text) }
            var finalReply = ""
            var isPreview = false

            aiRepository.streamReply(
                prompt = text,
                history = turns,
                memories = memories,
                settings = currentSettings,
                preferGroq = currentSettings.useGroqFastChat
            ).collect { chunk ->
                when (chunk) {
                    is AiStreamChunk.Delta -> {
                        _state.update {
                            it.copy(voice = it.voice.copy(assistantReply = chunk.accumulated))
                        }
                    }
                    is AiStreamChunk.Complete -> {
                        finalReply = chunk.fullText
                        isPreview = chunk.isPreviewFallback
                    }
                    is AiStreamChunk.Error -> {
                        _toastEvents.emit(chunk.message)
                    }
                }
            }

            if (finalReply.isNotBlank()) {
                speakAssistantReply(finalReply, isPreview)
            }
        }
    }

    private suspend fun speakAssistantReply(reply: String, isPreview: Boolean) {
        val currentSettings = _state.value.settings
        _state.update {
            it.copy(
                voice = it.voice.copy(
                    stage = VoicePipelineStage.SPEAK,
                    orbState = OrbState.SPEAKING,
                    assistantReply = reply,
                    isPreviewNotice = isPreview,
                    statusBanner = "Meena is speaking (tap orb to interrupt)"
                )
            )
        }

        val hasElevenLabs = keyVault.hasKey(ApiProvider.ELEVENLABS) && currentSettings.useElevenLabsWhenAvailable
        if (hasElevenLabs) {
            val cleanText = reply.replace(Regex("Preview AI Mode:.*"), "").trim()
            val audioRes = aiRepository.synthesizeElevenLabsAudio(cleanText, currentSettings)
            if (audioRes.isSuccess) {
                voiceEngine.playAudioBytes(audioRes.getOrThrow(), currentSettings.voiceVolume) {
                    _state.update { st ->
                        st.copy(
                            voice = st.voice.copy(
                                stage = VoicePipelineStage.IDLE,
                                orbState = OrbState.IDLE,
                                statusBanner = "Tap to continue talking"
                            )
                        )
                    }
                }
                return
            }
        }

        voiceEngine.speakWithSystemTts(reply, currentSettings) {
            _state.update { st ->
                st.copy(
                    voice = st.voice.copy(
                        stage = VoicePipelineStage.IDLE,
                        orbState = OrbState.IDLE,
                        statusBanner = "Tap to continue talking"
                    )
                )
            }
        }
    }

    fun speakTextAloud(text: String) {
        viewModelScope.launch {
            speakAssistantReply(text, isPreview = false)
        }
    }

    fun testCurrentVoice() {
        val sample = when (_state.value.settings.voiceLanguage) {
            com.example.domain.VoiceLanguage.HINDI ->
                "Namaste! Main Meena hoon, aapki personal AI companion. Aapka din shubh ho!"
            com.example.domain.VoiceLanguage.ENGLISH ->
                "Hey there! I'm Meena, your personal AI companion. Ready whenever you are!"
            com.example.domain.VoiceLanguage.HINGLISH_AUTO ->
                "Hey! Main Meena hoon — aapki personal AI companion! Bolo aaj kya mazedaar karein?"
        }
        viewModelScope.launch {
            speakAssistantReply(sample, isPreview = false)
        }
    }

    fun clearPendingVoiceCommand() {
        _state.update { it.copy(voice = it.voice.copy(pendingCommand = null)) }
    }

    // --- AI Image Generation ---
    fun generateAiImage(prompt: String, style: String, quality: String, aspectRatio: String) {
        val clean = prompt.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(imageGen = ImageGenUiState.Loading) }
            try {
                when (val result = aiRepository.generateImage(clean, style, quality, aspectRatio)) {
                    is AiImageResult.RequiresFirebase -> {
                        imageDao.insertImage(
                            GeneratedImageEntity(
                                prompt = clean,
                                style = style,
                                quality = quality,
                                aspectRatio = aspectRatio,
                                noticeText = result.notice
                            )
                        )
                        _state.update {
                            it.copy(
                                imageGen = ImageGenUiState.PreviewNotice(
                                    notice = result.notice,
                                    enhancedPrompt = result.enhancedPrompt,
                                    style = result.style,
                                    quality = result.quality,
                                    aspectRatio = result.aspectRatio
                                )
                            )
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(imageGen = ImageGenUiState.Error(e.localizedMessage ?: "Image generation failed"))
                }
            }
        }
    }

    fun deleteGeneratedImage(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            imageDao.deleteImageById(id)
        }
    }

    // --- Write Studio ---
    fun generateWrittenContent(category: String, tone: String, prompt: String) {
        val clean = prompt.trim()
        if (clean.isEmpty()) return
        activeWriteJob?.cancel()
        activeWriteJob = viewModelScope.launch {
            _state.update { it.copy(writeStudio = WriteUiState.Loading) }
            val fullPrompt = "Write a $tone $category about: $clean"
            try {
                aiRepository.streamReply(
                    prompt = fullPrompt,
                    history = emptyList(),
                    memories = emptyList(),
                    settings = _state.value.settings,
                    preferGroq = _state.value.settings.useGroqFastChat
                ).collect { chunk ->
                    when (chunk) {
                        is AiStreamChunk.Delta -> _state.update {
                            it.copy(
                                writeStudio = WriteUiState.Streaming(
                                    chunk.accumulated,
                                    chunk.engineTag,
                                    chunk.isPreviewFallback
                                )
                            )
                        }
                        is AiStreamChunk.Complete -> _state.update {
                            it.copy(
                                writeStudio = WriteUiState.Success(
                                    chunk.fullText,
                                    chunk.engineTag,
                                    chunk.isPreviewFallback
                                )
                            )
                        }
                        is AiStreamChunk.Error -> _toastEvents.emit(chunk.message)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(writeStudio = WriteUiState.Error(e.localizedMessage ?: "Write generation failed"))
                }
            }
        }
    }

    // --- Quick Actions & Phone Control ---
    fun toggleQuickActionEnabled(type: QuickActionType) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = _state.value.quickActions.firstOrNull { it.type == type } ?: return@launch
            quickActionDao.upsert(
                QuickActionEntity(
                    actionId = type.id,
                    enabled = !current.enabled,
                    pinned = current.pinned,
                    orderIndex = current.orderIndex
                )
            )
        }
    }

    fun toggleQuickActionPinned(type: QuickActionType) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = _state.value.quickActions.firstOrNull { it.type == type } ?: return@launch
            quickActionDao.upsert(
                QuickActionEntity(
                    actionId = type.id,
                    enabled = current.enabled,
                    pinned = !current.pinned,
                    orderIndex = current.orderIndex
                )
            )
        }
    }

    fun moveQuickActionUp(type: QuickActionType) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = _state.value.quickActions.toMutableList()
            val idx = list.indexOfFirst { it.type == type }
            if (idx > 0) {
                val swapped = list[idx - 1]
                list[idx - 1] = list[idx]
                list[idx] = swapped
                quickActionDao.upsertAll(
                    list.mapIndexed { i, item ->
                        QuickActionEntity(item.type.id, item.enabled, item.pinned, i)
                    }
                )
            }
        }
    }

    fun fetchWeatherForCity(city: String) {
        viewModelScope.launch {
            _state.update { it.copy(weather = WeatherUiState.Loading) }
            val result = aiRepository.fetchWeather(city)
            _state.update { st ->
                st.copy(
                    weather = result.fold(
                        onSuccess = { WeatherUiState.Success(it) },
                        onFailure = { WeatherUiState.Error(it.message ?: "Weather unavailable") }
                    )
                )
            }
        }
    }

    fun toggleFlashlight() {
        val target = !_state.value.isFlashlightOn
        val res = phoneControl.setFlashlight(target)
        res.onSuccess { on ->
            _state.update { it.copy(isFlashlightOn = on) }
        }.onFailure { err ->
            viewModelScope.launch { _toastEvents.emit(err.message ?: "Flashlight unavailable") }
        }
    }

    fun setMediaVolume(fraction: Float) {
        phoneControl.setMediaVolumeFraction(fraction)
        _state.update { it.copy(mediaVolume = fraction) }
    }

    fun openSystemSetting(action: String) {
        phoneControl.openSystemSetting(action).onFailure { err ->
            viewModelScope.launch { _toastEvents.emit(err.message ?: "Unable to open setting") }
        }
    }

    fun shareText(text: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(sendIntent, "Share via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        getApplication<Application>().startActivity(chooser)
    }

    override fun onCleared() {
        voiceEngine.release()
        super.onCleared()
    }
}
