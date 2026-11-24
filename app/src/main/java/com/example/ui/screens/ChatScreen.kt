package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.ChatMessageEntity
import com.example.domain.ApiProvider
import com.example.ui.components.FirebaseNoticeBanner
import com.example.ui.components.GlassCard
import com.example.ui.components.MarkdownMessageText
import com.example.ui.state.MeenaAppState
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.LocalMeenaGlow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    state: MeenaAppState,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onRegenerate: () -> Unit,
    onNewConversation: () -> Unit,
    onClearChat: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onAttachImageUri: (String?) -> Unit,
    onToggleGroq: (Boolean) -> Unit,
    onSpeakAloud: (String) -> Unit,
    onSaveToMemory: (String) -> Unit,
    onOpenVoice: () -> Unit,
    onOpenConnections: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glow = LocalMeenaGlow.current
    val clipboard = LocalClipboardManager.current
    var inputText by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        onAttachImageUri(uri?.toString())
    }

    val filteredMessages = remember(state.messages, state.chat.searchQuery) {
        val q = state.chat.searchQuery.trim()
        if (q.isEmpty()) state.messages
        else state.messages.filter { it.text.contains(q, ignoreCase = true) }
    }

    LaunchedEffect(filteredMessages.size, state.chat.streamingText) {
        val target = (filteredMessages.size + if (state.chat.isStreaming) 1 else 0) - 1
        if (target >= 0) {
            listState.animateScrollToItem(target)
        }
    }

    val groqConnected = state.connections[ApiProvider.GROQ]?.hasSavedKey == true

    Column(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 720.dp)
            .padding(horizontal = 12.dp)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${state.settings.assistantName} Chat",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (state.chat.preferGroqEngine && groqConnected) "Groq Fast Cloud Active" else "Gemini Preview Mode",
                    style = MaterialTheme.typography.labelSmall,
                    color = glow.secondaryGlow
                )
            }
            IconButton(onClick = { showSearch = !showSearch }, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Outlined.Search, contentDescription = "Search chat", tint = TextPrimary)
            }
            IconButton(onClick = onNewConversation, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Outlined.AddComment, contentDescription = "New conversation", tint = TextPrimary)
            }
            IconButton(onClick = onClearChat, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Clear chat", tint = TextSecondary)
            }
        }

        // Engine Switcher Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            FilterChip(
                selected = !state.chat.preferGroqEngine || !groqConnected,
                onClick = { onToggleGroq(false) },
                label = { Text("Gemini AI") }
            )
            FilterChip(
                selected = state.chat.preferGroqEngine && groqConnected,
                onClick = { onToggleGroq(true) },
                label = { Text(if (groqConnected) "Groq Fast ⚡" else "Groq (Add Key)") }
            )
        }

        AnimatedVisibility(visible = showSearch) {
            OutlinedTextField(
                value = state.chat.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search messages…", color = TextMuted) },
                singleLine = true,
                trailingIcon = {
                    if (state.chat.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        }

        AnimatedVisibility(visible = state.chat.showPreviewNotice) {
            FirebaseNoticeBanner(
                onOpenConnections = onOpenConnections,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Message List
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (filteredMessages.isEmpty() && !state.chat.isStreaming) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = state.settings.selectedCharacter.greetingHinglish,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Ask in Hindi, English, or Hinglish — attach an image, generate code, or tap the mic for voice chat.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            items(filteredMessages, key = { it.id }) { msg ->
                ChatBubbleItem(
                    message = msg,
                    isLastAssistant = !msg.isUser && msg.id == filteredMessages.lastOrNull { !it.isUser }?.id,
                    onCopy = { clipboard.setText(AnnotatedString(msg.text)) },
                    onRegenerate = onRegenerate,
                    onSpeak = { onSpeakAloud(msg.text) },
                    onSaveMemory = { onSaveToMemory(msg.text.take(160)) }
                )
            }

            if (state.chat.isStreaming) {
                item(key = "streaming_bubble") {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "${state.settings.assistantName} • ${state.chat.streamingEngineTag}",
                                style = MaterialTheme.typography.labelSmall,
                                color = glow.secondaryGlow
                            )
                            val display = state.chat.streamingText.ifBlank { "Typing…" }
                            MarkdownMessageText(markdown = display)
                        }
                    }
                }
            }
        }

        // Attached Image Preview
        state.chat.attachedImageUri?.let { uri ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(GlassSurface)
                    .padding(8.dp)
            ) {
                AsyncImage(
                    model = uri,
                    contentDescription = "Attached image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Image attached", style = MaterialTheme.typography.bodySmall, color = TextPrimary, modifier = Modifier.weight(1f))
                IconButton(onClick = { onAttachImageUri(null) }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Remove attachment", tint = TextSecondary)
                }
            }
        }

        // Chat Input Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 96.dp, top = 4.dp)
        ) {
            IconButton(
                onClick = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(Icons.Outlined.AttachFile, contentDescription = "Attach image", tint = TextSecondary)
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Ask ${state.settings.assistantName} anything…", color = TextMuted) },
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    }
                ),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = GlassSurface.copy(alpha = 0.8f),
                    unfocusedContainerColor = GlassSurface.copy(alpha = 0.6f),
                    focusedBorderColor = glow.primaryGlow,
                    unfocusedBorderColor = glow.cardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_message_input")
            )

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        onSendMessage(inputText)
                        inputText = ""
                    } else {
                        onOpenVoice()
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(glow.primaryGlow, glow.secondaryGlow)))
                    .testTag("chat_send_or_mic_button")
            ) {
                Icon(
                    imageVector = if (inputText.isNotBlank()) Icons.AutoMirrored.Rounded.Send else Icons.Rounded.Mic,
                    contentDescription = if (inputText.isNotBlank()) "Send" else "Voice input",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun ChatBubbleItem(
    message: ChatMessageEntity,
    isLastAssistant: Boolean,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit,
    onSpeak: () -> Unit,
    onSaveMemory: () -> Unit
) {
    val glow = LocalMeenaGlow.current
    val timeText = remember(message.timestamp) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp))
    }

    Column(
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (message.isUser) {
            Surface(
                shape = RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp),
                color = glow.primaryGlow.copy(alpha = 0.28f),
                modifier = Modifier
                    .widthIn(max = 310.dp)
                    .border(1.dp, glow.primaryGlow.copy(alpha = 0.5f), RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    message.imageUri?.let { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = "User attached photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(text = message.text, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = timeText, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }
        } else {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
                contentPadding = 14.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = message.engineTag,
                            style = MaterialTheme.typography.labelSmall,
                            color = glow.secondaryGlow
                        )
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                    MarkdownMessageText(markdown = message.text)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onCopy, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy response", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onSpeak, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Rounded.VolumeUp, contentDescription = "Speak response", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onSaveMemory, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Rounded.BookmarkAdd, contentDescription = "Save to AI memory", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                        if (isLastAssistant) {
                            IconButton(onClick = onRegenerate, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "Regenerate response", tint = glow.secondaryGlow, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
