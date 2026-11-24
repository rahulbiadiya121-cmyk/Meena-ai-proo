package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.MeenaCharacter
import com.example.domain.OrbState
import com.example.ui.components.GlassCard
import com.example.ui.components.MeenaCharacterAvatar
import com.example.ui.state.MeenaAppState
import com.example.ui.theme.LocalMeenaGlow
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CharactersScreen(
    state: MeenaAppState,
    onBack: () -> Unit,
    onSelectCharacter: (MeenaCharacter) -> Unit,
    onTestGreeting: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val glow = LocalMeenaGlow.current
    var previewChar by remember(state.settings.selectedCharacter) {
        mutableStateOf(state.settings.selectedCharacter)
    }
    var previewState by remember { mutableStateOf(OrbState.IDLE) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .widthIn(max = 640.dp)
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
                Text("AI Character Studio", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Text("Choose your personal companion persona", style = MaterialTheme.typography.bodySmall, color = glow.secondaryGlow)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Live Character Stage
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                MeenaCharacterAvatar(
                    character = previewChar,
                    orbState = previewState,
                    audioLevel = if (previewState == OrbState.LISTENING) 0.7f else 0.4f,
                    reduceMotion = state.settings.reduceMotion,
                    size = 186.dp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(previewChar.displayName, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                Text(previewChar.roleTag, style = MaterialTheme.typography.labelMedium, color = glow.secondaryGlow)
                Spacer(modifier = Modifier.height(6.dp))
                Text(previewChar.bio, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(modifier = Modifier.height(10.dp))

                // Animation State Previewer (Idle / Listening / Speaking)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        OrbState.IDLE to "Idle",
                        OrbState.LISTENING to "Listening",
                        OrbState.SPEAKING to "Speaking"
                    ).forEach { (st, label) ->
                        FilterChip(
                            selected = previewState == st,
                            onClick = { previewState = st },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { onSelectCharacter(previewChar) },
                        colors = ButtonDefaults.buttonColors(containerColor = glow.primaryGlow),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("select_character_button")
                    ) {
                        val isActive = state.settings.selectedCharacter == previewChar
                        Text(if (isActive) "Active Companion ✓" else "Set Active Character")
                    }
                    Button(
                        onClick = {
                            previewState = OrbState.SPEAKING
                            onTestGreeting(previewChar.greetingHinglish)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = glow.secondaryGlow.copy(alpha = 0.25f)),
                        modifier = Modifier.weight(0.75f)
                    ) {
                        Text("Hear Voice", color = TextPrimary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Character Roster
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MeenaCharacter.entries.forEach { character ->
                val isSelected = state.settings.selectedCharacter == character
                val isPreviewing = previewChar == character
                GlassCard(
                    onClick = { previewChar = character },
                    contentPadding = 12.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isPreviewing) Modifier.border(
                                1.5.dp,
                                glow.secondaryGlow,
                                RoundedCornerShape(20.dp)
                            ) else Modifier
                        )
                        .testTag("character_card_${character.id}")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = character.imageRes),
                            contentDescription = character.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = character.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Active",
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(character.roleTag, style = MaterialTheme.typography.labelSmall, color = glow.secondaryGlow)
                            Text(character.bio, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 2)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(104.dp))
    }
}
