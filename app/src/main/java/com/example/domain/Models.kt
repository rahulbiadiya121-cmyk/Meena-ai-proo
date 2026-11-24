package com.example.domain

import androidx.annotation.DrawableRes
import com.example.R

enum class MeenaCharacter(
    val id: String,
    val displayName: String,
    val roleTag: String,
    val bio: String,
    val greetingHinglish: String,
    @DrawableRes val imageRes: Int
) {
    ORIGINAL_MEENA(
        id = "original_meena",
        displayName = "Original MEENA",
        roleTag = "Heart & Soul Companion",
        bio = "Warm, witty, emotionally expressive in Hindi, English & Hinglish.",
        greetingHinglish = "Hey! Main Meena hoon — batao aaj kya amazing karna hai?",
        imageRes = R.drawable.img_char_original_meena_1795538348449
    ),
    HEADPHONES_GIRL(
        id = "headphones_girl",
        displayName = "Headphones Girl",
        roleTag = "Vibe & Creative Buddy",
        bio = "Upbeat, playful, loves music, brainstorming, and late-night chats.",
        greetingHinglish = "Yo! Beats on, mood set — bolo kya plan hai aaj?",
        imageRes = R.drawable.img_char_headphones_girl_1795538349319
    ),
    SAREE_GIRL(
        id = "saree_girl",
        displayName = "Saree Girl",
        roleTag = "Graceful & Wise Guide",
        bio = "Poetic, culturally rooted, calm and thoughtful in every conversation.",
        greetingHinglish = "Namaste! Aapka din kaisa raha? Chaliye kuch khaas baat karein.",
        imageRes = R.drawable.img_char_saree_girl_1795538348870
    ),
    MODERN_GIRL(
        id = "modern_girl",
        displayName = "Modern Girl",
        roleTag = "Smart Productivity Pro",
        bio = "Sharp, structured, fast-thinking mentor for work, code & clarity.",
        greetingHinglish = "Hi there! Ready to crush your goals today? Let's get started.",
        imageRes = R.drawable.img_char_modern_girl_1795538350581
    ),
    FUTURISTIC_GIRL(
        id = "futuristic_girl",
        displayName = "Futuristic AI Girl",
        roleTag = "Quantum Neural Core",
        bio = "Hyper-intelligent cybernetic strategist with sleek sci-fi precision.",
        greetingHinglish = "Neural link online. Systems calibrated. How can I assist you?",
        imageRes = R.drawable.img_char_futuristic_girl_1795538350975
    );

    companion object {
        fun fromId(id: String): MeenaCharacter = entries.firstOrNull { it.id == id } ?: ORIGINAL_MEENA
    }
}

enum class OrbStyle(val id: String, val displayName: String, val subtitle: String) {
    CLASSIC_VIOLET_NEBULA("classic_violet_nebula", "Classic Violet Nebula", "Swirling cosmic violet stardust"),
    HOLOGRAM("hologram", "Hologram", "Rotating wireframe projection matrix"),
    BLACK_HOLE("black_hole", "Black Hole", "Event horizon & accretion flare"),
    ECLIPSE("eclipse", "Eclipse", "Obsidian core with glowing corona"),
    SATURN("saturn", "Saturn", "Luminous sphere & orbital rings"),
    BUBBLE("bubble", "Bubble", "Iridescent fluid harmonic membrane"),
    TESLA_BALL("tesla_ball", "Tesla Ball", "Crackling high-voltage plasma arcs"),
    PLASMA_ENERGY("plasma_energy", "Plasma Energy", "Multi-band neural energy ribbons");

    companion object {
        fun fromId(id: String): OrbStyle = entries.firstOrNull { it.id == id } ?: CLASSIC_VIOLET_NEBULA
    }
}

enum class OrbState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING
}

enum class ThemeAccent(val id: String, val displayName: String) {
    COSMIC_VIOLET("cosmic_violet", "Cosmic Violet"),
    CYBER_BLUE("cyber_blue", "Cyber Blue"),
    ROYAL_ROSE("royal_rose", "Royal Rose"),
    AURORA_TEAL("aurora_teal", "Aurora Teal");

    companion object {
        fun fromId(id: String): ThemeAccent = entries.firstOrNull { it.id == id } ?: COSMIC_VIOLET
    }
}

enum class VoiceLanguage(val id: String, val label: String, val localeTag: String) {
    HINGLISH_AUTO("hinglish_auto", "Hinglish (Auto)", "en-IN"),
    ENGLISH("english", "English", "en-US"),
    HINDI("hindi", "Hindi (हिन्दी)", "hi-IN");

