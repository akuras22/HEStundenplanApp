import QtQuick
import QtQuick.Layouts
import org.kde.kirigami as Kirigami

Kirigami.InlineMessage {
    id: root
    type: Kirigami.MessageType.Warning
    visible: timetableController.offline
    text: qsTr("Offline-Ansicht – zuletzt aktualisiert: %1")
          .arg(Qt.formatDateTime(new Date(timetableController.cacheTimestamp), "dd.MM.yyyy hh:mm"))
    Layout.fillWidth: true
}
