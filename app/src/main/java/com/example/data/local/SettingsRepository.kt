package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.AppSettings
import com.example.domain.MeenaCharacter
import com.example.domain.OrbStyle
import com.example.domain.ThemeAccent
import com.example.domain.VoiceEmotion
import com.example.domain.VoiceLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.meenaSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "meena_ai_pro_settings"
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_HANDLE = stringPreferencesKey("user_handle")
        val ACCOUNT_EMAIL = stringPreferencesKey("account_email")
        val IS_SIGNED_IN = booleanPreferencesKey("is_signed_in")
        val ASSISTANT_NAME = stringPreferencesKey("assistant_name")
        val PERSONALITY = stringPreferencesKey("personality")
        val VOICE_ID = stringPreferencesKey("voice_id")
        val VOICE_SPEED = floatPreferencesKey("voice_speed")
        val VOICE_PITCH = floatPreferencesKey("voice_pitch")
        val VOICE_VOLUME = floatPreferencesKey("voice_volume")
        val VOICE_LANGUAGE = stringPreferencesKey("voice_language")
        val VOICE_EMOTION = stringPreferencesKey("voice_emotion")
        val USE_ELEVEN_LABS = booleanPreferencesKey("use_eleven_labs")
        val USE_GROQ_FAST = booleanPreferencesKey("use_groq_fast")
        val MEMORY_ENABLED = booleanPreferencesKey("memory_enabled")
        val SAVE_HISTORY_ENABLED = booleanPreferencesKey("save_history_enabled")
        val PRIVATE_MODE = booleanPreferencesKey("private_mode")
        val THEME_ACCENT = stringPreferencesKey("theme_accent")
        val ORB_STYLE = stringPreferencesKey("orb_style")
        val SELECTED_CHARACTER = stringPreferencesKey("selected_character")
        val SHOW_CHAR_EVERYWHERE = booleanPreferencesKey("show_char_everywhere")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
    }

    val settingsFlow: Flow<AppSettings> = context.meenaSettingsDataStore.data.map { prefs ->
        AppSettings(
            userName = prefs[Keys.USER_NAME] ?: "Friend",
            userHandle = prefs[Keys.USER_HANDLE] ?: "@meena_explorer",
            accountEmail = prefs[Keys.ACCOUNT_EMAIL] ?: "local.companion@meena.pro",
            isSignedIn = prefs[Keys.IS_SIGNED_IN] ?: true,
            assistantName = prefs[Keys.ASSISTANT_NAME] ?: "MEENA",
            personality = prefs[Keys.PERSONALITY] ?: "Friendly, lovely, funny & expressive",
            voiceId = prefs[Keys.VOICE_ID] ?: "Meena Natural (Female)",
            voiceSpeed = prefs[Keys.VOICE_SPEED] ?: 1.02f,
            voicePitch = prefs[Keys.VOICE_PITCH] ?: 1.08f,
            voiceVolume = prefs[Keys.VOICE_VOLUME] ?: 0.95f,
            voiceLanguage = VoiceLanguage.fromId(prefs[Keys.VOICE_LANGUAGE] ?: VoiceLanguage.HINGLISH_AUTO.id),
            voiceEmotion = VoiceEmotion.fromId(prefs[Keys.VOICE_EMOTION] ?: VoiceEmotion.FRIENDLY_LOVELY.id),
            useElevenLabsWhenAvailable = prefs[Keys.USE_ELEVEN_LABS] ?: true,
            useGroqFastChat = prefs[Keys.USE_GROQ_FAST] ?: true,
            memoryEnabled = prefs[Keys.MEMORY_ENABLED] ?: true,
            saveHistoryEnabled = prefs[Keys.SAVE_HISTORY_ENABLED] ?: true,
            privateModeEnabled = prefs[Keys.PRIVATE_MODE] ?: false,
            themeAccent = ThemeAccent.fromId(prefs[Keys.THEME_ACCENT] ?: ThemeAccent.COSMIC_VIOLET.id),
            orbStyle = OrbStyle.fromId(prefs[Keys.ORB_STYLE] ?: OrbStyle.CLASSIC_VIOLET_NEBULA.id),
            selectedCharacter = MeenaCharacter.fromId(prefs[Keys.SELECTED_CHARACTER] ?: MeenaCharacter.ORIGINAL_MEENA.id),
            showCharacterEverywhere = prefs[Keys.SHOW_CHAR_EVERYWHERE] ?: true,
            reduceMotion = prefs[Keys.REDUCE_MOTION] ?: false
        )
    }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        context.meenaSettingsDataStore.edit { prefs ->
            val current = AppSettings(
                userName = prefs[Keys.USER_NAME] ?: "Friend",
                userHandle = prefs[Keys.USER_HANDLE] ?: "@meena_explorer",
                accountEmail = prefs[Keys.ACCOUNT_EMAIL] ?: "local.companion@meena.pro",
                isSignedIn = prefs[Keys.IS_SIGNED_IN] ?: true,
                assistantName = prefs[Keys.ASSISTANT_NAME] ?: "MEENA",
                personality = prefs[Keys.PERSONALITY] ?: "Friendly, lovely, funny & expressive",
                voiceId = prefs[Keys.VOICE_ID] ?: "Meena Natural (Female)",
                voiceSpeed = prefs[Keys.VOICE_SPEED] ?: 1.02f,
                voicePitch = prefs[Keys.VOICE_PITCH] ?: 1.08f,
                voiceVolume = prefs[Keys.VOICE_VOLUME] ?: 0.95f,
                voiceLanguage = VoiceLanguage.fromId(prefs[Keys.VOICE_LANGUAGE] ?: VoiceLanguage.HINGLISH_AUTO.id),
                voiceEmotion = VoiceEmotion.fromId(prefs[Keys.VOICE_EMOTION] ?: VoiceEmotion.FRIENDLY_LOVELY.id),
                useElevenLabsWhenAvailable = prefs[Keys.USE_ELEVEN_LABS] ?: true,
                useGroqFastChat = prefs[Keys.USE_GROQ_FAST] ?: true,
                memoryEnabled = prefs[Keys.MEMORY_ENABLED] ?: true,
                saveHistoryEnabled = prefs[Keys.SAVE_HISTORY_ENABLED] ?: true,
                privateModeEnabled = prefs[Keys.PRIVATE_MODE] ?: false,
                themeAccent = ThemeAccent.fromId(prefs[Keys.THEME_ACCENT] ?: ThemeAccent.COSMIC_VIOLET.id),
                orbStyle = OrbStyle.fromId(prefs[Keys.ORB_STYLE] ?: OrbStyle.CLASSIC_VIOLET_NEBULA.id),
                selectedCharacter = MeenaCharacter.fromId(prefs[Keys.SELECTED_CHARACTER] ?: MeenaCharacter.ORIGINAL_MEENA.id),
                showCharacterEverywhere = prefs[Keys.SHOW_CHAR_EVERYWHERE] ?: true,
                reduceMotion = prefs[Keys.REDUCE_MOTION] ?: false
            )
            val updated = transform(current)
            prefs[Keys.USER_NAME] = updated.userName
            prefs[Keys.USER_HANDLE] = updated.userHandle
            prefs[Keys.ACCOUNT_EMAIL] = updated.accountEmail
            prefs[Keys.IS_SIGNED_IN] = updated.isSignedIn
            prefs[Keys.ASSISTANT_NAME] = updated.assistantName
            prefs[Keys.PERSONALITY] = updated.personality
            prefs[Keys.VOICE_ID] = updated.voiceId
            prefs[Keys.VOICE_SPEED] = updated.voiceSpeed
            prefs[Keys.VOICE_PITCH] = updated.voicePitch
            prefs[Keys.VOICE_VOLUME] = updated.voiceVolume
            prefs[Keys.VOICE_LANGUAGE] = updated.voiceLanguage.id
            prefs[Keys.VOICE_EMOTION] = updated.voiceEmotion.id
            prefs[Keys.USE_ELEVEN_LABS] = updated.useElevenLabsWhenAvailable
            prefs[Keys.USE_GROQ_FAST] = updated.useGroqFastChat
            prefs[Keys.MEMORY_ENABLED] = updated.memoryEnabled
            prefs[Keys.SAVE_HISTORY_ENABLED] = updated.saveHistoryEnabled
            prefs[Keys.PRIVATE_MODE] = updated.privateModeEnabled
            prefs[Keys.THEME_ACCENT] = updated.themeAccent.id
            prefs[Keys.ORB_STYLE] = updated.orbStyle.id
            prefs[Keys.SELECTED_CHARACTER] = updated.selectedCharacter.id
            prefs[Keys.SHOW_CHAR_EVERYWHERE] = updated.showCharacterEverywhere
            prefs[Keys.REDUCE_MOTION] = updated.reduceMotion
        }
    }

    suspend fun clearAll() {
        context.meenaSettingsDataStore.edit { it.clear() }
    }
}
