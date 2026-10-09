import QtQuick
import QtQuick.Layouts
import QtQuick.Controls as Controls
import org.kde.kirigami as Kirigami
import de.hsesslingen.stundenplan.desktop
import "../components"

PrefPage {
    id: root
    title: qsTr("Hochschul-Login")

    Component { id: loginWebPage; LoginWebPage {} }

    function formatTime(date) {
        const sameDay = date.toDateString() === new Date().toDateString()
        return Qt.formatDateTime(date, sameDay ? "hh:mm" : "dd.MM. hh:mm")
    }

    PrefGroup {
        PrefRow {
            text: !qisSession.hasLogin ? qsTr("Nicht angemeldet")
                : qisSession.expired ? qsTr("Anmeldung abgelaufen")
                : qsTr("Angemeldet")
            subtitle: !qisSession.hasLogin ? qsTr("Der Stundenplan wird ohne Anmeldung geladen.")
                : qisSession.expired ? qsTr("Seit %1 angemeldet, inzwischen abgelaufen. Der Stundenplan wird wieder ohne Anmeldung geladen.")
                                           .arg(root.formatTime(qisSession.loggedInAt))
                : qsTr("Seit %1. Verlängert sich automatisch ohne erneute Anmeldung, bis spätestens %2.")
                      .arg(root.formatTime(qisSession.loggedInAt)).arg(root.formatTime(qisSession.ssoExpiresAt))
        }
    }

    PrefGroup {
        PrefRow {
            text: qisSession.hasLogin ? qsTr("Erneut anmelden") : qsTr("Anmelden")
            subtitle: qsTr("Auf der Login-Seite der Hochschule, mit zweitem Faktor")
            icon.name: AppTheme.icon("dialog-password")
            activatable: true
            navigates: true
            onClicked: applicationWindow().pageStack.push(loginWebPage)
        }
        PrefRow {
            visible: qisSession.hasLogin
            text: qsTr("Abmelden")
            subtitle: qsTr("Sitzung aus der App entfernen")
            icon.name: AppTheme.icon("system-log-out")
            activatable: true
            onClicked: qisSession.logout()
        }
    }

    Controls.Label {
        Layout.fillWidth: true
        Layout.leftMargin: AppTheme.gnome ? 0 : Kirigami.Units.largeSpacing
        Layout.rightMargin: Layout.leftMargin
        wrapMode: Text.Wrap
        color: AppTheme.disabledTextColor
        font.pixelSize: Kirigami.Theme.smallFont.pixelSize
        text: qsTr("Normalerweise nicht nötig: Der Stundenplan ist öffentlich und lädt auch ohne Anmeldung. "
                   + "Angemeldet lädt die App ihn verschlüsselt über https – und auch dann, wenn die Hochschule "
                   + "den Zugang ohne Anmeldung sperrt.\n\n"
                   + "Dein Passwort gibst du direkt auf der Login-Seite der Hochschule ein; die App speichert es "
                   + "nicht, nur die Sitzung. Sie erneuert sich danach von selbst – ohne erneute Anmeldung – bis zu "
                   + "rund 10 Stunden nach dem Login; danach ist eine neue Anmeldung nötig.")
    }
}
