package de.hsesslingen.stundenplan.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import de.hsesslingen.stundenplan.MainActivity
import java.time.LocalDate

private const val CHANNEL_ID = "lecture_reminders"

/** Extra key MainActivity reads to jump straight to a specific day when a reminder notification
 *  (or the in-app "Test-Benachrichtigung senden" button) is tapped, instead of just opening to
 *  whatever was last shown. Value is a LocalDate in ISO format (LocalDate.toString()/parse()). */
const val EXTRA_OPEN_DATE = "open_date"

/** Posts "starts soon" notifications for upcoming lectures — see [LectureReminderWorker]. */
object NotificationHelper {

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Vorlesungserinnerungen",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Erinnert dich, wenn eine Veranstaltung in Kürze beginnt."
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun notifyUpcoming(context: Context, event: TimetableEvent, notificationId: Int, date: LocalDate) {
        // Silently does nothing if the user never granted the permission (or revoked it later) —
        // the worker doesn't need to know or care, it just tries again for the next lecture.
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val room = event.room?.substringAfterLast(" - ")?.trim()
        val text = buildString {
            append("Beginnt um ${event.startLabel} Uhr")
            if (!room.isNullOrBlank()) append(" in $room")
        }
        // Tapping the notification jumps straight to that day's Tag-Ansicht — MainActivity is
        // singleTask (see AndroidManifest) so this reuses the running instance via onNewIntent
        // rather than spawning a second one.
        val openIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_OPEN_DATE, date.toString())
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context, notificationId, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle(event.title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
