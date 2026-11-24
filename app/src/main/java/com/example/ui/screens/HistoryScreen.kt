package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.GlassCard
import com.example.ui.state.MeenaAppState
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LocalMeenaGlow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    state: MeenaAppState,
    onBack: () -> Unit,
    onOpenConversation: (Long) -> Unit,
    onNewChat: () -> Unit,
    onDeleteConversation: (Long) -> Unit,
    onClearAllHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glow = LocalMeenaGlow.current
    var query by remember { mutableStateOf("") }

    val filtered = remember(state.conversations, query) {
        val q = query.trim()
        if (q.isEmpty()) state.conversations
        else state.conversations.filter {
            it.title.contains(q, ignoreCase = true) || it.lastPreview.contains(q, ignoreCase = true)
        }
    }

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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Column {
                    Text("Conversation History", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Text("${state.conversations.size} saved threads", style = MaterialTheme.typography.bodySmall, color = glow.secondaryGlow)
                }
            }
            Row {
                IconButton(onClick = onNewChat) {
                    Icon(Icons.Rounded.AddComment, contentDescription = "New Chat", tint = glow.secondaryGlow)
                }
                if (state.conversations.isNotEmpty()) {
                    IconButton(onClick = onClearAllHistory) {
                        Icon(Icons.Outlined.DeleteSweep, contentDescription = "Clear all history", tint = ErrorRed)
                    }
                }
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search past conversations…", color = TextMuted) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        if (filtered.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, tint = glow.secondaryGlow, modifier = Modifier.size(36.dp))
                    Text("No conversations yet", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text("Start chatting with ${state.settings.assistantName} to build your companion history.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Button(
                        onClick = onNewChat,
                        colors = ButtonDefaults.buttonColors(containerColor = glow.primaryGlow)
                    ) {
                        Text("Start New Chat")
                    }
                }
            }
        } else {
            filtered.forEach { conv ->
                val dateLabel = remember(conv.updatedAt) {
                    SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault()).format(Date(conv.updatedAt))
                }
                GlassCard(
                    onClick = { onOpenConversation(conv.id) },
                    contentPadding = 14.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = conv.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = conv.lastPreview,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${conv.engineUsed} • $dateLabel",
                                style = MaterialTheme.typography.labelSmall,
                                color = glow.secondaryGlow
                            )
                        }
                        IconButton(onClick = { onDeleteConversation(conv.id) }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete conversation", tint = ErrorRed)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(104.dp))
    }
}
