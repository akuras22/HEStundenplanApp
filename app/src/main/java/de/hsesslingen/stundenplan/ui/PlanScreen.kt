package de.hsesslingen.stundenplan.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.hsesslingen.stundenplan.data.TimetableEvent
import de.hsesslingen.stundenplan.data.UpdateInfo
import de.hsesslingen.stundenplan.data.Weekday
import de.hsesslingen.stundenplan.ui.theme.PillShape
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs

private enum class PlanViewMode { WEEK, DAY }

private val eventPalette = listOf(
    Color(0xFF4DA3FF), Color(0xFF5FD68A), Color(0xFFFF8A5B),
    Color(0xFFC792EA), Color(0xFF3CC9C0), Color(0xFFFFC15E),
    Color(0xFF8C9EFF), Color(0xFFFF6FA3),
)

private fun colorFor(title: String): Color = eventPalette[abs(title.hashCode()) % eventPalette.size]

/** Just the subject name — QIS prefixes every title with the Studiengang code (e.g. "WKB1"),
 *  which is redundant once it's already shown in the screen header. */
private fun TimetableEvent.shortTitle(courseCode: String?): String {
    val withoutCode = if (!courseCode.isNullOrBlank()) title.removePrefix(courseCode) else title
    return withoutCode.trim().ifBlank { title }
}

/** Just the room number — QIS rooms come as "Gebäude 01 - F 01.-110"; only the part after the
 *  last " - " is what you'd actually look for on a door. */
private fun TimetableEvent.shortRoom(): String? {
    val room = room?.trim()?.takeIf { it.isNotBlank() } ?: return null
    val idx = room.lastIndexOf(" - ")
    return if (idx >= 0) room.substring(idx + 3).trim() else room
}

// Week-grid columns are narrow (5 fit across a phone), so titles routinely wrap onto several
// lines. The default labelSmall line-height (meant for single lines) leaves airy gaps between
// wrapped lines that read as broken/ugly — these tighten it into a dense little block instead.
private val WeekCardTitleStyle = TextStyle(fontSize = 11.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold)
private val WeekCardRoomStyle = TextStyle(fontSize = 10.sp, lineHeight = 11.sp)

/** Largest font size (down to [minFontSize]) at which every individual word of [text] fits
 *  within [availableWidth] — so `Text` only ever wraps between words, never mid-word. */
@Composable
private fun rememberFittingFontSize(
    text: String,
    availableWidth: Dp,
    maxFontSize: TextUnit,
    minFontSize: TextUnit,
    fontWeight: FontWeight,
): TextUnit {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(text, availableWidth) {
        val words = text.split(" ").filter { it.isNotBlank() }
        if (words.isEmpty()) return@remember maxFontSize
        val availablePx = with(density) { availableWidth.toPx() }
        // One measurement pass per word at maxFontSize, then scale linearly — text width scales
        // ~linearly with font size, so this needs no shrink-and-remeasure loop. That loop used to
        // run up to ~8 full measurement passes over every word on every recomposition (every page
        // in the pager, on every swipe), which is what made the week view laggy.
        val longestWordPxAtMax = words.maxOf { word ->
            textMeasurer.measure(text = word, style = TextStyle(fontSize = maxFontSize, fontWeight = fontWeight)).size.width
        }
        if (longestWordPxAtMax <= availablePx) {
            maxFontSize
        } else {
            val scale = availablePx / longestWordPxAtMax
            (maxFontSize.value * scale).coerceIn(minFontSize.value, maxFontSize.value).sp
        }
    }
}

private fun LocalDate.weekMonday(): LocalDate = this.with(DayOfWeek.MONDAY)

/** Saturday/Sunday never appear in this app (no lectures happen on weekends), so anything that
 *  lands on one snaps forward to the following Monday instead. */
