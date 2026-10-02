package de.hsesslingen.stundenplan.ui

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import de.hsesslingen.stundenplan.data.Departure
import de.hsesslingen.stundenplan.data.TransitNotice
import de.hsesslingen.stundenplan.data.TransitStop
import de.hsesslingen.stundenplan.data.TransitStops
import de.hsesslingen.stundenplan.ui.theme.LocalIsDarkTheme
import de.hsesslingen.stundenplan.ui.theme.OneUiAlertDialog
import de.hsesslingen.stundenplan.ui.theme.PillShape
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val CLOCK = DateTimeFormatter.ofPattern("HH:mm")
private const val AUTO_REFRESH_MS = 60_000L

/**
 * The Abfahrten tab: live VVS departures from one stop (by default the one at the user's
 * campus) — from now, or from when today's last lecture ends.
 */
@Composable
internal fun TransitView(
    viewModel: StundenplanViewModel,
    stop: TransitStop?,
    lastLectureEnd: LocalTime?,
    onSearchStop: () -> Unit,
) {
    when {
        stop == TRANSIT_STOP_LOADING -> Box(Modifier.fillMaxSize())
        stop == null -> TransitStopPicker(onSelect = viewModel::setTransitStop, onSearch = onSearchStop)
        else -> DepartureBoard(viewModel, stop, lastLectureEnd)
    }
}

