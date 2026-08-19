package de.hsesslingen.stundenplan.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import de.hsesslingen.stundenplan.data.NextEventResult
import de.hsesslingen.stundenplan.data.SettingsStore
import de.hsesslingen.stundenplan.data.TimetableCache
import de.hsesslingen.stundenplan.data.findNextEvent
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDateTime

/**
 * Home-screen widget showing the next upcoming lecture. Deliberately reads from [TimetableCache]
 * only — never a live network fetch — so a widget refresh (which the OS can trigger at any time,
 * including while the device is idle) always stays fast and never blocks on a slow/offline
 * request. The cache is kept warm by StundenplanViewModel on every successful in-app fetch, and
 * this widget is explicitly refreshed right after (see WidgetUpdater).
 */
class NextLectureWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settingsStore = SettingsStore(context)
        val studiengang = settingsStore.selectedStudiengang.first()

        if (studiengang == null) {
            provideContent { GlanceTheme { WidgetMessage("Kein Studiengang ausgewählt") } }
            return
        }

        val cache = TimetableCache(context)
        val hiddenKeys = settingsStore.hiddenEventKeys.first()
        val now = LocalDateTime.now()
        val thisMonday = now.toLocalDate().with(DayOfWeek.MONDAY)
        val nextMonday = thisMonday.plusWeeks(1)
        val events = (cache.get(studiengang, thisMonday)?.events ?: emptyList()) +
            (cache.get(studiengang, nextMonday)?.events ?: emptyList())
        val next = findNextEvent(events, now, hiddenKeys)

        provideContent {
            GlanceTheme {
                when {
                    next != null -> WidgetNextEvent(studiengang.code, next)
                    events.isEmpty() -> WidgetMessage("Noch keine Daten – App öffnen")
                    else -> WidgetMessage("Keine weiteren Veranstaltungen")
                }
            }
        }
    }
}

@Composable
private fun WidgetMessage(text: String) {
    Column(
        modifier = GlanceModifier.fillMaxSize().background(GlanceTheme.colors.background).padding(12.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
    ) {
        Text(
            text,
            style = TextStyle(fontSize = 13.sp, color = GlanceTheme.colors.onBackground),
        )
    }
}

@Composable
private fun WidgetNextEvent(courseCode: String, next: NextEventResult) {
    val event = next.event
    val room = event.room?.substringAfterLast(" - ")?.trim()
    Column(
        modifier = GlanceModifier.fillMaxSize().background(GlanceTheme.colors.background).padding(12.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(
            "$courseCode · ${event.day.germanLabel}",
            style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.onSurfaceVariant),
        )
        Text(
            event.title,
            maxLines = 2,
            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onBackground),
        )
        Text(
            buildString {
                append(event.startLabel)
                append(" – ")
                append(event.endLabel)
                if (!room.isNullOrBlank()) { append(" · "); append(room) }
            },
            style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant),
        )
    }
}
