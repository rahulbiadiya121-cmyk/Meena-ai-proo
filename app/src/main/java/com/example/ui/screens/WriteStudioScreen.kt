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
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.ui.components.FirebaseNoticeBanner
import com.example.ui.components.GlassCard
import com.example.ui.components.MarkdownMessageText
import com.example.ui.state.MeenaAppState
import com.example.ui.state.WriteUiState
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LocalMeenaGlow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun WriteStudioScreen(
    state: MeenaAppState,
    onBack: () -> Unit,
    onGenerate: (category: String, tone: String, prompt: String) -> Unit,
    onShare: (String) -> Unit,
    onSaveMemory: (String) -> Unit,
    onOpenConnections: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glow = LocalMeenaGlow.current
    val clipboard = LocalClipboardManager.current

    var category by remember { mutableStateOf("Notes") }
    var tone by remember { mutableStateOf("Friendly") }
    var prompt by remember { mutableStateOf("") }

    val categories = listOf("Notes", "Kotlin / Code", "Email", "Social Caption", "Summary")
    val tones = listOf("Friendly", "Professional", "Funny", "Poetic", "Concise")

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
                Text("Write Studio", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Text("Notes, code, emails & Hinglish content", style = MaterialTheme.typography.bodySmall, color = glow.secondaryGlow)
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Format", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) })
                    }
                }

                Text("Tone", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tones.forEach { t ->
                        FilterChip(selected = tone == t, onClick = { tone = t }, label = { Text(t) })
                    }
                }

                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("What should Meena write?") },
                    placeholder = { Text("e.g. Clean Kotlin coroutine retry helper or birthday wish in Hinglish…", color = TextMuted) },
                    minLines = 3,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { onGenerate(category, tone, prompt) },
                    enabled = prompt.isNotBlank() && state.writeStudio !is WriteUiState.Loading,
                    colors = ButtonDefaults.buttonColors(containerColor = glow.primaryGlow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.EditNote, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Draft with Meena")
                }
            }
        }

        when (val w = state.writeStudio) {
            is WriteUiState.Loading -> {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(color = glow.secondaryGlow, modifier = Modifier.size(26.dp))
                        Text("Composing $category…", color = TextPrimary)
                    }
                }
            }
            is WriteUiState.Streaming -> {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(w.engineTag, style = MaterialTheme.typography.labelSmall, color = glow.secondaryGlow)
                        MarkdownMessageText(markdown = w.text)
                    }
                }
            }
            is WriteUiState.Success -> {
                if (w.isPreviewFallback) {
                    FirebaseNoticeBanner(onOpenConnections = onOpenConnections)
                }
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(w.engineTag, style = MaterialTheme.typography.labelSmall, color = glow.secondaryGlow)
                        MarkdownMessageText(markdown = w.text)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(onClick = { clipboard.setText(AnnotatedString(w.text)) }) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy", tint = TextSecondary)
                            }
                            IconButton(onClick = { onShare(w.text) }) {
                                Icon(Icons.Rounded.Share, contentDescription = "Share", tint = TextSecondary)
                            }
                            IconButton(onClick = { onSaveMemory(w.text.take(160)) }) {
                                Icon(Icons.Rounded.BookmarkAdd, contentDescription = "Save to Memory", tint = TextSecondary)
                            }
                        }
                    }
                }
            }
            is WriteUiState.Error -> {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(w.message, color = ErrorRed)
                }
            }
            is WriteUiState.Idle -> {}
        }

        Spacer(modifier = Modifier.height(104.dp))
    }
}
