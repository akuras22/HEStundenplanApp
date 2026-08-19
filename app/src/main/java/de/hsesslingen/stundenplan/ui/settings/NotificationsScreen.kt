package de.hsesslingen.stundenplan.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import de.hsesslingen.stundenplan.ui.theme.PillShape

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

    // Custom lead times the user typed in show up alongside the presets, ordered together.
    val allChipMinutes = (LEAD_MINUTES_PRESETS + leadMinutesSet).distinct().sorted()

    SettingsPageScaffold(title = "Benachrichtigungen", onBack = onBack) {
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SettingsToggleRow(
                title = "Vorlesungserinnerungen",
                subtitle = "Benachrichtigung vor Beginn einer Veranstaltung",
                checked = enabled,
                onCheckedChange = ::onToggle,
            )
            if (enabled) {
                Column {
                    Text(
                        "Erinnerungen (mehrere möglich)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allChipMinutes) { minutes ->
                            LeadMinutesChip(
                                minutes = minutes,
                                selected = minutes in leadMinutesSet,
                                onClick = { toggleLead(minutes) },
                            )
                        }
                        item {
                            AddCustomChip(onClick = { showCustomDialog = true })
                        }
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
private fun LeadMinutesChip(minutes: Int, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(PillShape)
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "$minutes Min.",
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun AddCustomChip(onClick: () -> Unit) {
    Row(
        Modifier
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Add, contentDescription = "Eigene Erinnerungszeit", modifier = Modifier.height(18.dp))
    }
}

@Composable
private fun CustomLeadMinutesDialog(onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val minutes = text.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { minutes?.let(onConfirm) }, enabled = minutes != null && minutes > 0) {
                Text("Hinzufügen", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
        shape = MaterialTheme.shapes.extraLarge,
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
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
        )
    }
}
