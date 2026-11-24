package com.example.system

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.domain.AppSettings
import com.example.domain.VoiceLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.Locale

class VoicePipelineEngine(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isTtsReady = false

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var pendingSpeakText: String? = null
    private var pendingSettings: AppSettings? = null
    private var pendingOnDone: (() -> Unit)? = null

    init {
        initTts()
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context.applicationContext) { status ->
            isTtsReady = status == TextToSpeech.SUCCESS
            if (isTtsReady) {
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        pendingOnDone?.invoke()
                        pendingOnDone = null
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        pendingOnDone?.invoke()
                        pendingOnDone = null
                    }
                })
                val queued = pendingSpeakText
                val settings = pendingSettings
                if (queued != null && settings != null) {
                    pendingSpeakText = null
                    pendingSettings = null
                    speakWithSystemTts(queued, settings, pendingOnDone)
                }
            }
        }
    }

    fun isSpeechRecognitionAvailable(): Boolean =
        SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening(
        language: VoiceLanguage,
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        stopListening()
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("On-device speech recognizer unavailable. Type or tap a voice prompt below.")
            return
        }
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _audioLevel.value = 0.15f
                    }

                    override fun onBeginningOfSpeech() {
                        _audioLevel.value = 0.45f
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.08f, 1f)
                        _audioLevel.value = normalized
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _audioLevel.value = 0.1f
                    }

                    override fun onError(error: Int) {
                        _audioLevel.value = 0f
                        val msg = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Microphone audio error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech network timeout"
                            SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that — tap mic to try again"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                            else -> "Voice capture paused (code $error)"
                        }
                        onError(msg)
                    }

                    override fun onResults(results: Bundle?) {
                        _audioLevel.value = 0f
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val best = matches?.firstOrNull().orEmpty()
                        if (best.isNotBlank()) {
                            onFinalResult(best)
                        } else {
                            onError("No speech recognized")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull().orEmpty()
                        if (partial.isNotBlank()) {
                            onPartialResult(partial)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.localeTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _audioLevel.value = 0f
            onError(e.localizedMessage ?: "Failed to start microphone")
        }
    }

    fun stopListening() {
        _audioLevel.value = 0f
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        } finally {
            speechRecognizer = null
        }
    }

    fun speakWithSystemTts(
        rawText: String,
        settings: AppSettings,
        onDone: (() -> Unit)? = null
    ) {
        val cleanText = rawText
            .replace(Regex("```[\\s\\S]*?```"), "Code snippet ready on screen.")
            .replace(Regex("[*_`#>]"), "")
            .replace(Regex("Preview AI Mode:.*"), "")
            .trim()
            .ifBlank { rawText }

        if (!isTtsReady) {
            pendingSpeakText = cleanText
            pendingSettings = settings
            pendingOnDone = onDone
            return
        }

        val locale = when (settings.voiceLanguage) {
            VoiceLanguage.HINDI -> Locale("hi", "IN")
            VoiceLanguage.HINGLISH_AUTO -> Locale("en", "IN")
            VoiceLanguage.ENGLISH -> Locale.US
        }
        textToSpeech?.language = locale
        textToSpeech?.setSpeechRate(settings.voiceSpeed.coerceIn(0.6f, 1.8f))
        textToSpeech?.setPitch(settings.voicePitch.coerceIn(0.6f, 1.6f))
        pendingOnDone = onDone

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, settings.voiceVolume.coerceIn(0.1f, 1f))
        }
        _isSpeaking.value = true
        textToSpeech?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, "meena_utt_${System.currentTimeMillis()}")
    }

    fun playAudioBytes(audioBytes: ByteArray, volume: Float, onDone: () -> Unit) {
        stopSpeaking()
        try {
            val tempFile = File.createTempFile("meena_voice_", ".mp3", context.cacheDir)
            tempFile.writeBytes(audioBytes)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                setVolume(volume.coerceIn(0.1f, 1f), volume.coerceIn(0.1f, 1f))
                setOnCompletionListener {
                    _isSpeaking.value = false
                    tempFile.delete()
                    onDone()
                }
                setOnErrorListener { _, _, _ ->
                    _isSpeaking.value = false
                    tempFile.delete()
                    onDone()
                    true
                }
                prepare()
                _isSpeaking.value = true
                start()
            }
        } catch (_: Exception) {
            _isSpeaking.value = false
            onDone()
        }
    }

    fun stopSpeaking() {
        _isSpeaking.value = false
        pendingOnDone = null
        try {
            textToSpeech?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {
        } finally {
            mediaPlayer = null
        }
    }

    fun release() {
        stopListening()
        stopSpeaking()
        try {
            textToSpeech?.shutdown()
        } catch (_: Exception) {
        }
    }

    companion object {
        fun detectLanguageAndEmotion(input: String): Pair<String, String> {
            val lower = input.lowercase()
            val hasDevanagari = input.any { it in '\u0900'..'\u097F' }
            val hinglishKeywords = listOf("kya", "kaise", "hai", "haan", "nahi", "karo", "batao", "yaar", "aaj", "kal", "sun", "chalo", "bhai", "dost")
            val language = when {
                hasDevanagari -> "Hindi"
                hinglishKeywords.any { lower.contains(it) } -> "Hinglish"
                else -> "English"
            }
            val emotion = when {
                lower.contains("haha") || lower.contains("lol") || lower.contains("joke") || lower.contains("funny") -> "Playful"
                lower.contains("tired") || lower.contains("sad") || lower.contains("stress") || lower.contains("thak") -> "Caring Needed"
                lower.contains("urgent") || lower.contains("quick") || lower.contains("jaldi") -> "Focused"
                lower.contains("love") || lower.contains("awesome") || lower.contains("great") || lower.contains("mast") -> "Happy"
                else -> "Conversational"
            }
            return language to emotion
        }

        fun detectVoiceCommand(input: String): VoiceCommandIntent? {
            val lower = input.trim().lowercase()
            return when {
                lower.startsWith("open ") -> VoiceCommandIntent.OpenApp(input.trim().substring(5))
                lower.contains("flashlight on") || lower.contains("torch on") -> VoiceCommandIntent.Flashlight(true)
                lower.contains("flashlight off") || lower.contains("torch off") -> VoiceCommandIntent.Flashlight(false)
                lower.startsWith("call ") -> VoiceCommandIntent.Call(input.trim().substring(5))
                lower.contains("set alarm") || lower.contains("alarm ") -> VoiceCommandIntent.OpenAlarm(input)
                lower.contains("weather") || lower.contains("mausam") -> VoiceCommandIntent.CheckWeather
                else -> null
            }
        }
    }
}

sealed interface VoiceCommandIntent {
    data class OpenApp(val appQuery: String) : VoiceCommandIntent
    data class Flashlight(val on: Boolean) : VoiceCommandIntent
    data class Call(val target: String) : VoiceCommandIntent
    data class OpenAlarm(val rawPhrase: String) : VoiceCommandIntent
    data object CheckWeather : VoiceCommandIntent
}
