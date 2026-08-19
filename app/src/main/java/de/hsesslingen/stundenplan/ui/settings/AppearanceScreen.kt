package de.hsesslingen.stundenplan.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.hsesslingen.stundenplan.data.AccentPreset
import de.hsesslingen.stundenplan.data.ThemeMode
import de.hsesslingen.stundenplan.ui.StundenplanViewModel
import de.hsesslingen.stundenplan.ui.theme.PillShape

@Composable
fun AppearanceScreen(viewModel: StundenplanViewModel, onBack: () -> Unit) {
    val themeMode by viewModel.themeMode.collectAsState()
    val accentPreset by viewModel.accentPreset.collectAsState()
    val customAccentColor by viewModel.customAccentColor.collectAsState()
    val customBackgroundColor by viewModel.customBackgroundColor.collectAsState()
    val hiddenGroupKeys by viewModel.hiddenGroupKeys.collectAsState()
    val defaultViewIsDay by viewModel.defaultViewIsDay.collectAsState()
    var showAccentPicker by remember { mutableStateOf(false) }
    var showBackgroundPicker by remember { mutableStateOf(false) }

    SettingsPageScaffold(title = "Darstellung", onBack = onBack) {
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SettingsToggleRow(
                title = "Beim Start Tagesansicht",
                subtitle = "Öffnet direkt die Tag- statt der Wochenansicht",
                checked = defaultViewIsDay,
                onCheckedChange = { viewModel.setDefaultViewIsDay(it) },
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            SectionLabel("Design")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeModeChip("System", ThemeMode.SYSTEM, themeMode) { viewModel.setThemeMode(it) }
                ThemeModeChip("Hell", ThemeMode.LIGHT, themeMode) { viewModel.setThemeMode(it) }
                ThemeModeChip("Dunkel", ThemeMode.DARK, themeMode) { viewModel.setThemeMode(it) }
            }

            Column {
                SectionLabel("Akzentfarbe")
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(AccentPreset.entries.filter { it != AccentPreset.CUSTOM }) { preset ->
                        ColorSwatch(
                            color = preset.color,
                            selected = accentPreset == preset,
                            onClick = { viewModel.setAccentPreset(preset) },
                        )
                    }
                    item {
                        CustomColorSwatch(
                            color = customAccentColor,
                            selected = accentPreset == AccentPreset.CUSTOM,
                            onClick = { showAccentPicker = true },
                        )
                    }
                }
            }

            Column {
                SectionLabel("Hintergrundfarbe")
                Spacer(Modifier.height(8.dp))
                CustomColorSwatch(
                    color = customBackgroundColor,
                    selected = customBackgroundColor != null,
                    onClick = { showBackgroundPicker = true },
                )
            }

            TextButton(onClick = { viewModel.resetAppearance() }) {
                Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Auf Standard zurücksetzen")
            }

            if (hiddenGroupKeys.isNotEmpty()) {
                HiddenGroupsSection(
                    hiddenGroupKeys = hiddenGroupKeys,
                    onUnhide = { key -> viewModel.setGroupHidden(key, false) },
                )
            }
        }
    }

    if (showAccentPicker) {
        ColorPickerDialog(
            title = "Akzentfarbe",
            initialColor = customAccentColor ?: MaterialTheme.colorScheme.primary,
            onConfirm = {
                viewModel.setCustomAccentColor(it)
                showAccentPicker = false
            },
            onDismiss = { showAccentPicker = false },
        )
    }
    if (showBackgroundPicker) {
        ColorPickerDialog(
            title = "Hintergrundfarbe",
            initialColor = customBackgroundColor ?: MaterialTheme.colorScheme.background,
            onConfirm = {
                viewModel.setCustomBackgroundColor(it)
                showBackgroundPicker = false
            },
            onDismiss = { showBackgroundPicker = false },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ThemeModeChip(label: String, mode: ThemeMode, selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val isSelected = mode == selected
    Row(
        Modifier
            .clip(PillShape)
            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable { onSelect(mode) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            label,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun ColorSwatch(color: Color?, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color ?: MaterialTheme.colorScheme.primary)
            .border(2.dp, if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun CustomColorSwatch(color: Color?, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color ?: MaterialTheme.colorScheme.surfaceContainerHighest)
            .border(2.dp, if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Edit, contentDescription = "Eigene Farbe wählen", tint = if (color != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

/** Lets the user bring back recurring event groups they previously hid from the plan (e.g. a
 *  parallel Tutorium section they're not in) — see TimetableEvent.groupKey. */
@Composable
private fun HiddenGroupsSection(hiddenGroupKeys: Set<String>, onUnhide: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        SectionLabel("Ausgeblendete Veranstaltungen")
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
