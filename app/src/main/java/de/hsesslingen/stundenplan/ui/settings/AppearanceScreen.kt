package de.hsesslingen.stundenplan.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.hsesslingen.stundenplan.data.ThemeMode
import de.hsesslingen.stundenplan.ui.StundenplanViewModel
import de.hsesslingen.stundenplan.ui.theme.PillShape

@Composable
fun AppearanceScreen(viewModel: StundenplanViewModel, onBack: () -> Unit) {
    val dynamicColor by viewModel.dynamicColorEnabled.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val hiddenGroupKeys by viewModel.hiddenGroupKeys.collectAsState()

    SettingsPageScaffold(title = "Darstellung", onBack = onBack) {
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column {
                Text(
                    "Design",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeModeChip("System", ThemeMode.SYSTEM, themeMode) { viewModel.setThemeMode(it) }
                    ThemeModeChip("Hell", ThemeMode.LIGHT, themeMode) { viewModel.setThemeMode(it) }
                    ThemeModeChip("Dunkel", ThemeMode.DARK, themeMode) { viewModel.setThemeMode(it) }
                }
            }

            SettingsToggleRow(
                title = "Dynamische Farben",
                subtitle = "Farben an dein Hintergrundbild anpassen, statt der festen App-Farben",
                checked = dynamicColor,
                onCheckedChange = { viewModel.setDynamicColorEnabled(it) },
            )

            if (hiddenGroupKeys.isNotEmpty()) {
                HiddenGroupsSection(
                    hiddenGroupKeys = hiddenGroupKeys,
                    onUnhide = { key -> viewModel.setGroupHidden(key, false) },
                )
            }
        }
    }
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

/** Lets the user bring back recurring event groups they previously hid from the plan (e.g. a
 *  parallel Tutorium section they're not in) — see TimetableEvent.groupKey. */
@Composable
private fun HiddenGroupsSection(hiddenGroupKeys: Set<String>, onUnhide: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
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
