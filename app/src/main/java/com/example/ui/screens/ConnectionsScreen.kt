package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.domain.ApiProvider
import com.example.domain.ConnectionState
import com.example.domain.ProviderConnectionUi
import com.example.ui.components.GlassCard
import com.example.ui.state.MeenaAppState
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LocalMeenaGlow
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun ConnectionsScreen(
    state: MeenaAppState,
    onBack: () -> Unit,
    onSaveKey: (ApiProvider, String) -> Unit,
    onClearKey: (ApiProvider) -> Unit,
    onTestConnection: (ApiProvider) -> Unit,
    modifier: Modifier = Modifier
) {
    val glow = LocalMeenaGlow.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .widthIn(max = 660.dp),
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
                Text("AI Connections", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Text("Hardware-backed Android Keystore vault", style = MaterialTheme.typography.bodySmall, color = glow.secondaryGlow)
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "All API credentials are encrypted using AES-256-GCM in the Android Keystore and never logged or exposed in plain text.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        ApiProvider.entries.forEach { provider ->
            val ui = state.connections[provider] ?: ProviderConnectionUi(provider)
            ConnectionProviderCard(
                ui = ui,
                onSave = { key -> onSaveKey(provider, key) },
                onClear = { onClearKey(provider) },
                onTest = { onTestConnection(provider) }
            )
        }

        Spacer(modifier = Modifier.height(104.dp))
    }
}

@Composable
private fun ConnectionProviderCard(
    ui: ProviderConnectionUi,
    onSave: (String) -> Unit,
    onClear: () -> Unit,
    onTest: () -> Unit
) {
    val glow = LocalMeenaGlow.current
    var keyInput by remember(ui.provider) { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }

    val (icon, accent) = when (ui.provider) {
        ApiProvider.GEMINI -> Icons.Rounded.AutoAwesome to glow.primaryGlow
        ApiProvider.GROQ -> Icons.Rounded.Bolt to NeonCyan
        ApiProvider.ELEVENLABS -> Icons.Rounded.GraphicEq to glow.secondaryGlow
    }

    val statusColor = when (ui.status) {
        ConnectionState.CONNECTED -> SuccessGreen
        ConnectionState.SAVED -> glow.secondaryGlow
        ConnectionState.TESTING -> WarningAmber
        ConnectionState.ERROR -> ErrorRed
        ConnectionState.NOT_CONFIGURED -> TextMuted
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("connection_card_${ui.provider.id}")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.2f))
                ) {
                    Icon(icon, contentDescription = null, tint = accent)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(ui.provider.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(ui.provider.subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.16f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        if (ui.status == ConnectionState.TESTING) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = statusColor)
                            Spacer(modifier = Modifier.width(4.dp))
                        } else if (ui.status == ConnectionState.CONNECTED) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = statusColor, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                        } else if (ui.status == ConnectionState.ERROR) {
                            Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = statusColor, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = ui.status.name.replace('_', ' '),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor
                        )
                    }
                }
            }

            if (ui.hasSavedKey) {
                Text(
                    text = "Stored Key: ${ui.maskedPreview}",
                    fontFamily = MonoFontFamily,
                    style = MaterialTheme.typography.bodySmall,
                    color = glow.secondaryGlow
                )
            }

            Text(
                text = ui.statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = if (ui.status == ConnectionState.ERROR) ErrorRed else TextSecondary
            )

            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                placeholder = { Text(ui.provider.placeholder, color = TextMuted) },
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(
                            imageVector = if (showKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (showKey) "Hide key" else "Show key",
                            tint = TextSecondary
                        )
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        if (keyInput.isNotBlank()) {
                            onSave(keyInput)
                            keyInput = ""
                        }
                    },
                    enabled = keyInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = glow.primaryGlow),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
                OutlinedButton(
                    onClick = onTest,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Test")
                }
                if (ui.hasSavedKey) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove key", tint = ErrorRed)
                    }
                }
            }
        }
    }
}
