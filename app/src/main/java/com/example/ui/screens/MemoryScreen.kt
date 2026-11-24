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
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.components.GlassCard
import com.example.ui.state.MeenaAppState
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LocalMeenaGlow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun MemoryScreen(
    state: MeenaAppState,
    onBack: () -> Unit,
    onToggleMemoryEnabled: (Boolean) -> Unit,
    onAddMemory: (String, String) -> Unit,
    onDeleteMemory: (Long) -> Unit,
    onClearAllMemories: () -> Unit,
    onClearAllHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glow = LocalMeenaGlow.current
    var newFact by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Preference") }

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
                Text("AI Memory System", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Text("Personalized context & privacy control", style = MaterialTheme.typography.bodySmall, color = glow.secondaryGlow)
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Psychology, contentDescription = null, tint = glow.secondaryGlow, modifier = Modifier.size(26.dp))
                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text("Enable AI Memory", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text("Allow Meena to remember preferences & key details", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Switch(
                    checked = state.settings.memoryEnabled,
                    onCheckedChange = onToggleMemoryEnabled
                )
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Add Memory Detail", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Preference", "Identity", "Context").forEach { cat ->
                        FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) })
                    }
                }
                OutlinedTextField(
                    value = newFact,
                    onValueChange = { newFact = it },
                    placeholder = { Text("e.g. I prefer concise Kotlin answers and Hinglish voice", color = TextMuted) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (newFact.isNotBlank()) {
                            onAddMemory(newFact, category)
                            newFact = ""
                        }
                    },
                    enabled = newFact.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = glow.primaryGlow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text("Save to Memory")
                }
            }
        }

        if (state.memories.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "No memories saved yet. Tell Meena things like \"My name is…\" or \"I like…\" or add a custom detail above.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        } else {
            Text("Remembered Facts (${state.memories.size})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            state.memories.forEach { mem ->
                GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mem.category.uppercase(), style = MaterialTheme.typography.labelSmall, color = glow.secondaryGlow)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(mem.factText, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        }
                        IconButton(onClick = { onDeleteMemory(mem.id) }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete memory", tint = ErrorRed)
                        }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onClearAllMemories, modifier = Modifier.weight(1f)) {
                Text("Clear Memories", color = ErrorRed)
            }
            OutlinedButton(onClick = onClearAllHistory, modifier = Modifier.weight(1f)) {
                Text("Clear Chats", color = ErrorRed)
            }
        }

        Spacer(modifier = Modifier.height(104.dp))
    }
}
