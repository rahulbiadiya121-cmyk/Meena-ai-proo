package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.OrbState
import com.example.ui.components.GlassCard
import com.example.ui.components.MeenaCharacterAvatar
import com.example.ui.components.Routes
import com.example.ui.state.MeenaAppState
import com.example.ui.theme.CosmicBlack
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.LocalMeenaGlow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.sin

@Composable
fun HomeScreen(
    state: MeenaAppState,
    audioLevel: Float,
    hasAudioPermission: Boolean,
    onOpenDrawer: () -> Unit,
    onNavigate: (String) -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onSendPrompt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val glow = LocalMeenaGlow.current
    val haptic = LocalHapticFeedback.current
    var promptText by remember { mutableStateOf("") }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onStartVoice()
        } else {
            onNavigate(Routes.VOICE)
        }
    }

    val isListening = state.voice.isListening

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .widthIn(max = 620.dp)
    ) {
        // Top Header: Menu Left, Centered MEENA AI PRO Logo, Settings Right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onOpenDrawer,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(GlassSurface.copy(alpha = 0.7f))
                    .border(1.dp, glow.cardBorder, CircleShape)
                    .testTag("home_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Menu,
                    contentDescription = "Open menu",
                    tint = TextPrimary
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${state.settings.assistantName} AI PRO",
                    style = MaterialTheme.typography.titleLarge.copy(
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextPrimary
                )
                Text(
                    text = "Your Personal AI Companion",
                    style = MaterialTheme.typography.labelSmall,
                    color = glow.secondaryGlow
                )
            }

            IconButton(
                onClick = { onNavigate(Routes.SETTINGS) },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(GlassSurface.copy(alpha = 0.7f))
                    .border(1.dp, glow.cardBorder, CircleShape)
                    .testTag("home_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Open settings",
                    tint = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Large Animated Female AI Character in Center
        MeenaCharacterAvatar(
            character = state.settings.selectedCharacter,
            orbState = state.voice.orbState,
            audioLevel = audioLevel,
            reduceMotion = state.settings.reduceMotion,
            size = 200.dp,
            onClick = { onNavigate(Routes.CHARACTERS) }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Live Voice Waveform Animation
        LiveVoiceWaveform(
            active = isListening || state.voice.orbState == OrbState.SPEAKING,
            audioLevel = audioLevel,
            reduceMotion = state.settings.reduceMotion
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Circular Glowing Microphone Button + "Tap to Talk"
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(76.dp)
                .shadow(
                    elevation = if (isListening) 24.dp else 14.dp,
                    shape = CircleShape,
                    ambientColor = if (isListening) NeonCyan else glow.primaryGlow,
                    spotColor = if (isListening) NeonCyan else glow.primaryGlow
                )
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        if (isListening) listOf(NeonCyan, glow.primaryGlow)
                        else listOf(glow.primaryGlow, glow.secondaryGlow)
                    )
                )
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (isListening) {
                        onStopVoice()
                    } else if (hasAudioPermission) {
                        onStartVoice()
                    } else {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
                .testTag("home_mic_button")
        ) {
            Icon(
                imageVector = if (isListening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                contentDescription = if (isListening) "Stop listening" else "Tap to Talk",
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = when (state.voice.orbState) {
                OrbState.LISTENING -> state.voice.liveTranscript.ifBlank { "Listening… Tap to stop" }
                OrbState.PROCESSING -> "Meena is thinking…"
                OrbState.SPEAKING -> "Speaking… Tap mic to interrupt"
                OrbState.IDLE -> "Tap to Talk"
            },
            style = MaterialTheme.typography.titleMedium,
            color = if (isListening) NeonCyan else TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        AnimatedVisibility(visible = state.voice.assistantReply.isNotBlank()) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                contentPadding = 12.dp,
                onClick = { onNavigate(Routes.VOICE) }
            ) {
                Text(
                    text = state.voice.assistantReply,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Text Input "Ask Meena anything…" + Send Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = promptText,
                onValueChange = { promptText = it },
                placeholder = {
                    Text(
                        text = "Ask ${state.settings.assistantName} anything…",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (promptText.isNotBlank()) {
                            val q = promptText.trim()
                            promptText = ""
                            onSendPrompt(q)
                        }
                    }
                ),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = GlassSurface.copy(alpha = 0.78f),
                    unfocusedContainerColor = GlassSurface.copy(alpha = 0.62f),
                    focusedBorderColor = glow.primaryGlow,
                    unfocusedBorderColor = glow.cardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("home_ask_input")
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (promptText.isNotBlank()) {
                        val q = promptText.trim()
                        promptText = ""
                        onSendPrompt(q)
                    } else {
                        onNavigate(Routes.CHAT)
                    }
                },
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(glow.primaryGlow, glow.secondaryGlow)))
                    .testTag("home_send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = "Send to Meena",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Three Premium Feature Cards: Chat, Create Image, Write
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HomeFeatureCard(
                title = "Chat",
                subtitle = "Get instant answers.",
                icon = Icons.Outlined.ChatBubbleOutline,
                accentColor = glow.secondaryGlow,
                testTag = "feature_card_chat",
                onClick = { onNavigate(Routes.CHAT) },
                modifier = Modifier.weight(1f)
            )
            HomeFeatureCard(
                title = "Create Image",
                subtitle = "Generate AI images.",
                icon = Icons.Outlined.AutoAwesome,
                accentColor = glow.primaryGlow,
                testTag = "feature_card_image",
                onClick = { onNavigate(Routes.IMAGE_GEN) },
                modifier = Modifier.weight(1f)
            )
            HomeFeatureCard(
                title = "Write",
                subtitle = "Notes, code & content.",
                icon = Icons.Outlined.EditNote,
                accentColor = NeonCyan,
                testTag = "feature_card_write",
                onClick = { onNavigate(Routes.WRITE) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Companion Ecosystem Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HomeShortcutChip("Quick Actions", Icons.Outlined.Bolt) { onNavigate(Routes.QUICK_ACTIONS) }
            HomeShortcutChip("Characters", Icons.Outlined.Face) { onNavigate(Routes.CHARACTERS) }
            HomeShortcutChip("Phone Control", Icons.Outlined.PhoneAndroid) { onNavigate(Routes.PHONE_CONTROL) }
            HomeShortcutChip("AI Memory", Icons.Outlined.Psychology) { onNavigate(Routes.MEMORY) }
            HomeShortcutChip("Connections", Icons.Outlined.Hub) { onNavigate(Routes.CONNECTIONS) }
        }

        Spacer(modifier = Modifier.height(104.dp))
    }
}

@Composable
private fun HomeFeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .height(126.dp)
            .testTag(testTag),
        cornerRadius = 20.dp,
        contentPadding = 12.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.18f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HomeShortcutChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val glow = LocalMeenaGlow.current
    AssistChip(
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        leadingIcon = {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = glow.secondaryGlow)
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = GlassSurface.copy(alpha = 0.72f),
            labelColor = TextPrimary
        ),
        border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = glow.cardBorder),
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun LiveVoiceWaveform(
    active: Boolean,
    audioLevel: Float,
    reduceMotion: Boolean
) {
    val glow = LocalMeenaGlow.current
    val infinite = rememberInfiniteTransition(label = "waveform")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (reduceMotion) 4000 else 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(26.dp)
    ) {
        for (i in 0 until 13) {
            val baseHeight = if (active) {
                val wave = ((sin(phase + i * 0.65f) + 1f) * 0.5f)
                val amp = (0.35f + audioLevel.coerceIn(0f, 1f) * 0.65f)
                (6f + 18f * wave * amp).dp
            } else {
                val resting = 5f + (1f - kotlin.math.abs(i - 6) / 6f) * 5f
                resting.dp
            }
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(baseHeight)
                    .clip(CircleShape)
                    .background(
                        if (active) Brush.verticalGradient(listOf(NeonCyan, glow.primaryGlow))
                        else Brush.verticalGradient(listOf(glow.primaryGlow.copy(alpha = 0.45f), CosmicBlack))
                    )
            )
        }
    }
}
