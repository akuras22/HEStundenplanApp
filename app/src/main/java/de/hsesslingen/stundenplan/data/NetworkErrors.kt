package de.hsesslingen.stundenplan.data

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Translates low-level network exceptions into short, actionable German messages instead of
 *  showing raw technical text ("HTTP 503 beim Laden von https://www3.hs-esslingen.de/...") to the
 *  user, who has no way to act on a URL or exception name anyway. */
fun friendlyNetworkErrorMessage(e: Exception): String = when (e) {
    is UnknownHostException -> "Keine Internetverbindung."
    is SocketTimeoutException -> "Die Hochschul-Seite antwortet nicht. Bitte später erneut versuchen."
    is IOException -> {
        val httpCode = Regex("""HTTP (\d{3})""").find(e.message.orEmpty())?.groupValues?.get(1)
        if (httpCode != null) {
            "Die Hochschul-Seite ist gerade nicht erreichbar (Fehler $httpCode)."
        } else {
            "Verbindung zur Hochschul-Seite fehlgeschlagen."
        }
    }
    else -> "Stundenplan konnte nicht geladen werden."
}
