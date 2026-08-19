package de.hsesslingen.stundenplan.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

/** Called by StundenplanViewModel after a successful fetch so any placed widget instance reflects
 *  new data immediately, instead of waiting for the OS's own (30-minute-minimum) update cycle.
 *  updateAll() is a no-op if the widget hasn't been added to any home screen. */
object WidgetUpdater {
    suspend fun refresh(context: Context) {
        NextLectureWidget().updateAll(context)
    }
}