private fun LocalDate.nearestWeekday(): LocalDate = when (dayOfWeek) {
    DayOfWeek.SATURDAY -> plusDays(2)
    DayOfWeek.SUNDAY -> plusDays(1)
    else -> this
}

private val SHORT_DATE = DateTimeFormatter.ofPattern("dd.MM.")
private val FULL_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy")

// Swipe navigation is implemented as a huge pager anchored to a fixed Monday far in the past, so
// page indices map 1:1 to real dates/weeks without ever needing negative pages.
private val PAGE_EPOCH_MONDAY: LocalDate = LocalDate.of(2000, 1, 3)

// The day pager only ever indexes Monday..Friday — each week contributes exactly 5 pages, so
// swiping past Friday lands straight on the next Monday, skipping the weekend entirely.
private const val WEEKDAYS_PER_WEEK = 5
private const val DAY_PAGE_COUNT = 10_400 * WEEKDAYS_PER_WEEK // ~200 years
private const val WEEK_PAGE_COUNT = 10_400 // ~200 years

private fun dateToDayPage(date: LocalDate): Int {
    val weekIndex = ChronoUnit.WEEKS.between(PAGE_EPOCH_MONDAY, date.weekMonday()).toInt()
    val dayOffset = (date.dayOfWeek.value - 1).coerceIn(0, WEEKDAYS_PER_WEEK - 1)
    return weekIndex * WEEKDAYS_PER_WEEK + dayOffset
}
private fun dayPageToDate(page: Int): LocalDate {
    val weekIndex = Math.floorDiv(page, WEEKDAYS_PER_WEEK)
    val dayOffset = Math.floorMod(page, WEEKDAYS_PER_WEEK)
    return PAGE_EPOCH_MONDAY.plusWeeks(weekIndex.toLong()).plusDays(dayOffset.toLong())
}
private fun dateToWeekPage(date: LocalDate): Int =
    (ChronoUnit.DAYS.between(PAGE_EPOCH_MONDAY, date.weekMonday()) / 7).toInt()
private fun weekPageToMonday(page: Int): LocalDate = PAGE_EPOCH_MONDAY.plusWeeks(page.toLong())

private const val PILL_ANIM_MS = 200
private val HEADER_ZONE_HEIGHT = 92.dp

/**
 * Colorful blobs that sit behind the header — this is the actual content Haze blurs into "glass".
 * Clipped tightly to the header's own bounds so nothing bleeds into the content below it.
 */
@Composable
private fun AmbientBackdrop(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(HEADER_ZONE_HEIGHT).clipToBounds()) {
        Box(
            Modifier
                .size(220.dp)
                .offset(x = (-60).dp, y = (-90).dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape),
        )
        Box(
            Modifier
                .size(190.dp)
                .align(Alignment.TopEnd)
                .offset(x = 50.dp, y = (-60).dp)
                .background(colorFor("accent2").copy(alpha = 0.5f), CircleShape),
        )
        Box(
            Modifier
                .size(160.dp)
                .align(Alignment.BottomStart)
                .offset(x = 40.dp, y = 60.dp)
                .background(colorFor("accent3").copy(alpha = 0.35f), CircleShape),
        )
    }
}