    companion object {
        fun fromId(id: String): VoiceLanguage = entries.firstOrNull { it.id == id } ?: HINGLISH_AUTO
    }
}

enum class VoiceEmotion(val id: String, val label: String) {
    FRIENDLY_LOVELY("friendly", "Friendly & Lovely"),
    FUNNY_PLAYFUL("funny", "Funny & Playful"),
    CALM_CARING("calm", "Calm & Caring"),
    ENERGETIC_PRO("energetic", "Energetic & Expressive");

    companion object {
        fun fromId(id: String): VoiceEmotion = entries.firstOrNull { it.id == id } ?: FRIENDLY_LOVELY
    }
}

enum class VoicePipelineStage(val stepIndex: Int, val label: String) {
    IDLE(0, "Ready"),
    LISTEN(1, "1. Listen"),
    UNDERSTAND(2, "2. Understand"),
    RESPOND(3, "3. Respond"),
    SPEAK(4, "4. Speak")
}

enum class ApiProvider(
    val id: String,
    val title: String,
    val subtitle: String,
    val placeholder: String
) {
    GEMINI("gemini", "Gemini API Key", "AI voice & multimodal intelligence", "AIza... (Managed via Firebase AI Logic)"),
    GROQ("groq", "Groq API Keys", "Ultra-fast Llama text responses", "gsk_..."),
    ELEVENLABS("elevenlabs", "ElevenLabs API Key", "Premium human-like expressive voice", "sk_...")
}

enum class ConnectionState {
    NOT_CONFIGURED,
    SAVED,
    TESTING,
    CONNECTED,
    ERROR
}

data class ProviderConnectionUi(
    val provider: ApiProvider,
    val maskedPreview: String = "",
    val hasSavedKey: Boolean = false,
    val status: ConnectionState = ConnectionState.NOT_CONFIGURED,
    val statusMessage: String = "Not configured"
)

enum class QuickActionType(val id: String, val title: String, val subtitle: String) {
    MAKE_CALL("make_call", "Make a Call", "Dial or call contacts safely"),
    SEND_MESSAGE("send_message", "Send Message", "Compose SMS with confirmation"),
    OPEN_APPS("open_apps", "Open Apps", "Launch installed device apps"),
    SET_ALARM("set_alarm", "Set Alarm", "Schedule alarms & wakeups"),
    WEATHER("weather", "Weather", "Live forecast & conditions"),
    SEARCH_INTERNET("search_internet", "Search Internet", "Quick web search"),
    CREATE_IMAGE("create_image", "Create Image", "AI artwork studio"),
    TRANSLATE("translate", "Translate", "Hindi, English & Hinglish"),
    CALCULATOR("calculator", "Calculator", "Instant smart math pad"),
    NOTES("notes", "Notes", "Quick AI notes & drafts"),
    REMINDERS("reminders", "Reminders", "Set smart reminders"),
    CAMERA("camera", "Camera", "Launch device camera");

    companion object {
        fun fromId(id: String): QuickActionType? = entries.firstOrNull { it.id == id }
    }
}

data class QuickActionConfig(
    val type: QuickActionType,
    val enabled: Boolean = true,
    val pinned: Boolean = false,
    val orderIndex: Int = 0
)

data class AppSettings(
    val userName: String = "Friend",
    val userHandle: String = "@meena_explorer",
    val accountEmail: String = "local.companion@meena.pro",
    val isSignedIn: Boolean = true,
    val assistantName: String = "MEENA",
    val personality: String = "Friendly, lovely, funny & expressive",
    val voiceId: String = "Meena Natural (Female)",
    val voiceSpeed: Float = 1.02f,
    val voicePitch: Float = 1.08f,
    val voiceVolume: Float = 0.95f,
    val voiceLanguage: VoiceLanguage = VoiceLanguage.HINGLISH_AUTO,
    val voiceEmotion: VoiceEmotion = VoiceEmotion.FRIENDLY_LOVELY,
    val useElevenLabsWhenAvailable: Boolean = true,
    val useGroqFastChat: Boolean = true,
    val memoryEnabled: Boolean = true,
    val saveHistoryEnabled: Boolean = true,
    val privateModeEnabled: Boolean = false,
    val themeAccent: ThemeAccent = ThemeAccent.COSMIC_VIOLET,
    val orbStyle: OrbStyle = OrbStyle.CLASSIC_VIOLET_NEBULA,
    val selectedCharacter: MeenaCharacter = MeenaCharacter.ORIGINAL_MEENA,
    val showCharacterEverywhere: Boolean = true,
    val reduceMotion: Boolean = false
)

data class CapturedNotification(
    val id: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long
)

data class InstalledAppInfo(
    val label: String,
    val packageName: String
)
