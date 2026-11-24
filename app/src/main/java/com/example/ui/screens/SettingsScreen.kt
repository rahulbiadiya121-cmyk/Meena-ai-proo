package com.example.ui.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.AppSettings
import com.example.domain.OrbState
import com.example.domain.OrbStyle
import com.example.domain.ThemeAccent
import com.example.domain.VoiceLanguage
import com.example.ui.components.GlassCard
import com.example.ui.components.MeenaOrb
import com.example.ui.components.Routes
import com.example.ui.state.MeenaAppState
import com.example.ui.theme.AuroraTeal
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LocalMeenaGlow
import com.example.ui.theme.MidnightVoid
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.RoyalRose
import com.example.ui.theme.SoftBlue
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: MeenaAppState,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onTestVoice: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glow = LocalMeenaGlow.current
    val settings = state.settings

    var editFieldDialog by remember { mutableStateOf<String?>(null) }
    var editFieldValue by remember { mutableStateOf("") }
    var showVoiceSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var infoModalTitle by remember { mutableStateOf<String?>(null) }
    var infoModalBody by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .widthIn(max = 680.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Column {
                Text("Control Center", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Text("Complete MEENA AI PRO configuration", style = MaterialTheme.typography.bodySmall, color = glow.secondaryGlow)
            }
        }

        // 1. YOU
        SettingsSectionHeader("YOU")
        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
            Column {
                SettingsActionRow(
                    title = "Your Name",
                    subtitle = settings.userName,
                    icon = Icons.Outlined.Person,
                    onClick = {
                        editFieldValue = settings.userName
                        editFieldDialog = "userName"
                    }
                )
                SectionDivider()
                SettingsToggleRow(
                    title = "Private Session Mode",
                    subtitle = "Pause saving conversations and memories",
                    checked = settings.privateModeEnabled,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(privateModeEnabled = v) } }
                )
                SectionDivider()
                SettingsToggleRow(
                    title = "Save Chat History",
                    subtitle = "Keep conversation logs locally",
                    checked = settings.saveHistoryEnabled,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(saveHistoryEnabled = v) } }
                )
            }
        }

        // 2. ACCOUNT
        SettingsSectionHeader("ACCOUNT")
        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
            Column {
                SettingsActionRow(
                    title = "Account Information",
                    subtitle = settings.accountEmail,
                    icon = Icons.Outlined.Info,
                    onClick = {
                        infoModalTitle = "Account Information"
                        infoModalBody = "Name: ${settings.userName}\nUsername: ${settings.userHandle}\nEmail: ${settings.accountEmail}\nStorage: Encrypted Local Vault & Room DB"
                    }
                )
                SectionDivider()
                SettingsActionRow(
                    title = "Username",
                    subtitle = settings.userHandle,
                    icon = Icons.Outlined.Person,
                    onClick = {
                        editFieldValue = settings.userHandle
                        editFieldDialog = "userHandle"
                    }
                )
                SectionDivider()
                SettingsActionRow(
                    title = "Log Out",
                    subtitle = "Lock session and return to companion welcome",
                    icon = Icons.Outlined.Logout,
                    onClick = onSignOut
                )
                SectionDivider()
                SettingsActionRow(
                    title = "Delete Account",
                    subtitle = "Erase profile, chats, memories & encrypted keys",
                    icon = Icons.Outlined.DeleteForever,
                    tint = ErrorRed,
                    onClick = { showDeleteConfirm = true }
                )
            }
        }

        // 3. YOUR ASSISTANT
        SettingsSectionHeader("YOUR ASSISTANT")
        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
            Column {
                SettingsActionRow(
                    title = "Assistant Name",
                    subtitle = settings.assistantName,
                    icon = Icons.Outlined.Face,
                    onClick = {
                        editFieldValue = settings.assistantName
                        editFieldDialog = "assistantName"
                    }
                )
                SectionDivider()
                SettingsActionRow(
                    title = "Personality",
                    subtitle = settings.personality,
                    icon = Icons.Outlined.Psychology,
                    onClick = {
                        editFieldValue = settings.personality
                        editFieldDialog = "personality"
                    }
                )
                SectionDivider()
                Text("Primary Language", style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                    VoiceLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = settings.voiceLanguage == lang,
                            onClick = { onUpdateSettings { it.copy(voiceLanguage = lang) } },
                            label = { Text(lang.label) }
                        )
                    }
                }
                SectionDivider()
                SettingsActionRow(
                    title = "Voice Engine & Prosody",
                    subtitle = "${settings.voiceId} • ${settings.voiceEmotion.label}",
                    icon = Icons.Outlined.RecordVoiceOver,
                    onClick = { showVoiceSheet = true }
                )
            }
        }

        // 4. AI SETTINGS
        SettingsSectionHeader("AI SETTINGS")
        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
            Column {
                SettingsActionRow(
                    title = "Connections & API Keys",
                    subtitle = "Gemini, Groq & ElevenLabs Keystore vault",
                    icon = Icons.Outlined.Hub,
                    onClick = { onNavigate(Routes.CONNECTIONS) }
                )
                SectionDivider()
                SettingsToggleRow(
                    title = "Use Groq Fast Chat When Key Saved",
                    subtitle = "Low-latency Llama 3.3 responses",
                    checked = settings.useGroqFastChat,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(useGroqFastChat = v) } }
                )
                SectionDivider()
                SettingsToggleRow(
                    title = "Use ElevenLabs Natural Voice",
                    subtitle = "Prefer ElevenLabs studio voice when connected",
                    checked = settings.useElevenLabsWhenAvailable,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(useElevenLabsWhenAvailable = v) } }
                )
                SectionDivider()
                SettingsActionRow(
                    title = "Chats & AI Memory",
                    subtitle = "${state.memories.size} remembered facts • ${state.conversations.size} chats",
                    icon = Icons.Outlined.Psychology,
                    onClick = { onNavigate(Routes.MEMORY) }
                )
                SectionDivider()
                SettingsActionRow(
                    title = "Clear Conversation History",
                    subtitle = "Delete all saved chat threads",
                    icon = Icons.Outlined.DeleteForever,
                    tint = ErrorRed,
                    onClick = onClearHistory
                )
            }
        }

        // 5. APP SETTINGS (Look & Feel, Theme, 8-Orb Picker, Character, Reduce Motion)
        SettingsSectionHeader("APP SETTINGS")
        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Theme Accent", style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.padding(horizontal = 6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ThemeAccent.entries.forEach { accent ->
                        val swatchColor = when (accent) {
                            ThemeAccent.COSMIC_VIOLET -> NeonViolet
                            ThemeAccent.CYBER_BLUE -> SoftBlue
                            ThemeAccent.ROYAL_ROSE -> RoyalRose
                            ThemeAccent.AURORA_TEAL -> AuroraTeal
                        }
                        val selected = settings.themeAccent == accent
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onUpdateSettings { it.copy(themeAccent = accent) } }
                                .border(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) swatchColor else Color.White.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(swatchColor)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(accent.displayName, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                        }
                    }
                }

                SectionDivider()

                Text("Animated Orb Style (8 Styles)", style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.padding(horizontal = 6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OrbStyle.entries.forEach { orb ->
                        val selected = settings.orbStyle == orb
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(104.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onUpdateSettings { it.copy(orbStyle = orb) } }
                                .border(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) glow.secondaryGlow else Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(8.dp)
                                .testTag("orb_option_${orb.id}")
                        ) {
                            MeenaOrb(
                                orbStyle = orb,
                                orbState = if (selected) OrbState.SPEAKING else OrbState.IDLE,
                                reduceMotion = settings.reduceMotion,
                                size = 62.dp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = orb.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) glow.secondaryGlow else TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }

                SectionDivider()
                SettingsActionRow(
                    title = "Home Character",
                    subtitle = settings.selectedCharacter.displayName,
                    icon = Icons.Outlined.Face,
                    onClick = { onNavigate(Routes.CHARACTERS) }
                )
                SectionDivider()
                SettingsToggleRow(
                    title = "Show Character Everywhere",
                    subtitle = "Display companion avatar inside Voice Live & navigation",
                    checked = settings.showCharacterEverywhere,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(showCharacterEverywhere = v) } }
                )
                SectionDivider()
                SettingsToggleRow(
                    title = "Reduce Motion",
                    subtitle = "Minimize floating & high-speed orbital animations",
                    checked = settings.reduceMotion,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(reduceMotion = v) } }
                )
            }
        }

        // 6. PHONE CONTROL
        SettingsSectionHeader("PHONE CONTROL")
        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
            Column {
                SettingsActionRow(
                    title = "Quick Actions",
                    subtitle = "Customize 12 smart shortcuts",
                    icon = Icons.Outlined.Bolt,
                    onClick = { onNavigate(Routes.QUICK_ACTIONS) }
                )
                SectionDivider()
                SettingsActionRow(
                    title = "Permissions, Calling, SMS, Apps & Notifications",
                    subtitle = "Open complete Phone Control Center",
                    icon = Icons.Outlined.PhoneAndroid,
                    onClick = { onNavigate(Routes.PHONE_CONTROL) }
                )
            }
        }

        // 7. ABOUT
        SettingsSectionHeader("ABOUT")
        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
            Column {
                SettingsActionRow(
                    title = "Privacy Policy",
                    subtitle = "Zero unnecessary tracking, local Keystore protection",
                    icon = Icons.Outlined.Policy,
                    onClick = {
                        infoModalTitle = "Privacy Policy"
                        infoModalBody = "MEENA AI PRO stores your API keys encrypted inside Android Keystore (AES-256-GCM). Conversations and memories stay on your device unless you enable third-party AI engines like Groq or ElevenLabs using your own key. Phone Control actions always require your explicit confirmation."
                    }
                )
                SectionDivider()
                SettingsActionRow(
                    title = "Terms & Conditions",
                    subtitle = "Companion usage & safety guidelines",
                    icon = Icons.Outlined.Info,
                    onClick = {
                        infoModalTitle = "Terms & Conditions"
                        infoModalBody = "MEENA AI PRO is your personal AI companion for productivity, creativity, and device shortcuts. Always review sensitive actions such as phone calls and SMS before confirming."
                    }
                )
                SectionDivider()
                SettingsActionRow(
                    title = "App Version",
                    subtitle = "v2.4.0 ULTRA MAX PRO",
                    icon = Icons.Outlined.Info,
                    onClick = {}
                )
                SectionDivider()
                SettingsActionRow(
                    title = "Support",
                    subtitle = "Help & diagnostics",
                    icon = Icons.Outlined.SupportAgent,
                    onClick = {
                        infoModalTitle = "MEENA AI PRO Support"
                        infoModalBody = "Need help? Check Connections to verify your Groq or ElevenLabs keys, or visit Phone Control to inspect Android permissions and Notification Access."
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(104.dp))
    }

    // Field Edit Dialog
    editFieldDialog?.let { field ->
        AlertDialog(
            onDismissRequest = { editFieldDialog = null },
            containerColor = MidnightVoid,
            title = { Text("Update ${field.replaceFirstChar { it.uppercase() }}", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = editFieldValue,
                    onValueChange = { editFieldValue = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = editFieldValue.trim()
                        if (trimmed.isNotEmpty()) {
                            onUpdateSettings { s ->
                                when (field) {
                                    "userName" -> s.copy(userName = trimmed)
                                    "userHandle" -> s.copy(userHandle = trimmed)
                                    "assistantName" -> s.copy(assistantName = trimmed)
                                    "personality" -> s.copy(personality = trimmed)
                                    else -> s
                                }
                            }
                        }
                        editFieldDialog = null
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editFieldDialog = null }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = MidnightVoid,
            title = { Text("Delete Account & Data?", color = ErrorRed) },
            text = { Text("This permanently erases your local profile, all conversation history, AI memories, generated image history, and encrypted API keys.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) { Text("Delete Everything") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    infoModalTitle?.let { title ->
        AlertDialog(
            onDismissRequest = { infoModalTitle = null },
            containerColor = MidnightVoid,
            title = { Text(title, color = TextPrimary) },
            text = { Text(infoModalBody, color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { infoModalTitle = null }) { Text("Close") }
            }
        )
    }

    if (showVoiceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showVoiceSheet = false },
            containerColor = MidnightVoid,
            contentColor = TextPrimary
        ) {
            VoiceSettingsSheetContent(
                settings = settings,
                onUpdateSettings = onUpdateSettings,
                onTestVoice = onTestVoice
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    val glow = LocalMeenaGlow.current
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = glow.secondaryGlow,
        modifier = Modifier.padding(start = 6.dp, top = 4.dp)
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(color = Color.White.copy(alpha = 0.07f), modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color = TextPrimary,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = tint)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = TextMuted)
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val glow = LocalMeenaGlow.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = glow.primaryGlow
            )
        )
    }
}
