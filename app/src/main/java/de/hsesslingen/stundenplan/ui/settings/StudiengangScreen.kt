package de.hsesslingen.stundenplan.ui.settings

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.hsesslingen.stundenplan.data.Studiengang
import de.hsesslingen.stundenplan.ui.StundenplanViewModel
import de.hsesslingen.stundenplan.ui.theme.PillShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudiengangScreen(viewModel: StundenplanViewModel, onBack: () -> Unit) {
    val planState by viewModel.planState.collectAsState()
    val pickerState by viewModel.pickerState.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val favoriteIds = favorites.map { it.id }.toSet()
    var selectedGroup by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { viewModel.loadStudiengangList() }

    // QIS doesn't expose a real Fachbereich/department field, only the course code — so "group"
    // here is derived from it (e.g. "WKB1" -> "WKB") rather than a proper department name. Still
    // useful as a quick filter once the full list gets long, without claiming to be more than it is.
    val groups = remember(pickerState.all) {
        pickerState.all.map { studiengangGroup(it.code) }.distinct().sorted()
    }
    val groupFiltered = if (selectedGroup != null) {
        pickerState.filtered.filter { studiengangGroup(it.code) == selectedGroup }
    } else {
        pickerState.filtered
    }

    SettingsPageScaffold(title = "Studiengänge", onBack = onBack) {
        Text(
            "Wähle deinen aktuellen Studiengang und markiere weitere als Favoriten.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        )

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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        )

        if (groups.size > 1) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                item {
                    GroupChip(label = "Alle", selected = selectedGroup == null, onClick = { selectedGroup = null })
                }
                items(groups) { group ->
                    GroupChip(label = group, selected = selectedGroup == group, onClick = { selectedGroup = group })
                }
            }
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
                items(groupFiltered, key = { it.id }) { studiengang ->
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

@Composable
private fun GroupChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(PillShape)
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

/** QIS only exposes a Studiengang's short code (e.g. "WKB1"), never a real department/Fachbereich
 *  name — this strips the trailing semester digits to get a rough grouping key ("WKB1" -> "WKB")
 *  good enough to cluster same-program semesters together in the picker. */
private fun studiengangGroup(code: String): String = code.trimEnd { it.isDigit() }.ifBlank { code }
