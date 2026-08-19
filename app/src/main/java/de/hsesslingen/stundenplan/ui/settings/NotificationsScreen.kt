package de.hsesslingen.stundenplan.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import de.hsesslingen.stundenplan.ui.StundenplanViewModel
import de.hsesslingen.stundenplan.ui.theme.OneUiAlertDialog
import de.hsesslingen.stundenplan.ui.theme.OneUiSwitch

private val LEAD_MINUTES_PRESETS = listOf(5, 10, 15, 20, 30, 45, 60)

@Composable
fun NotificationsScreen(viewModel: StundenplanViewModel, onBack: () -> Unit) {
    val enabled by viewModel.remindersEnabled.collectAsState()
    val leadMinutesSet by viewModel.reminderLeadMinutes.collectAsState()
    val context = LocalContext.current
    var showCustomDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        // Persist "on" either way: if denied, NotificationHelper just silently no-ops every
        // individual notification later, and the user can still grant it via system settings.
        viewModel.setRemindersEnabled(true)
    }

    fun onToggle(wantEnabled: Boolean) {
        val needsPermission = wantEnabled &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.setRemindersEnabled(wantEnabled)
        }
    }

    fun toggleLead(minutes: Int) {
        val updated = if (minutes in leadMinutesSet) leadMinutesSet - minutes else leadMinutesSet + minutes
        viewModel.setReminderLeadMinutes(updated.ifEmpty { setOf(15) })
    }

    fun removeCustomLead(minutes: Int) {
        viewModel.setReminderLeadMinutes((leadMinutesSet - minutes).ifEmpty { setOf(15) })
    }

    // Custom lead times the user typed in that aren't one of the presets get their own section.
    val customMinutes = leadMinutesSet.filterNot { it in LEAD_MINUTES_PRESETS }.sorted()

    SettingsPageScaffold(title = "Benachrichtigungen", onBack = onBack) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingsToggleRow(
                title = "Vorlesungserinnerungen",
                subtitle = "Benachrichtigung vor Beginn einer Veranstaltung",
                checked = enabled,
                onCheckedChange = ::onToggle,
            )
            if (enabled) {
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Erinnerungen (mehrere möglich)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        IconButton(onClick = { showCustomDialog = true }) {
                            Icon(Icons.Filled.Add, contentDescription = "Eigene Erinnerungszeit hinzufügen")
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LEAD_MINUTES_PRESETS.forEach { minutes ->
                            LeadMinutesRow(
                                minutes = minutes,
                                selected = minutes in leadMinutesSet,
                                onClick = { toggleLead(minutes) },
                            )
                        }
                    }
                    if (customMinutes.isNotEmpty()) {
                        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        Text(
                            "Eigene Zeiten",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            customMinutes.forEach { minutes ->
                                LeadMinutesRow(
                                    minutes = minutes,
                                    selected = true,
                                    onClick = { toggleLead(minutes) },
                                    onRemove = { removeCustomLead(minutes) },
                                )
                            }
                        }
                    }

                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    TextButton(onClick = { viewModel.sendTestNotification() }) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Test-Benachrichtigung senden")
                    }
                }
            }
        }
    }

    if (showCustomDialog) {
        CustomLeadMinutesDialog(
            onConfirm = { minutes ->
                viewModel.setReminderLeadMinutes(leadMinutesSet + minutes)
                showCustomDialog = false
            },
            onDismiss = { showCustomDialog = false },
        )
    }
}

@Composable
private fun LeadMinutesRow(minutes: Int, selected: Boolean, onClick: () -> Unit, onRemove: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(28.dp).clip(CircleShape).background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
            }
            Text(
                "$minutes Min. vorher",
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onRemove != null) {
            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Entfernen", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CustomLeadMinutesDialog(onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val minutes = text.toIntOrNull()
    OneUiAlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { minutes?.let(onConfirm) }, enabled = minutes != null && minutes > 0) {
                Text("Hinzufügen", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
        title = { Text("Eigene Erinnerungszeit", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter(Char::isDigit) },
                label = { Text("Minuten vor Beginn") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
        },
    )
}

/** Shared toggle row used across settings sub-pages (title + subtitle + Switch). */
@Composable
fun SettingsToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OneUiSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
