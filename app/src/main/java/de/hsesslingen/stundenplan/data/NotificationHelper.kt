package de.hsesslingen.stundenplan.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

private const val CHANNEL_ID = "lecture_reminders"

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

    fun notifyUpcoming(context: Context, event: TimetableEvent, notificationId: Int) {
        // Silently does nothing if the user never granted the permission (or revoked it later) —
        // the worker doesn't need to know or care, it just tries again for the next lecture.
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val room = event.room?.substringAfterLast(" - ")?.trim()
        val text = buildString {
            append("Beginnt um ${event.startLabel} Uhr")
            if (!room.isNullOrBlank()) append(" in $room")
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle(event.title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
