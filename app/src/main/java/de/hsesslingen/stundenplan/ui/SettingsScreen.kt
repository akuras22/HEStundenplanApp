package de.hsesslingen.stundenplan.ui

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import de.hsesslingen.stundenplan.data.Studiengang
import de.hsesslingen.stundenplan.ui.theme.PillShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: StundenplanViewModel, onBack: () -> Unit) {
    val planState by viewModel.planState.collectAsState()
    val pickerState by viewModel.pickerState.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val hiddenGroupKeys by viewModel.hiddenGroupKeys.collectAsState()
    val remindersEnabled by viewModel.remindersEnabled.collectAsState()
    val favoriteIds = favorites.map { it.id }.toSet()
    val context = LocalContext.current

    // Android 13+ requires the POST_NOTIFICATIONS runtime permission before any notification can
    // show — requested only when the user actually turns reminders on, not proactively at launch.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        // Persist "on" either way: if denied, NotificationHelper just silently no-ops every
        // individual notification later, and the user can still grant it via system settings.
        viewModel.setRemindersEnabled(true)
    }

    LaunchedEffect(Unit) { viewModel.loadStudiengangList() }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "Studiengang wählen",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f),
                )
                // Exports the currently loaded week's events as .ics — covers the whole semester
                // in one go since QIS reports each event's full recurrence on every week's page,
                // not just the visited one (see IcsExporter). Only meaningful once a week has
                // actually loaded.
                if (planState.events.isNotEmpty()) {
                    IconButton(onClick = { viewModel.exportIcs() }) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = "Als Kalender exportieren")
                    }
                }
            }

            OutlinedTextField(
                value = pickerState.query,
                onValueChange = viewModel::setPickerQuery,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text("Suche, z. B. WKB1") },
                singleLine = true,
                shape = PillShape,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            )

            Spacer(Modifier.height(12.dp))

            RemindersToggleRow(
                enabled = remindersEnabled,
                onToggle = { wantEnabled ->
                    val needsPermission = wantEnabled &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    if (needsPermission) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.setRemindersEnabled(wantEnabled)
                    }
                },
            )
            Spacer(Modifier.height(4.dp))

            if (hiddenGroupKeys.isNotEmpty()) {
                HiddenGroupsSection(
                    hiddenGroupKeys = hiddenGroupKeys,
                    onUnhide = { key -> viewModel.setGroupHidden(key, false) },
                )
                Spacer(Modifier.height(4.dp))
            }

            when {
                pickerState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                pickerState.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(pickerState.error ?: "")
                        TextButton(onClick = { viewModel.loadStudiengangList(forceReload = true) }) {
                            Text("Erneut versuchen")
                        }
                    }
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(pickerState.filtered, key = { it.id }) { studiengang ->
                        StudiengangRow(
                            studiengang = studiengang,
                            selected = studiengang.id == planState.studiengang?.id,
                            isFavorite = studiengang.id in favoriteIds,
                            onToggleFavorite = { viewModel.toggleFavorite(studiengang) },
                            onClick = {
                                viewModel.selectStudiengang(studiengang)
                                onBack()
                            },
                        )
                    }
                }
            }
        }
    }
}

/** Toggles the background worker that notifies ~15 minutes before a lecture starts — see
 *  LectureReminderWorker. */
@Composable
private fun RemindersToggleRow(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable { onToggle(!enabled) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Vorlesungserinnerungen", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(
                "Benachrichtigung ca. 15 Min. vor Beginn",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
        )
    }
}

/** Lets the user bring back recurring event groups they previously hid from the plan (e.g. a
 *  parallel Tutorium section they're not in) — see TimetableEvent.groupKey. */
@Composable
private fun HiddenGroupsSection(hiddenGroupKeys: Set<String>, onUnhide: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Text(
            "Ausgeblendete Veranstaltungen",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            hiddenGroupKeys.sorted().forEach { key ->
                val parts = key.split("|", limit = 2)
                val label = if (parts.size == 2) "${parts[0]} (${parts[1]})" else key
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = { onUnhide(key) }) { Text("Einblenden") }
                }
            }
        }
    }
}

@Composable
private fun StudiengangRow(
    studiengang: Studiengang,
    selected: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceContainerHigh,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.School,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(studiengang.code, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = "Ausgewählt", tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(4.dp))
        }
        IconButton(onClick = onToggleFavorite) {
            Icon(
                if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = if (isFavorite) "Favorit entfernen" else "Als Favorit merken",
                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
