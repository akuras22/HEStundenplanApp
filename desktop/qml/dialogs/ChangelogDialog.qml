import QtQuick
import QtQuick.Layouts
import QtQuick.Controls as Controls
import org.kde.kirigami as Kirigami

Kirigami.OverlaySheet {
    id: root

    property var releaseInfo: ({})

    header: Kirigami.Heading {
        text: qsTr("Änderungsprotokoll")
        level: 2
    }

    ColumnLayout {
        Layout.preferredWidth: Kirigami.Units.gridUnit * 22
        spacing: Kirigami.Units.smallSpacing

        Controls.Label {
            Layout.fillWidth: true
            font.bold: true
            text: root.releaseInfo.versionName || ""
        }

        Controls.ScrollView {
            Layout.fillWidth: true
            Layout.preferredHeight: Kirigami.Units.gridUnit * 14
            Controls.Label {
                width: parent.width
                text: root.releaseInfo.releaseNotes || qsTr("Keine Angaben verfügbar.")
                wrapMode: Text.Wrap
            }
        }
    }
}
