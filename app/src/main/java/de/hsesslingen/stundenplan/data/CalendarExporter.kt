package de.hsesslingen.stundenplan.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Writes the currently loaded week's events out as an .ics file and hands it to the system share
 * sheet so the user can import it into whichever calendar app they actually use. Works for the
 * whole semester from just one week's events — see [buildIcs] for why.
 */
class CalendarExporter(private val context: Context) {
    fun shareAsIcs(events: List<TimetableEvent>, calendarName: String) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val fileName = calendarName.ifBlank { "Stundenplan" }.filter { it.isLetterOrDigit() }.ifBlank { "Stundenplan" }
        val file = File(dir, "$fileName.ics")
        file.writeText(buildIcs(events, calendarName))

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/calendar"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        // context here may be the Application context (see StundenplanViewModel), which requires
        // FLAG_ACTIVITY_NEW_TASK on any startActivity() call — the same gotcha that previously
        // crashed the OTA "open in browser" button (see UpdateManager).
        val chooser = Intent.createChooser(sendIntent, "Stundenplan exportieren").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