@Composable
private fun GlassIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = MaterialTheme.colorScheme.onSurface)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(viewModel: StundenplanViewModel, onOpenSettings: () -> Unit) {
    val state by viewModel.planState.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val context = LocalContext.current
    var viewMode by remember { mutableStateOf(PlanViewMode.WEEK) }
    var selectedDate by remember { mutableStateOf(LocalDate.now().nearestWeekday()) }
    var selectedEvent by remember { mutableStateOf<TimetableEvent?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    val headerHaze = rememberHazeState()
    val navHaze = rememberHazeState()

    // The site serves real per-week data (empty outside term dates, room changes, cancellations),
    // not a recurring template, so every week the user swipes to needs its own live fetch.
    LaunchedEffect(state.studiengang, selectedDate.weekMonday()) {
        if (state.studiengang != null) viewModel.loadWeek(selectedDate.weekMonday())
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            AmbientBackdrop(Modifier.hazeSource(state = headerHaze))
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(HEADER_ZONE_HEIGHT)
                        .hazeEffect(state = headerHaze, style = HazeMaterials.thin())
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        state.studiengang?.code ?: "Stundenplan",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassIconButton(Icons.Filled.Today, "Heute") { selectedDate = LocalDate.now().nearestWeekday() }
                        GlassIconButton(Icons.Filled.EditCalendar, "Datum wählen") { showDatePicker = true }
                        GlassIconButton(Icons.Filled.Refresh, "Aktualisieren") { viewModel.refresh() }
                        GlassIconButton(Icons.Filled.Settings, "Einstellungen") { onOpenSettings() }
                    }
                }

                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .hazeSource(state = navHaze),
                ) {
                    when {
                        state.studiengang == null && !state.isLoading -> EmptyState(onOpenSettings)
                        // Only block the whole screen on the very first load. Once a week has ever
                        // loaded, swiping to a new week fetches quietly in the background so the
                        // pager keeps swiping smoothly instead of flashing a spinner every time.
                        state.isLoading && state.weekMonday == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            LoadingGlyph()
                        }
                        state.error != null -> ErrorState(state.error!!, onRetry = { viewModel.refresh() })
                        else -> AnimatedContent(
                            targetState = viewMode,
                            transitionSpec = {
                                // "Tag" reads as a zoom into one day of the grid, "Woche" as
                                // zooming back out — scale/slide direction flips with the target so
                                // the switch feels like a fluid morph rather than a flat cross-fade.
                                // Springs (not tweens) drive scale/slide/size so the motion settles
                                // naturally instead of stopping abruptly.
                                val enteringDay = targetState == PlanViewMode.DAY
                                val direction = if (enteringDay) 1 else -1
                                val motionSpring = spring<Float>(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow,
                                )
                                val offsetSpring = spring<IntOffset>(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow,
                                )
                                val enter = fadeIn(animationSpec = tween(320, easing = FastOutSlowInEasing)) +
                                    scaleIn(initialScale = if (enteringDay) 0.90f else 1.08f, animationSpec = motionSpring) +
                                    slideInVertically(animationSpec = offsetSpring) { h -> direction * h / 12 }
                                val exit = fadeOut(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                                    scaleOut(targetScale = if (enteringDay) 1.06f else 0.92f, animationSpec = motionSpring) +
                                    slideOutVertically(animationSpec = offsetSpring) { h -> -direction * h / 20 }
                                enter.togetherWith(exit).using(
                                    SizeTransform(clip = false) { _, _ ->
                                        spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
                                    },
                                )
                            },
                            label = "viewMode",
                        ) { mode ->
                            if (mode == PlanViewMode.WEEK) {
                                WeekView(
                                    events = state.events,
                                    selectedDate = selectedDate,
                                    courseCode = state.studiengang?.code,
                                    onEventClick = { selectedEvent = it },
                                    onWeekChanged = { selectedDate = it },
                                    onDayClick = { date ->
                                        selectedDate = date
                                        viewMode = PlanViewMode.DAY
                                    },
                                )
                            } else {
                                DayView(
                                    events = state.events,
                                    selectedDate = selectedDate,
                                    courseCode = state.studiengang?.code,
                                    onDateSelected = { selectedDate = it },
                                    onEventClick = { selectedEvent = it },
                                )
                            }
                        }
                    }
                }
            }

            // Full-width soft shadow rising from the bottom edge, like Samsung's own nav bars —
            // separate from the pill itself, which floats on top of it.
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = 0.9f)),
                        ),
                    ),
            )

            BottomNavPill(
                selected = viewMode,
                onSelect = { viewMode = it },
                hazeState = navHaze,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 20.dp),
            )
        }
    }

    selectedEvent?.let { event ->
        EventDetailDialog(event = event, onDismiss = { selectedEvent = null })
    }

    updateState.available?.let { info ->
        UpdateDialog(
            info = info,
            downloadProgress = updateState.downloadProgress,
            error = updateState.error,
            canInstall = viewModel.canRequestInstall(),
            onInstall = { viewModel.downloadAndInstallUpdate() },
            onOpenInstallSettings = { context.startActivity(viewModel.requestInstallPermissionIntent()) },
            onDismiss = { viewModel.dismissUpdate() },
        )
    }

    if (showDatePicker) {
        // No lectures happen on weekends, so Saturday/Sunday aren't pickable at all.
        val weekdaysOnly = remember {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val day = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().dayOfWeek
                    return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY
                }
            }
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = weekdaysOnly,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Abbrechen") }
            },
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 0.dp,
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                    todayDateBorderColor = MaterialTheme.colorScheme.primary,
                    todayContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}

