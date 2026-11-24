package com.example.ui.screens

import android.Manifest
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.system.MeenaNotificationListenerService
import com.example.system.PhoneControlManager
import com.example.ui.components.GlassCard
import com.example.ui.state.MeenaAppState
import com.example.ui.theme.LocalMeenaGlow
import com.example.ui.theme.MidnightVoid
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PhoneControlScreen(
    state: MeenaAppState,
    phoneControl: PhoneControlManager,
    onBack: () -> Unit,
    onToggleFlashlight: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onOpenSetting: (String) -> Unit,
    onRefreshApps: () -> Unit,
    onFeedback: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val glow = LocalMeenaGlow.current
    val notifications by MeenaNotificationListenerService.notifications.collectAsStateWithLifecycle()
    val hasNotifAccess = remember(state) { phoneControl.isNotificationAccessEnabled() }

    var phoneNumber by remember { mutableStateOf("") }
    var smsBody by remember { mutableStateOf("") }
    var alarmHour by remember { mutableIntStateOf(7) }
    var alarmMinute by remember { mutableIntStateOf(30) }
    var alarmLabel by remember { mutableStateOf("Wake up with Meena") }
    var appSearch by remember { mutableStateOf("") }

    var confirmCallNumber by remember { mutableStateOf<String?>(null) }
    var confirmSmsPair by remember { mutableStateOf<Pair<String, String>?>(null) }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        confirmCallNumber?.let { num ->
            phoneControl.placeCall(num).onSuccess(onFeedback).onFailure { onFeedback(it.message ?: "Call failed") }
            confirmCallNumber = null
        }
    }

    val filteredApps = remember(state.installedApps, appSearch) {
        val q = appSearch.trim()
        if (q.isEmpty()) state.installedApps.take(12)
        else state.installedApps.filter { it.label.contains(q, ignoreCase = true) }.take(20)
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Column {
                Text("Phone Control Center", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Text("Official Android APIs with explicit confirmation", style = MaterialTheme.typography.bodySmall, color = glow.secondaryGlow)
            }
        }

        // Device Controls: Flashlight, Volume & Quick System Settings
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Device Controls", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.FlashlightOn, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Flashlight / Torch", style = MaterialTheme.typography.bodyLarge, color = TextPrimary, modifier = Modifier.weight(1f))
                    Switch(checked = state.isFlashlightOn, onCheckedChange = { onToggleFlashlight() })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.VolumeUp, contentDescription = null, tint = glow.secondaryGlow)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Media Volume", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Slider(
                        value = state.mediaVolume,
                        onValueChange = onVolumeChange,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { onOpenSetting(Settings.ACTION_WIFI_SETTINGS) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Wi-Fi")
                    }
                    OutlinedButton(onClick = { onOpenSetting(Settings.ACTION_BLUETOOTH_SETTINGS) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bluetooth")
                    }
                    OutlinedButton(onClick = { onOpenSetting(Settings.ACTION_DISPLAY_SETTINGS) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.SettingsBrightness, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Display")
                    }
                }
            }
        }

        // Calling & SMS with Confirmation
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Calls & Messaging", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = smsBody,
                    onValueChange = { smsBody = it },
                    label = { Text("SMS Message (optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            if (phoneNumber.isNotBlank()) confirmCallNumber = phoneNumber.trim()
                            else onFeedback("Enter a phone number first")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = glow.primaryGlow),
                        modifier = Modifier.weight(1f).testTag("phone_call_button")
                    ) {
                        Icon(Icons.Outlined.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Call")
                    }
                    OutlinedButton(
                        onClick = {
                            if (phoneNumber.isNotBlank()) confirmSmsPair = phoneNumber.trim() to smsBody.trim()
                            else onFeedback("Enter a phone number first")
                        },
                        modifier = Modifier.weight(1f).testTag("phone_sms_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Message, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send SMS")
                    }
                }
            }
        }

        // Set Alarm & Reminders
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Alarms & Reminders", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = alarmHour.toString(),
                        onValueChange = { alarmHour = it.toIntOrNull()?.coerceIn(0, 23) ?: alarmHour },
                        label = { Text("Hour (0-23)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = alarmMinute.toString(),
                        onValueChange = { alarmMinute = it.toIntOrNull()?.coerceIn(0, 59) ?: alarmMinute },
                        label = { Text("Min (0-59)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = alarmLabel,
                    onValueChange = { alarmLabel = it },
                    label = { Text("Alarm / Reminder Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        phoneControl.setAlarm(alarmHour, alarmMinute, alarmLabel)
                            .onSuccess(onFeedback)
                            .onFailure { onFeedback(it.message ?: "Unable to set alarm") }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = glow.primaryGlow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Alarm, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Set Alarm (%02d:%02d)".format(alarmHour, alarmMinute))
                }
            }
        }

        // Installed Applications Launcher
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Apps, contentDescription = null, tint = glow.secondaryGlow)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Installed Apps (${state.installedApps.size})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                    TextButton(onClick = onRefreshApps) { Text("Refresh") }
                }
                OutlinedTextField(
                    value = appSearch,
                    onValueChange = { appSearch = it },
                    placeholder = { Text("Search installed apps…", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (filteredApps.isEmpty()) {
                    Text("No matching apps found on device.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                } else {
                    filteredApps.forEach { app ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    phoneControl.launchApp(app.packageName)
                                        .onFailure { onFeedback(it.message ?: "Launch failed") }
                                }
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(app.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = TextPrimary)
                                Text(app.packageName, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                            Text("Open", style = MaterialTheme.typography.labelMedium, color = glow.secondaryGlow)
                        }
                    }
                }
            }
        }

        // Notification Reader (Requires explicit Android Notification Access)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = if (hasNotifAccess) SuccessGreen else glow.secondaryGlow)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Notification Reader", style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                    Text(
                        text = if (hasNotifAccess) "Granted" else "Permission Needed",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hasNotifAccess) SuccessGreen else TextMuted
                    )
                }
                OutlinedButton(
                    onClick = { phoneControl.openNotificationListenerSettings() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (hasNotifAccess) "Manage Notification Access" else "Grant Notification Access")
                }
                if (notifications.isEmpty()) {
                    Text(
                        text = if (hasNotifAccess) "No active notifications right now." else "Enable Notification Access in Android Settings so Meena can summarize your alerts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                } else {
                    notifications.take(5).forEach { n ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text("${n.appName}: ${n.title}", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                            Text(n.text, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 2)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(104.dp))
    }

    // Call Confirmation Dialog
    confirmCallNumber?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmCallNumber = null },
            containerColor = MidnightVoid,
            title = { Text("Confirm Phone Call", color = TextPrimary) },
            text = { Text("Allow MEENA AI PRO to call $target?", color = TextSecondary) },
            confirmButton = {
                Button(onClick = {
                    if (phoneControl.hasCallPermission()) {
                        phoneControl.placeCall(target).onSuccess(onFeedback).onFailure { onFeedback(it.message ?: "Call failed") }
                        confirmCallNumber = null
                    } else {
                        callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                    }
                }) { Text("Call Now") }
            },
            dismissButton = {
                TextButton(onClick = { confirmCallNumber = null }) { Text("Cancel") }
            }
        )
    }

    // SMS Confirmation Dialog
    confirmSmsPair?.let { (num, msg) ->
        AlertDialog(
            onDismissRequest = { confirmSmsPair = null },
            containerColor = MidnightVoid,
            title = { Text("Confirm SMS Message", color = TextPrimary) },
            text = { Text("Open your messaging app to send \"$msg\" to $num?", color = TextSecondary) },
            confirmButton = {
                Button(onClick = {
                    phoneControl.sendSms(num, msg).onSuccess(onFeedback).onFailure { onFeedback(it.message ?: "SMS failed") }
                    confirmSmsPair = null
                }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { confirmSmsPair = null }) { Text("Cancel") }
            }
        )
    }
}
