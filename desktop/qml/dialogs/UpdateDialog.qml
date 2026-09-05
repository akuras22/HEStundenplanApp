import QtQuick
import QtQuick.Layouts
import QtQuick.Controls as Controls
import org.kde.kirigami as Kirigami

Kirigami.OverlaySheet {
    id: root

    property var updateInfo: ({})

    header: Kirigami.Heading {
        text: qsTr("Update verfügbar: %1").arg(root.updateInfo.versionName || "")
        level: 2
        wrapMode: Text.Wrap
    }

    ColumnLayout {
        Layout.preferredWidth: Kirigami.Units.gridUnit * 22
        spacing: Kirigami.Units.smallSpacing

        Controls.ScrollView {
            Layout.fillWidth: true
            Layout.preferredHeight: Kirigami.Units.gridUnit * 12
            Controls.Label {
                width: parent.width
                text: root.updateInfo.releaseNotes || qsTr("Keine Details verfügbar.")
                wrapMode: Text.Wrap
            }
        }

        Controls.Button {
            Layout.fillWidth: true
            text: qsTr("Release-Seite öffnen")
            icon.name: "internet-web-browser"
            onClicked: updateManager.openReleasePage(root.updateInfo.releaseUrl)
        }
    }
}
