import QtQuick
import org.kde.kirigami as Kirigami

Banner {
    type: Kirigami.MessageType.Warning
    visible: timetableController.offline
    // loginRequired: QIS wanted a login rather than the connection failing, which only
    // Einstellungen ▸ Hochschul-Login fixes.
    text: (qisSession.loginRequired ? qsTr("Anmeldung nötig (Einstellungen ▸ Hochschul-Login) – zuletzt aktualisiert: %1")
                                    : qsTr("Offline-Ansicht – zuletzt aktualisiert: %1"))
          .arg(Qt.formatDateTime(new Date(timetableController.cacheTimestamp), "dd.MM.yyyy hh:mm"))
}
