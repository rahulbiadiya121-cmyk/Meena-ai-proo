package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.QuickActionConfig
import com.example.domain.QuickActionType
import com.example.system.PhoneControlManager
import com.example.ui.components.GlassCard
import com.example.ui.components.Routes
import com.example.ui.state.MeenaAppState
import com.example.ui.state.WeatherUiState
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LocalMeenaGlow
import com.example.ui.theme.MidnightVoid
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun QuickActionsScreen(
    state: MeenaAppState,
    phoneControl: PhoneControlManager,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    onToggleEnabled: (QuickActionType) -> Unit,
    onTogglePinned: (QuickActionType) -> Unit,
    onMoveUp: (QuickActionType) -> Unit,
    onFetchWeather: (String) -> Unit,
    onAskMeenaTranslate: (String) -> Unit,
    onFeedback: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val glow = LocalMeenaGlow.current
    var customizeMode by remember { mutableStateOf(false) }

    var showWeatherDialog by remember { mutableStateOf(false) }
    var weatherCity by remember { mutableStateOf("Mumbai") }
    var showSearchDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showTranslateDialog by remember { mutableStateOf(false) }
    var translateText by remember { mutableStateOf("") }
    var showCalculatorDialog by remember { mutableStateOf(false) }

    val visibleActions = remember(state.quickActions, customizeMode) {
        if (customizeMode) state.quickActions else state.quickActions.filter { it.enabled }
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
                    Text("Quick Actions", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Text("12 customizable instant tools", style = MaterialTheme.typography.bodySmall, color = glow.secondaryGlow)
                }
            }
            OutlinedButton(onClick = { customizeMode = !customizeMode }) {
                Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (customizeMode) "Done" else "Customize")
            }
        }

        if (customizeMode) {
            visibleActions.forEach { cfg ->
                QuickActionCustomizeRow(
                    config = cfg,
                    onToggleEnabled = { onToggleEnabled(cfg.type) },
                    onTogglePinned = { onTogglePinned(cfg.type) },
                    onMoveUp = { onMoveUp(cfg.type) }
                )
            }
        } else {
            val rows = visibleActions.chunked(2)
            rows.forEach { pair ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    pair.forEach { cfg ->
                        QuickActionCard(
                            config = cfg,
                            onClick = {
                                when (cfg.type) {
                                    QuickActionType.MAKE_CALL,
                                    QuickActionType.SEND_MESSAGE,
                                    QuickActionType.OPEN_APPS,
                                    QuickActionType.SET_ALARM,
                                    QuickActionType.REMINDERS -> onNavigate(Routes.PHONE_CONTROL)
                                    QuickActionType.WEATHER -> {
                                        showWeatherDialog = true
                                        onFetchWeather(weatherCity)
                                    }
                                    QuickActionType.SEARCH_INTERNET -> showSearchDialog = true
                                    QuickActionType.CREATE_IMAGE -> onNavigate(Routes.IMAGE_GEN)
                                    QuickActionType.TRANSLATE -> showTranslateDialog = true
                                    QuickActionType.CALCULATOR -> showCalculatorDialog = true
                                    QuickActionType.NOTES -> onNavigate(Routes.WRITE)
                                    QuickActionType.CAMERA -> {
                                        phoneControl.openCamera().onFailure {
                                            onFeedback(it.message ?: "Camera unavailable")
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(104.dp))
    }

    if (showWeatherDialog) {
        AlertDialog(
            onDismissRequest = { showWeatherDialog = false },
            containerColor = MidnightVoid,
            title = { Text("Live Weather", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = weatherCity,
                        onValueChange = { weatherCity = it },
                        label = { Text("City Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    when (val w = state.weather) {
                        is WeatherUiState.Loading -> CircularProgressIndicator()
                        is WeatherUiState.Success -> {
                            Text(w.summary.locationName, style = MaterialTheme.typography.titleMedium, color = NeonCyan)
                            Text("${w.summary.temperatureC}°C • ${w.summary.conditionLabel}", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                            Text("Wind: ${w.summary.windSpeedKmh} km/h", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        is WeatherUiState.Error -> Text(w.message, color = ErrorRed)
                        is WeatherUiState.Idle -> {}
                    }
                }
            },
            confirmButton = {
                Button(onClick = { onFetchWeather(weatherCity) }) { Text("Refresh") }
            },
            dismissButton = {
                TextButton(onClick = { showWeatherDialog = false }) { Text("Close") }
            }
        )
    }

    if (showSearchDialog) {
        AlertDialog(
            onDismissRequest = { showSearchDialog = false },
            containerColor = MidnightVoid,
            title = { Text("Search Internet", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search query") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (searchQuery.isNotBlank()) {
                        phoneControl.searchWeb(searchQuery)
                        showSearchDialog = false
                    }
                }) { Text("Search") }
            },
            dismissButton = {
                TextButton(onClick = { showSearchDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showTranslateDialog) {
        AlertDialog(
            onDismissRequest = { showTranslateDialog = false },
            containerColor = MidnightVoid,
            title = { Text("Quick Translate (Hindi / English)", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = translateText,
                    onValueChange = { translateText = it },
                    label = { Text("Text to translate") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (translateText.isNotBlank()) {
                        showTranslateDialog = false
                        onAskMeenaTranslate("Translate this naturally between English and Hindi/Hinglish: $translateText")
                    }
                }) { Text("Translate with Meena") }
            },
            dismissButton = {
                TextButton(onClick = { showTranslateDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showCalculatorDialog) {
        CalculatorModal(onDismiss = { showCalculatorDialog = false })
    }
}

@Composable
private fun QuickActionCard(
    config: QuickActionConfig,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glow = LocalMeenaGlow.current
    GlassCard(
        onClick = onClick,
        contentPadding = 14.dp,
        modifier = modifier
            .height(112.dp)
            .testTag("quick_action_${config.type.id}")
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(glow.primaryGlow.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = iconForQuickAction(config.type),
                        contentDescription = config.type.title,
                        tint = glow.secondaryGlow,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (config.pinned) {
                    Icon(Icons.Rounded.PushPin, contentDescription = "Pinned", tint = NeonCyan, modifier = Modifier.size(16.dp))
                }
            }
            Column {
                Text(config.type.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1)
                Text(config.type.subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1)
            }
        }
    }
}

@Composable
private fun QuickActionCustomizeRow(
    config: QuickActionConfig,
    onToggleEnabled: () -> Unit,
    onTogglePinned: () -> Unit,
    onMoveUp: () -> Unit
) {
    val glow = LocalMeenaGlow.current
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(iconForQuickAction(config.type), contentDescription = null, tint = glow.secondaryGlow)
            Spacer(modifier = Modifier.width(10.dp))
            Text(config.type.title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, modifier = Modifier.weight(1f))
            IconButton(onClick = onMoveUp) {
                Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move up", tint = TextSecondary)
            }
            IconButton(onClick = onTogglePinned) {
                Icon(
                    imageVector = if (config.pinned) Icons.Rounded.PushPin else Icons.Outlined.PushPin,
                    contentDescription = "Pin",
                    tint = if (config.pinned) NeonCyan else TextMuted
                )
            }
            Switch(checked = config.enabled, onCheckedChange = { onToggleEnabled() })
        }
    }
}

@Composable
private fun CalculatorModal(onDismiss: () -> Unit) {
    val glow = LocalMeenaGlow.current
    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightVoid,
        title = { Text("Smart Calculator", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(expression.ifBlank { "0" }, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text(result, style = MaterialTheme.typography.headlineLarge, color = glow.secondaryGlow)
                val rows = listOf(
                    listOf("7", "8", "9", "/"),
                    listOf("4", "5", "6", "*"),
                    listOf("1", "2", "3", "-"),
                    listOf("C", "0", "=", "+")
                )
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { key ->
                            Button(
                                onClick = {
                                    when (key) {
                                        "C" -> {
                                            expression = ""
                                            result = "0"
                                        }
                                        "=" -> {
                                            result = evaluateSimpleMath(expression)
                                        }
                                        else -> expression += key
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (key == "=") glow.primaryGlow else glow.cardGlass
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(key, color = TextPrimary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

private fun evaluateSimpleMath(expr: String): String {
    return try {
        val clean = expr.replace(" ", "")
        val opIdx = clean.indexOfAny(charArrayOf('+', '-', '*', '/'), startIndex = 1)
        if (opIdx == -1) return clean.ifBlank { "0" }
        val a = clean.substring(0, opIdx).toDouble()
        val op = clean[opIdx]
        val b = clean.substring(opIdx + 1).toDouble()
        val ans = when (op) {
            '+' -> a + b
            '-' -> a - b
            '*' -> a * b
            '/' -> if (b == 0.0) return "Undefined" else a / b
            else -> 0.0
        }
        if (ans % 1.0 == 0.0) ans.toLong().toString() else "%.4f".format(ans).trimEnd('0').trimEnd('.')
    } catch (_: Exception) {
        "Error"
    }
}

private fun iconForQuickAction(type: QuickActionType): ImageVector = when (type) {
    QuickActionType.MAKE_CALL -> Icons.Outlined.Call
    QuickActionType.SEND_MESSAGE -> Icons.AutoMirrored.Rounded.Message
    QuickActionType.OPEN_APPS -> Icons.Outlined.Apps
    QuickActionType.SET_ALARM -> Icons.Outlined.Alarm
    QuickActionType.WEATHER -> Icons.Outlined.WbSunny
    QuickActionType.SEARCH_INTERNET -> Icons.Outlined.Search
    QuickActionType.CREATE_IMAGE -> Icons.Outlined.AutoAwesome
    QuickActionType.TRANSLATE -> Icons.Outlined.Translate
    QuickActionType.CALCULATOR -> Icons.Outlined.Calculate
    QuickActionType.NOTES -> Icons.Outlined.EditNote
    QuickActionType.REMINDERS -> Icons.Outlined.EventNote
    QuickActionType.CAMERA -> Icons.Outlined.CameraAlt
}