/** Floating pill bottom-nav bar, Samsung One UI style (à la Galaxy Store / Gallery / Contacts). */
@Composable
private fun BottomNavPill(
    selected: PlanViewMode,
    onSelect: (PlanViewMode) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    // Wrap-content, not full-width — Samsung's own bars hug their items tightly instead of
    // stretching tabs across the whole width with big gaps between them. Items are equal width
    // and overlap slightly, matching the reference screenshots.
    Row(
        modifier
            .clip(PillShape)
            .hazeEffect(state = hazeState, style = HazeMaterials.ultraThin())
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy((-10).dp),
    ) {
        BottomNavItem(Icons.Filled.ViewWeek, "Woche", selected == PlanViewMode.WEEK, Modifier.width(96.dp)) {
            onSelect(PlanViewMode.WEEK)
        }
        BottomNavItem(Icons.Filled.ViewDay, "Tag", selected == PlanViewMode.DAY, Modifier.width(96.dp)) {
            onSelect(PlanViewMode.DAY)
        }
    }
}

@Composable
private fun BottomNavItem(icon: ImageVector, label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    // Only the selected tab gets a visible pill — unselected tabs are fully transparent,
    // matching the reference screenshot exactly.
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f) else Color.Transparent,
        animationSpec = tween(PILL_ANIM_MS),
        label = "navItemBg",
    )
    val fg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(PILL_ANIM_MS),
        label = "navItemFg",
    )
    Column(
        modifier
            .clip(PillShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(22.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun LoadingGlyph() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        androidx.compose.material3.CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text("Lade Stundenplan …", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EmptyState(onOpenSettings: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconBadge(Icons.Filled.CalendarMonth)
            Spacer(Modifier.height(16.dp))
            Text("Kein Studiengang ausgewählt", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            PillButton("Studiengang wählen", onClick = onOpenSettings)
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            IconBadge(Icons.Filled.WarningAmber, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(16.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            PillButton("Erneut versuchen", onClick = onRetry)
        }
    }
}

@Composable
private fun IconBadge(icon: ImageVector, tint: Color = MaterialTheme.colorScheme.primary) {
    Box(
        Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun PillButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Text(label, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DayView(
    events: List<TimetableEvent>,
    selectedDate: LocalDate,
    courseCode: String?,
    onDateSelected: (LocalDate) -> Unit,
    onEventClick: (TimetableEvent) -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = dateToDayPage(selectedDate)) { DAY_PAGE_COUNT }

    // Swipe -> selectedDate.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            val date = dayPageToDate(page)
            if (date != selectedDate) onDateSelected(date)
        }
    }
    // External date change (date picker, tapping a day in week view, weekday pill) -> swipe there.
    LaunchedEffect(selectedDate) {
        val targetPage = dateToDayPage(selectedDate)
        if (pagerState.currentPage != targetPage) pagerState.animateScrollToPage(targetPage)
    }

    val monday = selectedDate.weekMonday()
    Column(Modifier.fillMaxSize()) {
        // Same header structure as WeekView: week-range title, then the weekday chip row.
        Text(
            "Woche vom ${monday.format(SHORT_DATE)} – ${monday.plusDays(4).format(FULL_DATE)}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Weekday.entries.forEach { day ->
                val date = monday.plusDays(day.ordinal.toLong())
                DateChip(
                    day = day,
                    date = date,
                    selected = date == selectedDate,
                    isToday = date == LocalDate.now(),
                    modifier = Modifier.weight(1f),
                    onClick = { onDateSelected(date) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        // Pre-compose the adjacent page so it's already laid out when a swipe reaches it instead
        // of building it from scratch mid-gesture, which is what reads as swipe lag/jank.
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
            DayTimeline(date = dayPageToDate(page), events = events, courseCode = courseCode, onEventClick = onEventClick)
        }
    }
}

/** Single-day version of the week grid — same hour axis/gridlines/now-line, one wide column. */
@Composable
private fun DayTimeline(date: LocalDate, events: List<TimetableEvent>, courseCode: String?, onEventClick: (TimetableEvent) -> Unit) {
    val dayEvents = events.filter { it.appliesOn(date) }.sortedBy { it.startMinutes }

    if (dayEvents.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconBadge(Icons.Filled.EventBusy, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Text("Keine Veranstaltungen an diesem Tag")
            }
        }
        return
    }

    val dayStart = DAY_START_MINUTES_DEFAULT
    val dayEnd = DAY_END_MINUTES_DEFAULT
    val totalHeight = MINUTE_HEIGHT * (dayEnd - dayStart)

    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        val columnWidth = maxWidth - TIME_AXIS_WIDTH
        val vScroll = rememberScrollState()
        Column(Modifier.fillMaxWidth().verticalScroll(vScroll)) {
        Box(Modifier.fillMaxWidth()) {
            GridLines(dayStart = dayStart, dayEnd = dayEnd, totalHeight = totalHeight, columnWidth = columnWidth, dayCount = 1)
            Row(Modifier.fillMaxWidth()) {
                TimeAxis(dayStart = dayStart, dayEnd = dayEnd, totalHeight = totalHeight)
                Box(Modifier.width(columnWidth).height(totalHeight)) {
                    if (date == LocalDate.now()) {
                        val now = LocalTime.now()
                        val nowMinutes = now.hour * 60 + now.minute
                        if (nowMinutes in dayStart..dayEnd) {
                            val y = MINUTE_HEIGHT * (nowMinutes - dayStart)
                            Box(
                                Modifier
                                    .offset(y = y)
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(MaterialTheme.colorScheme.error),
                            )
                        }
                    }
                    dayEvents.forEach { event ->
                        val top = MINUTE_HEIGHT * (event.startMinutes - dayStart)
                        val height = MINUTE_HEIGHT * (event.endMinutes - event.startMinutes).coerceAtLeast(35)
                        DayTimelineCard(
                            event = event,
                            courseCode = courseCode,
                            onClick = { onEventClick(event) },
                            modifier = Modifier
                                .offset(y = top)
                                .padding(horizontal = 4.dp)
                                .width(columnWidth - 8.dp)
                                .heightIn(min = height),
                        )
                    }
                }
            }
        }
            // Scroll headroom so the last hour can clear the floating bottom nav bar.
            Spacer(Modifier.height(120.dp))
        }
    }
}

@Composable
private fun DayTimelineCard(event: TimetableEvent, courseCode: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = colorFor(event.title)
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(accent.copy(alpha = 0.9f))
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        Text(
            "${event.startLabel} – ${event.endLabel}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Text(
            event.shortTitle(courseCode),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        event.shortRoom()?.let { room ->
            Text(
                room,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
    }
}

private const val DAY_START_MINUTES_DEFAULT = 8 * 60
private const val DAY_END_MINUTES_DEFAULT = 19 * 60
private val MINUTE_HEIGHT = 1.3.dp
private val TIME_AXIS_WIDTH = 40.dp

/** A real Untis-style grid: all five weekdays fit on screen, hour gridlines, sticky day header, now-line, swipeable. */
@Composable
private fun WeekView(
    events: List<TimetableEvent>,
    selectedDate: LocalDate,
    courseCode: String?,
    onEventClick: (TimetableEvent) -> Unit,
    onWeekChanged: (LocalDate) -> Unit,
    onDayClick: (LocalDate) -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = dateToWeekPage(selectedDate)) { WEEK_PAGE_COUNT }
    val dayOffsetInWeek = ChronoUnit.DAYS.between(selectedDate.weekMonday(), selectedDate).toInt()

    // Swipe -> selectedDate (keeping the same weekday offset within the new week).
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            val newMonday = weekPageToMonday(page)
            val newDate = newMonday.plusDays(dayOffsetInWeek.toLong())
            if (newDate != selectedDate) onWeekChanged(newDate)
        }
    }
    // External date change (date picker, coming back from day view) -> swipe there.
    LaunchedEffect(selectedDate) {
        val targetPage = dateToWeekPage(selectedDate)
        if (pagerState.currentPage != targetPage) pagerState.animateScrollToPage(targetPage)
    }

    Column(Modifier.fillMaxSize()) {
        val headerMonday = weekPageToMonday(pagerState.currentPage)
        Text(
            "Woche vom ${headerMonday.format(SHORT_DATE)} – ${headerMonday.plusDays(4).format(FULL_DATE)}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )

        // Pre-compose the adjacent week so its grid + font-fit pass are already done by the time a
        // swipe reaches it, instead of building all of that from scratch mid-gesture — that
        // first-frame cost (not the swipe/animation itself) is what read as lag. The data for that
        // neighboring week is prefetched into the ViewModel's cache already (see loadWeek), so this
        // composition doesn't trigger a network call — it just lays out data that's already there.
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
            val monday = weekPageToMonday(page)
            WeekGrid(
                monday = monday,
                events = events,
                courseCode = courseCode,
                onEventClick = onEventClick,
                onDayClick = onDayClick,
            )
        }
    }
}

@Composable
private fun WeekGrid(
    monday: LocalDate,
    events: List<TimetableEvent>,
    courseCode: String?,
    onEventClick: (TimetableEvent) -> Unit,
    onDayClick: (LocalDate) -> Unit,
) {
    val weekEvents = Weekday.entries.associateWith { day ->
        val date = monday.plusDays(day.ordinal.toLong())
        events.filter { it.day == day && it.appliesOn(date) }
    }
    val allVisible = weekEvents.values.flatten()

    if (allVisible.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconBadge(Icons.Filled.EventBusy, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Text("Keine Veranstaltungen gefunden")
            }
        }
        return
    }

    val dayStart = DAY_START_MINUTES_DEFAULT
    val dayEnd = DAY_END_MINUTES_DEFAULT
    val totalMinutes = dayEnd - dayStart
    val totalHeight = MINUTE_HEIGHT * totalMinutes

    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        val columnWidth = (maxWidth - TIME_AXIS_WIDTH) / 5

        // One shared font size for every card in this week, not a per-card best-fit — otherwise
        // "Programmieren" ends up tiny while "BWL und VWL" stays big and the grid looks
        // inconsistent. Sized so the single longest word across the whole week still fits without
        // being chopped mid-word; short weeks get a bigger, easier-to-read size for free.
        val titleFontSize = rememberFittingFontSize(
            text = allVisible.joinToString(" ") { it.shortTitle(courseCode) },
            availableWidth = columnWidth - 4.dp - 10.dp,
            maxFontSize = WeekCardTitleStyle.fontSize,
            minFontSize = 7.sp,
            fontWeight = FontWeight.Bold,
        )

        // Sticky day-of-week header row, aligned with the grid columns below. Tap a day to open it.
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(TIME_AXIS_WIDTH))
            Weekday.entries.forEach { day ->
                val date = monday.plusDays(day.ordinal.toLong())
                val isToday = date == LocalDate.now()
                DateChip(
                    day = day,
                    date = date,
                    // Only today gets highlighted here — the week view isn't "about" a chosen day,
                    // so the same weekday shouldn't stay lit up forever as you swipe between weeks.
                    selected = isToday,
                    isToday = isToday,
                    modifier = Modifier.width(columnWidth),
                    onClick = { onDayClick(date) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        val vScroll = rememberScrollState()
        Column(
            Modifier
                .padding(top = 52.dp)
                .fillMaxWidth()
                .verticalScroll(vScroll),
        ) {
        Box(Modifier.fillMaxWidth()) {
            GridLines(dayStart = dayStart, dayEnd = dayEnd, totalHeight = totalHeight, columnWidth = columnWidth)
            Row(Modifier.fillMaxWidth()) {
                TimeAxis(dayStart = dayStart, dayEnd = dayEnd, totalHeight = totalHeight)
                Weekday.entries.forEach { day ->
                    val date = monday.plusDays(day.ordinal.toLong())
                    DayColumn(
                        date = date,
                        events = weekEvents[day].orEmpty(),
                        courseCode = courseCode,
                        titleFontSize = titleFontSize,
                        dayStart = dayStart,
                        dayEnd = dayEnd,
                        totalHeight = totalHeight,
                        columnWidth = columnWidth,
                        onEventClick = onEventClick,
                    )
                }
            }
        }
            // Scroll headroom so the last hour can clear the floating bottom nav bar.
            Spacer(Modifier.height(120.dp))
        }
    }
}

/** Unified weekday+date pill used both as the week-view header and the day-view quick switcher. */
@Composable
private fun DateChip(
    day: Weekday,
    date: LocalDate,
    selected: Boolean,
    isToday: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f),
        animationSpec = tween(PILL_ANIM_MS),
        label = "dateChipBg",
    )
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val fgVariant = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier
            .padding(horizontal = 3.dp)
            .height(48.dp)
            .clip(PillShape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(day.germanLabel.take(2), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = fg)
            Text(date.format(SHORT_DATE), style = MaterialTheme.typography.labelSmall, color = fgVariant)
        }
        if (isToday && !selected) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 5.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
private fun GridLines(dayStart: Int, dayEnd: Int, totalHeight: Dp, columnWidth: Dp, dayCount: Int = 5) {
    val lineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val hourCount = (dayEnd - dayStart) / 60
    Canvas(Modifier.fillMaxWidth().height(totalHeight)) {
        val axisPx = TIME_AXIS_WIDTH.toPx()
        val colPx = columnWidth.toPx()
        val totalWidth = axisPx + colPx * dayCount
        // Horizontal hour lines.
        for (i in 0..hourCount) {
            val y = size.height * i / hourCount
            drawLine(lineColor, Offset(axisPx, y), Offset(totalWidth, y), strokeWidth = 1f)
        }
        // Vertical day separators.
        for (i in 0..dayCount) {
            val x = axisPx + colPx * i
            drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        }
    }
}

@Composable
private fun TimeAxis(dayStart: Int, dayEnd: Int, totalHeight: Dp) {
    Column(Modifier.width(TIME_AXIS_WIDTH).height(totalHeight)) {
        var hour = dayStart
        while (hour < dayEnd) {
            Box(Modifier.height(MINUTE_HEIGHT * 60), contentAlignment = Alignment.TopCenter) {
                Text(
                    "%02d:00".format(hour / 60),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            hour += 60
        }
    }
}

@Composable
private fun DayColumn(
    date: LocalDate,
    events: List<TimetableEvent>,
    courseCode: String?,
    titleFontSize: TextUnit,
    dayStart: Int,
    dayEnd: Int,
    totalHeight: Dp,
    columnWidth: Dp,
    onEventClick: (TimetableEvent) -> Unit,
) {
    Box(Modifier.width(columnWidth).height(totalHeight)) {
        if (date == LocalDate.now()) {
            val now = LocalTime.now()
            val nowMinutes = now.hour * 60 + now.minute
            if (nowMinutes in dayStart..dayEnd) {
                val y = MINUTE_HEIGHT * (nowMinutes - dayStart)
                Box(
                    Modifier
                        .offset(y = y)
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(MaterialTheme.colorScheme.error),
                )
            }
        }
        events.forEach { event ->
            val top = MINUTE_HEIGHT * (event.startMinutes - dayStart)
            val height = MINUTE_HEIGHT * (event.endMinutes - event.startMinutes).coerceAtLeast(15)
            val roomFontSize = (titleFontSize.value - 1f).coerceAtLeast(6f).sp
            Box(
                Modifier
                    .offset(y = top)
                    .padding(horizontal = 2.dp)
                    .width(columnWidth - 4.dp)
                    .heightIn(min = height)
                    .clip(MaterialTheme.shapes.medium)
                    .background(colorFor(event.title).copy(alpha = 0.9f))
                    .clickable { onEventClick(event) }
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Text(
                        event.shortTitle(courseCode),
                        style = WeekCardTitleStyle.copy(fontSize = titleFontSize, lineHeight = (titleFontSize.value * 1.15f).sp),
                        textAlign = TextAlign.Center,
                        color = Color.White,
                    )
                    event.shortRoom()?.let { room ->
                        Text(
                            room,
                            style = WeekCardRoomStyle.copy(fontSize = roomFontSize, lineHeight = (roomFontSize.value * 1.15f).sp),
                            textAlign = TextAlign.Center,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EventDetailDialog(event: TimetableEvent, onDismiss: () -> Unit) {
    // Samsung's own dialogs (e.g. Gallery "Details") use a plain near-black card, a bold
    // left-aligned title with no colored banner, and simple stacked label/value rows.
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Schließen", fontWeight = FontWeight.Bold) } },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        title = { Text(event.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "${event.day.germanLabel}, ${event.startLabel} – ${event.endLabel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                DetailRow("Turnus", event.frequency)
                DetailRow("Raum", event.room)
                DetailRow("Art", event.category)
                DetailRow("Dozent", event.lecturer)
                DetailRow("Start", event.startDate)
                DetailRow("Ende", event.endDate)
            }
        },
    )
}

/** OTA update prompt — download progress replaces the action buttons mid-download so there's
 *  no way to dismiss or double-trigger a download that's already running. */
@Composable
private fun UpdateDialog(
    info: UpdateInfo,
    downloadProgress: Float?,
    error: String?,
    canInstall: Boolean,
    onInstall: () -> Unit,
    onOpenInstallSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    val downloading = downloadProgress != null
    AlertDialog(
        onDismissRequest = { if (!downloading) onDismiss() },
        confirmButton = {
            TextButton(onClick = { if (canInstall) onInstall() else onOpenInstallSettings() }, enabled = !downloading) {
                Text(if (canInstall) "Update installieren" else "Berechtigung erteilen", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            if (!downloading) TextButton(onClick = onDismiss) { Text("Später") }
        },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        title = { Text("Update verfügbar: ${info.versionName}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (!info.releaseNotes.isNullOrBlank()) {
                    Text(info.releaseNotes, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                }
                if (!canInstall) {
                    Text(
                        "Android verlangt einmalig die Erlaubnis, Apps aus dieser App heraus zu installieren.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                downloadProgress?.let { progress ->
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                }
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
    )
}

@Composable
private fun DetailRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}
