package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.ChatMessage
import com.example.ui.GeminiScreenState
import com.example.ui.GeminiUiState
import com.example.ui.GeminiViewModel
import com.example.ui.theme.BrightViolet
import com.example.ui.theme.CosmicBlack
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.MidnightVoid
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SoftBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            MyApplicationTheme {
                val viewModel: GeminiViewModel = viewModel()
                val state by viewModel.state.collectAsStateWithLifecycle()
                GeminiCompanionScreen(
                    state = state,
                    onSendPrompt = viewModel::sendPrompt,
                    onSelectModel = viewModel::setModel,
                    onClear = viewModel::clearConversation
                )
            }
        }
    }
}

@Composable
fun GeminiCompanionScreen(
    state: GeminiScreenState,
    onSendPrompt: (String) -> Unit,
    onSelectModel: (String) -> Unit,
    onClear: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    Scaffold(
        containerColor = CosmicBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(CosmicBlack, MidnightVoid, CosmicBlack)))
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = SoftBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "MEENA AI PRO",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = if (state.isKeyConfigured) "BuildConfig Key Ready" else "Add GEMINI_API_KEY in Secrets",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (state.isKeyConfigured) SuccessGreen else ErrorRed
                            )
                        }
                    }
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.testTag("clear_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Clear conversation",
                            tint = TextSecondary
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    listOf("gemini-3.8-flash", "gemini-3.5-flash-lite").forEach { modelId ->
                        FilterChip(
                            selected = state.selectedModel == modelId,
                            onClick = { onSelectModel(modelId) },
                            label = { Text(modelId) }
                        )
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.messages.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = GlassSurface,
                                border = BorderStroke(1.dp, NeonViolet.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Gemini API Repository Connected",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Requests use NetworkGeminiRepository via Retrofit with BuildConfig.GEMINI_API_KEY in the x-goog-api-key header.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    items(state.messages, key = { "${it.timestamp}_${it.isUser}_${it.text.hashCode()}" }) { msg ->
                        MessageCard(message = msg)
                    }

                    when (val ui = state.uiState) {
                        is GeminiUiState.Loading -> {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = SoftBlue,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Calling Gemini API…", color = TextSecondary)
                                }
                            }
                        }
                        is GeminiUiState.Error -> {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = ErrorRed.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = ui.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ErrorRed,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                        else -> {}
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ask Meena anything…", color = TextMuted) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank()) {
                                    onSendPrompt(inputText)
                                    inputText = ""
                                }
                            }
                        ),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = GlassSurface,
                            unfocusedContainerColor = GlassSurface.copy(alpha = 0.7f),
                            focusedBorderColor = NeonViolet,
                            unfocusedBorderColor = BrightViolet.copy(alpha = 0.3f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("prompt_input")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                onSendPrompt(inputText)
                                inputText = ""
                            }
                        },
                        enabled = inputText.isNotBlank() && state.uiState !is GeminiUiState.Loading,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(NeonViolet, SoftBlue)))
                            .testTag("send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Send prompt",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageCard(message: ChatMessage) {
    Column(
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (message.isUser) NeonViolet.copy(alpha = 0.25f) else GlassSurface,
            border = BorderStroke(
                width = 1.dp,
                color = if (message.isUser) NeonViolet.copy(alpha = 0.55f) else SoftBlue.copy(alpha = 0.3f)
            ),
            modifier = Modifier.widthIn(max = 520.dp)
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                modifier = Modifier.padding(14.dp)
            )
        }
    }
}
