package de.hsesslingen.stundenplan.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import de.hsesslingen.stundenplan.data.AccentPreset
import de.hsesslingen.stundenplan.data.NextEventResult
import de.hsesslingen.stundenplan.data.SettingsStore
import de.hsesslingen.stundenplan.data.ThemeMode
import de.hsesslingen.stundenplan.data.TimetableCache
import de.hsesslingen.stundenplan.data.findUpcomingEvents
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDateTime

// The two sizes a placed widget is rounded to for layout purposes (see GlanceAppWidget.sizeMode
// below) — small keeps today's original single-event card, large (roughly a 4x2+ placement) has
// room to list the next few events instead of just one.
private val SMALL_WIDGET_SIZE = DpSize(180.dp, 90.dp)
private val LARGE_WIDGET_SIZE = DpSize(250.dp, 180.dp)

/** The widget's own resolved palette — mirrors StundenplanTheme's accent/background logic (see
 *  ui/theme/Theme.kt) so the widget doesn't look out of place next to a customized in-app theme.
 *  Computed directly from settings rather than going through Glance's ColorProviders/GlanceTheme
 *  machinery, since that's built around Material You's day/night resource system, not an
 *  arbitrary user-picked color. */
private data class WidgetPalette(val background: Color, val onBackground: Color, val onSurfaceVariant: Color, val accent: Color)

private suspend fun resolvePalette(context: Context, settingsStore: SettingsStore): WidgetPalette {
    val isDark = when (settingsStore.themeMode.first()) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }
    val accentPreset = settingsStore.accentPreset.first()
    val accent = when {
        accentPreset == AccentPreset.CUSTOM -> settingsStore.customAccentColor.first()
        accentPreset != AccentPreset.DEFAULT -> accentPreset.color
        else -> null
    } ?: if (isDark) Color(0xFF4DA3FF) else Color(0xFF0381FE)
    val background = settingsStore.customBackgroundColor.first()
        ?: if (isDark) Color(0xFF0A0A0A) else Color(0xFFFFFFFF)
    val onBackground = if (background.luminance() > 0.5f) Color(0xFF17171A) else Color(0xFFF2F2F2)
    val onSurfaceVariant = onBackground.copy(alpha = 0.7f)
    return WidgetPalette(background, onBackground, onSurfaceVariant, accent)
}

/**
 * Home-screen widget showing the next upcoming lecture. Deliberately reads from [TimetableCache]
 * only — never a live network fetch — so a widget refresh (which the OS can trigger at any time,
 * including while the device is idle) always stays fast and never blocks on a slow/offline
 * request. The cache is kept warm by StundenplanViewModel on every successful in-app fetch, and
 * this widget is explicitly refreshed right after (see WidgetUpdater).
 */
class NextLectureWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(SMALL_WIDGET_SIZE, LARGE_WIDGET_SIZE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settingsStore = SettingsStore(context)
        val palette = resolvePalette(context, settingsStore)
        val studiengang = settingsStore.selectedStudiengang.first()

        if (studiengang == null) {
            provideContent { WidgetMessage("Kein Studiengang ausgewählt", palette) }
            return
        }

        val cache = TimetableCache(context)
        val hiddenKeys = settingsStore.hiddenEventKeys.first()
        val now = LocalDateTime.now()
        val thisMonday = now.toLocalDate().with(DayOfWeek.MONDAY)
        val nextMonday = thisMonday.plusWeeks(1)
        val events = (cache.get(studiengang, thisMonday)?.events ?: emptyList()) +
            (cache.get(studiengang, nextMonday)?.events ?: emptyList())
        val upcoming = findUpcomingEvents(events, now, hiddenKeys, count = 3)

        provideContent {
            val isLarge = LocalSize.current.height >= LARGE_WIDGET_SIZE.height
            when {
                upcoming.isEmpty() && events.isEmpty() -> WidgetMessage("Noch keine Daten – App öffnen", palette)
                upcoming.isEmpty() -> WidgetMessage("Keine weiteren Veranstaltungen", palette)
                isLarge -> WidgetUpcomingEvents(studiengang.code, upcoming, palette)
                else -> WidgetNextEvent(studiengang.code, upcoming.first(), palette)
            }
        }
    }
}

@Composable
private fun WidgetMessage(text: String, palette: WidgetPalette) {
    Column(
        modifier = GlanceModifier.fillMaxSize().background(palette.background).padding(12.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
    ) {
        Text(
            text,
            style = TextStyle(fontSize = 13.sp, color = ColorProvider(palette.onSurfaceVariant)),
        )
    }
}

/** Large-size layout — lists the next few events instead of just one, since there's room for it. */
@Composable
private fun WidgetUpcomingEvents(courseCode: String, upcoming: List<NextEventResult>, palette: WidgetPalette) {
    Column(
        modifier = GlanceModifier.fillMaxSize().background(palette.background).padding(12.dp),
    ) {
        Text(
            courseCode,
            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ColorProvider(palette.accent)),
        )
        Spacer(GlanceModifier.height(6.dp))
        upcoming.forEachIndexed { index, result ->
            if (index > 0) Spacer(GlanceModifier.height(8.dp))
            WidgetUpcomingEventRow(result, palette)
        }
    }
}

@Composable
private fun WidgetUpcomingEventRow(result: NextEventResult, palette: WidgetPalette) {
    val event = result.event
    val room = event.room?.substringAfterLast(" - ")?.trim()
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Text(
            event.title,
            maxLines = 1,
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ColorProvider(palette.onBackground)),
        )
        Text(
            buildString {
                append(event.day.germanLabel)
                append(", ")
                append(event.startLabel)
                append(" – ")
                append(event.endLabel)
                if (!room.isNullOrBlank()) { append(" · "); append(room) }
            },
            maxLines = 1,
            style = TextStyle(fontSize = 11.sp, color = ColorProvider(palette.onSurfaceVariant)),
        )
    }
}

@Composable
private fun WidgetNextEvent(courseCode: String, next: NextEventResult, palette: WidgetPalette) {
    val event = next.event
    val room = event.room?.substringAfterLast(" - ")?.trim()
    Column(
        modifier = GlanceModifier.fillMaxSize().background(palette.background).padding(12.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(
            "$courseCode · ${event.day.germanLabel}",
            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ColorProvider(palette.accent)),
        )
        Text(
            event.title,
            maxLines = 2,
            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ColorProvider(palette.onBackground)),
        )
        Text(
            buildString {
                append(event.startLabel)
                append(" – ")
                append(event.endLabel)
                if (!room.isNullOrBlank()) { append(" · "); append(room) }
            },
            style = TextStyle(fontSize = 12.sp, color = ColorProvider(palette.onSurfaceVariant)),
        )
    }
}
