package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.FirebaseNoticeBanner
import com.example.ui.components.GlassCard
import com.example.ui.state.ImageGenUiState
import com.example.ui.state.MeenaAppState
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LocalMeenaGlow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ImageGenScreen(
    state: MeenaAppState,
    onBack: () -> Unit,
    onGenerate: (prompt: String, style: String, quality: String, aspectRatio: String) -> Unit,
    onDeleteImage: (Long) -> Unit,
    onSharePrompt: (String) -> Unit,
    onOpenConnections: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glow = LocalMeenaGlow.current
    var prompt by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf("Cyberpunk Neon") }
    var selectedQuality by remember { mutableStateOf("Ultra 2K") }
    var selectedRatio by remember { mutableStateOf("1:1") }

    val styles = listOf("Cyberpunk Neon", "Cinematic 3D", "Anime Luxe", "Photorealistic", "Digital Oil", "Minimalist Vector")
    val qualities = listOf("Standard 1K", "Ultra 2K", "Studio 4K")
    val ratios = listOf("1:1", "16:9", "3:4", "9:16")

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
                Text("AI Image Studio", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Text("Craft art with styles, quality & prompt tuning", style = MaterialTheme.typography.bodySmall, color = glow.secondaryGlow)
            }
        }

        FirebaseNoticeBanner(onOpenConnections = onOpenConnections)

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Describe the image you want Meena to create") },
                    placeholder = { Text("A futuristic cosmic library glowing with neon violet crystals…", color = TextMuted) },
                    minLines = 3,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("image_prompt_input")
                )

                Text("Art Style", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    styles.forEach { s ->
                        FilterChip(
                            selected = selectedStyle == s,
                            onClick = { selectedStyle = s },
                            label = { Text(s) }
                        )
                    }
                }

                Text("Quality & Aspect Ratio", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    qualities.forEach { q ->
                        FilterChip(
                            selected = selectedQuality == q,
                            onClick = { selectedQuality = q },
                            label = { Text(q) }
                        )
                    }
                    ratios.forEach { r ->
                        FilterChip(
                            selected = selectedRatio == r,
                            onClick = { selectedRatio = r },
                            label = { Text(r) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            if (prompt.isNotBlank()) {
                                prompt = "$prompt, ultra-detailed $selectedStyle, volumetric neon violet lighting, 8k octane render"
                            }
                        },
                        modifier = Modifier.weight(0.85f)
                    ) {
                        Text("Enhance")
                    }
                    Button(
                        onClick = { onGenerate(prompt, selectedStyle, selectedQuality, selectedRatio) },
                        enabled = prompt.isNotBlank() && state.imageGen !is ImageGenUiState.Loading,
                        colors = ButtonDefaults.buttonColors(containerColor = glow.primaryGlow),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("generate_image_button")
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate Image")
                    }
                }
            }
        }

        when (val imgState = state.imageGen) {
            is ImageGenUiState.Loading -> {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(color = glow.secondaryGlow, modifier = Modifier.size(28.dp))
                        Text("Synthesizing prompt & preparing Gemini Nano Banana canvas…", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    }
                }
            }
            is ImageGenUiState.PreviewNotice -> {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Firebase AI Logic Required for Live Rendering", style = MaterialTheme.typography.titleMedium, color = NeonCyan)
                        Text(imgState.notice, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text("Enhanced Prompt Saved:", style = MaterialTheme.typography.labelSmall, color = glow.secondaryGlow)
                        Text(imgState.enhancedPrompt, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                    }
                }
            }
            is ImageGenUiState.Error -> {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(imgState.message, color = ErrorRed, style = MaterialTheme.typography.bodyMedium)
                }
            }
            is ImageGenUiState.Idle -> {}
        }

        if (state.generatedImages.isNotEmpty()) {
            Text("Image Prompt History", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            state.generatedImages.forEach { item ->
                GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(item.prompt, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                        Text("${item.style} • ${item.quality} • ${item.aspectRatio}", style = MaterialTheme.typography.labelSmall, color = glow.secondaryGlow)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(onClick = { prompt = item.prompt }) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Remix prompt", tint = TextSecondary)
                            }
                            IconButton(onClick = { onSharePrompt(item.prompt) }) {
                                Icon(Icons.Rounded.Share, contentDescription = "Share prompt", tint = TextSecondary)
                            }
                            IconButton(onClick = { onDeleteImage(item.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = ErrorRed)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(104.dp))
    }
}