@Composable
private fun DepartureBoard(viewModel: StundenplanViewModel, stop: TransitStop, lastLectureEnd: LocalTime?) {
    val state by viewModel.transitState.collectAsState()
    var afterLecture by rememberSaveable { mutableStateOf(false) }
    var openNotices by remember { mutableStateOf<List<TransitNotice>?>(null) }
    // Only worth offering while the last lecture is still ahead today.
    val lectureEndOffered = lastLectureEnd != null && lastLectureEnd.isAfter(LocalTime.now())
    val at = if (afterLecture && lectureEndOffered) LocalDate.now().atTime(lastLectureEnd) else null

    // Refreshes every minute — but only while the app is actually on screen, not from the
    // background after the user switched away with this tab open.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(stop.id, at, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                viewModel.loadDepartures(stop, at)
                delay(AUTO_REFRESH_MS)
            }
        }
    }
    // Keeps "in 4 Min." counting down between the minute-by-minute refreshes.
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            now = Instant.now()
        }
    }

    val board = state.takeIf { it.stop?.id == stop.id && it.at == at }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChoicePill("Jetzt", selected = at == null) { afterLecture = false }
            if (lectureEndOffered) {
                ChoicePill("Nach der Vorlesung · ${lastLectureEnd!!.format(CLOCK)}", selected = at != null) { afterLecture = true }
            }
            Spacer(Modifier.weight(1f))
            if (board?.isLoading == true) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            }
        }
        Text(
            buildString {
                append(stop.name)
                board?.updatedAt?.let {
                    append(" · Stand ")
                    append(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(CLOCK))
                }
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(8.dp))
        when {
            board == null || (board.isLoading && board.updatedAt == null && board.error == null) ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingGlyph("Lade Abfahrten …") }
            board.departures.isEmpty() && board.error != null ->
                ErrorState(board.error, onRetry = { viewModel.loadDepartures(stop, at) })
            board.departures.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                EmptyScheduleBanner(
                    text = "Keine Abfahrten gefunden",
                    icon = Icons.Outlined.DirectionsBus,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (board.error != null) {
                    item {
                        Text(
                            "Aktualisieren fehlgeschlagen: ${board.error}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }
                items(board.departures) { departure ->
                    DepartureRow(departure, now, onShowNotices = { openNotices = departure.notices })
                }
                item {
                    Text(
                        "Echtzeitdaten: VVS. Angaben ohne Gewähr.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                    )
                }
            }
        }
    }

    openNotices?.let { notices ->
        OneUiAlertDialog(
            onDismissRequest = { openNotices = null },
            confirmButton = { TextButton(onClick = { openNotices = null }) { Text("Schließen", fontWeight = FontWeight.Bold) } },
            title = { Text("Hinweise", fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    notices.forEach { notice ->
                        Column {
                            Text(notice.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            if (notice.text.isNotEmpty() && notice.text != notice.title) {
                                Text(notice.text, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun ChoicePill(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        modifier = Modifier
            .clip(PillShape)
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun DepartureRow(departure: Departure, now: Instant, onShowNotices: () -> Unit) {
    val hasNotices = departure.notices.isNotEmpty()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(enabled = hasNotices, onClick = onShowNotices)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LineBadge(departure)
        Column(Modifier.weight(1f)) {
            Text(
                departure.destination,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (departure.isCancelled) TextDecoration.LineThrough else null,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                departure.platform?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (hasNotices) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = "Hinweise",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            val minutes = Duration.between(now, departure.expected).toMinutes()
            Text(
                when {
                    departure.isCancelled -> "fällt aus"
                    minutes <= 0 -> "jetzt"
                    minutes < 60 -> "$minutes Min."
                    else -> departure.expected.atZone(ZoneId.systemDefault()).format(CLOCK)
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (departure.isCancelled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            DepartureTimeLine(departure)
        }
    }
}

/** "14:58" plus the delay ("+3", red) or, when real-time data says it's on time, the time in green. */
@Composable
private fun DepartureTimeLine(departure: Departure) {
    val planned = departure.planned.atZone(ZoneId.systemDefault()).format(CLOCK)
    val delay = departure.delayMinutes
    val onTimeColor = if (LocalIsDarkTheme.current) Color(0xFF78E9AB) else Color(0xFF2E9E5B)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            planned,
            style = MaterialTheme.typography.bodySmall,
            color = if (delay == 0L && !departure.isCancelled) onTimeColor else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (delay != null && delay > 0 && !departure.isCancelled) {
            Text("+$delay", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun LineBadge(departure: Departure) {
    val color = when (departure.productClass) {
        0 -> Color(0xFF6E6E73) // Regional-/Fernzug
        1 -> Color(0xFF2E9E5B) // S-Bahn
        2, 3, 4 -> Color(0xFF0A6BB0) // U-/Stadtbahn, Tram
        5, 6, 7, 10 -> Color(0xFFC0392B) // Bus
        else -> Color(0xFF6E6E73)
    }
    Text(
        departure.line,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.ExtraBold,
        color = Color.White,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = Modifier
            .widthIn(min = 48.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(color)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}

/** First visit to the Abfahrten tab without a Mensa picked to guess from: which stop? */
@Composable
private fun TransitStopPicker(onSelect: (TransitStop) -> Unit, onSearch: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            IconBadge(Icons.Filled.DirectionsBus)
        }
        Text(
            "Welche Haltestelle?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Text(
            "Abfahrten in Echtzeit vom VVS. Wechseln kannst du jederzeit oben über den Namen der Haltestelle.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        TransitStops.presets.forEach { preset ->
            PickerRow(title = preset.stop.name, subtitle = preset.campus, onClick = { onSelect(preset.stop) })
        }
        PickerRow(title = "Andere Haltestelle suchen", subtitle = null, icon = Icons.Outlined.Search, onClick = onSearch)
        Spacer(Modifier.height(120.dp))
    }
}

@Composable
private fun PickerRow(
    title: String,
    subtitle: String?,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Outlined.DirectionsBus,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** "Andere Haltestelle suchen": any stop in the VVS network, searched as you type. */
@Composable
internal fun TransitStopSearchDialog(
    viewModel: StundenplanViewModel,
    onSelect: (TransitStop) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<TransitStop>>(emptyList()) }
    var failed by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    LaunchedEffect(query) {
        if (query.trim().length < 2) {
            results = emptyList()
            failed = false
            return@LaunchedEffect
        }
        delay(300) // wait for typing to pause instead of searching every keystroke
        searching = true
        val result = viewModel.searchTransitStops(query.trim())
        results = result.getOrDefault(emptyList())
        failed = result.isFailure
        searching = false
    }

    OneUiAlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
        title = { Text("Haltestelle suchen", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("z. B. Esslingen Bahnhof") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    when {
                        searching && results.isEmpty() -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        failed -> Text(
                            "Suche fehlgeschlagen — keine Verbindung zur VVS-Auskunft?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        query.trim().length >= 2 && !searching && results.isEmpty() -> Text(
                            "Keine Haltestelle gefunden",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    results.forEach { stop ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable { onSelect(stop) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            Text(stop.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        },
    )
}
