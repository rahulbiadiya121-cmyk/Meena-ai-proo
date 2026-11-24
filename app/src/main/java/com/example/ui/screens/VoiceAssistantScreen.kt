package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.AppSettings
import com.example.domain.OrbState
import com.example.domain.VoiceEmotion
import com.example.domain.VoiceLanguage
import com.example.domain.VoicePipelineStage
import com.example.system.VoiceCommandIntent
import com.example.ui.components.FirebaseNoticeBanner
import com.example.ui.components.GlassCard
import com.example.ui.components.MeenaCharacterAvatar
import com.example.ui.components.MeenaOrb
import com.example.ui.state.MeenaAppState
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.LocalMeenaGlow
import com.example.ui.theme.MidnightVoid
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAssistantScreen(
    state: MeenaAppState,
    audioLevel: Float,
    hasAudioPermission: Boolean,
    onBack: () -> Unit,
    onStartListen: () -> Unit,
    onStopListen: () -> Unit,
    onInterrupt: () -> Unit,
    onSubmitTextVoice: (String) -> Unit,
    onExecuteCommand: (VoiceCommandIntent) -> Unit,
    onDismissCommand: () -> Unit,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onTestVoice: () -> Unit,
    onOpenConnections: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glow = LocalMeenaGlow.current
    var showVoiceSettingsSheet by remember { mutableStateOf(false) }
    var manualSpeechText by remember { mutableStateOf("") }

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onStartListen()
    }

    val voiceState = state.voice

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .widthIn(max = 620.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${state.settings.assistantName} Voice Live",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = "${state.settings.orbStyle.displayName} • ${state.settings.voiceLanguage.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = glow.secondaryGlow
                )
            }
            IconButton(
                onClick = { showVoiceSettingsSheet = true },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("voice_settings_button")
            ) {
                Icon(Icons.Rounded.Tune, contentDescription = "Voice Settings", tint = TextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4-Stage Voice Pipeline Stepper
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                VoicePipelineStage.LISTEN,
                VoicePipelineStage.UNDERSTAND,
                VoicePipelineStage.RESPOND,
                VoicePipelineStage.SPEAK
            ).forEach { stage ->
                val active = voiceState.stage == stage
                val passed = voiceState.stage.stepIndex > stage.stepIndex
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        active -> glow.primaryGlow.copy(alpha = 0.35f)
                        passed -> SuccessGreen.copy(alpha = 0.16f)
                        else -> GlassSurface.copy(alpha = 0.55f)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            width = 1.dp,
                            color = when {
                                active -> glow.secondaryGlow
                                passed -> SuccessGreen.copy(alpha = 0.5f)
                                else -> Color.White.copy(alpha = 0.08f)
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Text(
                        text = stage.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active || passed) TextPrimary else TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Animated Orb + Optional Character Badge
        Box(contentAlignment = Alignment.Center) {
            MeenaOrb(
                orbStyle = state.settings.orbStyle,
                orbState = voiceState.orbState,
                audioLevel = audioLevel,
                reduceMotion = state.settings.reduceMotion,
                size = 230.dp,
                onClick = {
                    when (voiceState.orbState) {
                        OrbState.SPEAKING -> onInterrupt()
                        OrbState.LISTENING -> onStopListen()
                        else -> if (hasAudioPermission) onStartListen() else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            )
            if (state.settings.showCharacterEverywhere) {
                Box(modifier = Modifier.align(Alignment.BottomEnd)) {
                    MeenaCharacterAvatar(
                        character = state.settings.selectedCharacter,
                        orbState = voiceState.orbState,
                        audioLevel = audioLevel,
                        reduceMotion = state.settings.reduceMotion,
                        size = 78.dp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Language & Emotion Pills
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = glow.secondaryGlow.copy(alpha = 0.16f)
            ) {
                Text(
                    text = "Lang: ${voiceState.detectedLanguage}",
                    style = MaterialTheme.typography.labelSmall,
                    color = glow.secondaryGlow,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = glow.primaryGlow.copy(alpha = 0.18f)
            ) {
                Text(
                    text = "Tone: ${voiceState.detectedEmotion}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = voiceState.statusBanner,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Live Transcript & Reply Card
        AnimatedVisibility(visible = voiceState.liveTranscript.isNotBlank() || voiceState.assistantReply.isNotBlank()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (voiceState.liveTranscript.isNotBlank()) {
                        Text(
                            text = "You: \"${voiceState.liveTranscript}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = glow.secondaryGlow,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (voiceState.assistantReply.isNotBlank()) {
                        Text(
                            text = voiceState.assistantReply,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                    }
                    if (voiceState.isPreviewNotice) {
                        FirebaseNoticeBanner(onOpenConnections = onOpenConnections)
                    }
                }
            }
        }

        // Voice-activated Phone Command Confirmation
        voiceState.pendingCommand?.let { cmd ->
            Spacer(modifier = Modifier.height(10.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Voice Action Ready",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonCyan
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { onExecuteCommand(cmd) },
                            colors = ButtonDefaults.buttonColors(containerColor = glow.primaryGlow)
                        ) {
                            Text("Confirm & Run")
                        }
                        OutlinedButton(onClick = onDismissCommand) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Primary Mic / Interrupt Controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            if (voiceState.isListening) listOf(NeonCyan, glow.primaryGlow)
                            else listOf(glow.primaryGlow, glow.secondaryGlow)
                        )
                    )
                    .clickable {
                        when {
                            voiceState.orbState == OrbState.SPEAKING -> onInterrupt()
                            voiceState.isListening -> onStopListen()
                            hasAudioPermission -> onStartListen()
                            else -> micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                    .testTag("voice_mic_toggle")
            ) {
                Icon(
                    imageVector = when {
                        voiceState.orbState == OrbState.SPEAKING || voiceState.isListening -> Icons.Rounded.Stop
                        else -> Icons.Rounded.Mic
                    },
                    contentDescription = "Voice Mic",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Hinglish / Hindi / English Voice Starters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "Namaste Meena, aaj ka plan kya hai?",
                "Tell me a funny Hinglish joke!",
                "Open Calculator",
                "Flashlight on"
            ).forEach { phrase ->
                AssistChip(
                    onClick = { onSubmitTextVoice(phrase) },
                    label = { Text(phrase, style = MaterialTheme.typography.labelMedium) },
                    leadingIcon = {
                        Icon(Icons.Rounded.GraphicEq, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = GlassSurface.copy(alpha = 0.7f),
                        labelColor = TextPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Type-to-Voice Fallback Input
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = manualSpeechText,
                onValueChange = { manualSpeechText = it },
                placeholder = { Text("Or type to speak with Meena…", color = TextMuted) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (manualSpeechText.isNotBlank()) {
                            onSubmitTextVoice(manualSpeechText)
                            manualSpeechText = ""
                        }
                    }
                ),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (manualSpeechText.isNotBlank()) {
                        onSubmitTextVoice(manualSpeechText)
                        manualSpeechText = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(glow.primaryGlow)
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Speak message", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(108.dp))
    }

    if (showVoiceSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showVoiceSettingsSheet = false },
            containerColor = MidnightVoid,
            contentColor = TextPrimary
        ) {
            VoiceSettingsSheetContent(
                settings = state.settings,
                onUpdateSettings = onUpdateSettings,
                onTestVoice = onTestVoice
            )
        }
    }
}

@Composable
fun VoiceSettingsSheetContent(
    settings: AppSettings,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onTestVoice: () -> Unit
) {
    val glow = LocalMeenaGlow.current
    val voices = listOf(
        "Meena Natural (Female)",
        "Aanya Expressive (Hinglish)",
        "Kavya Warm (Hindi)",
        "Nova Studio (English)"
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text("Voice & Personality Engine", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)

        Text("Voice Selection", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            voices.forEach { v ->
                FilterChip(
                    selected = settings.voiceId == v,
                    onClick = { onUpdateSettings { it.copy(voiceId = v) } },
                    label = { Text(v) }
                )
            }
        }

        Text("Language", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VoiceLanguage.entries.forEach { lang ->
                FilterChip(
                    selected = settings.voiceLanguage == lang,
                    onClick = { onUpdateSettings { it.copy(voiceLanguage = lang) } },
                    label = { Text(lang.label) }
                )
            }
        }

        Text("Emotion & Expression", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VoiceEmotion.entries.forEach { emo ->
                FilterChip(
                    selected = settings.voiceEmotion == emo,
                    onClick = { onUpdateSettings { it.copy(voiceEmotion = emo) } },
                    label = { Text(emo.label) }
                )
            }
        }

        LabeledSlider(
            label = "Voice Speed",
            valueText = "%.2fx".format(settings.voiceSpeed),
            value = settings.voiceSpeed,
            range = 0.6f..1.6f,
            onValueChange = { v -> onUpdateSettings { it.copy(voiceSpeed = v) } }
        )
        LabeledSlider(
            label = "Voice Pitch",
            valueText = "%.2f".format(settings.voicePitch),
            value = settings.voicePitch,
            range = 0.7f..1.4f,
            onValueChange = { v -> onUpdateSettings { it.copy(voicePitch = v) } }
        )
        LabeledSlider(
            label = "Voice Volume",
            valueText = "${(settings.voiceVolume * 100).toInt()}%",
            value = settings.voiceVolume,
            range = 0.2f..1.0f,
            onValueChange = { v -> onUpdateSettings { it.copy(voiceVolume = v) } }
        )

        Button(
            onClick = onTestVoice,
            colors = ButtonDefaults.buttonColors(containerColor = glow.primaryGlow),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("test_voice_button")
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Test Meena's Voice")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    val glow = LocalMeenaGlow.current
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Text(valueText, style = MaterialTheme.typography.labelMedium, color = glow.secondaryGlow)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = glow.secondaryGlow,
                activeTrackColor = glow.primaryGlow
            )
        )
    }
}
