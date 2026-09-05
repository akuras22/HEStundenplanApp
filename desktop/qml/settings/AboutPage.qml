import QtQuick
import QtQuick.Layouts
import QtQuick.Controls as Controls
import org.kde.kirigami as Kirigami
import "../dialogs"

Kirigami.ScrollablePage {
    id: root
    title: qsTr("Über die App")

    UpdateDialog {
        id: updateDialog
    }
    ChangelogDialog {
        id: changelogDialog
    }

    Connections {
        target: updateManager
        function onUpdateAvailable(info) {
            updateDialog.updateInfo = info
            updateDialog.open()
        }
        function onReleaseNotesFetched(info) {
            changelogDialog.releaseInfo = info
            changelogDialog.open()
        }
        function onUpdateCheckFailed(message) {
            statusMessage.text = message
            statusMessage.visible = true
        }
    }

    ColumnLayout {
        width: root.width
        spacing: Kirigami.Units.largeSpacing

        RowLayout {
            Layout.fillWidth: true
            Kirigami.Icon {
                source: "org.hsesslingen.stundenplan.desktop"
                Layout.preferredWidth: Kirigami.Units.iconSizes.huge
                Layout.preferredHeight: Kirigami.Units.iconSizes.huge
            }
            ColumnLayout {
                Kirigami.Heading {
                    text: qsTr("HS Esslingen Stundenplan")
                    level: 2
                }
                Controls.Label {
                    text: qsTr("Version 0.1.0 (Desktop)")
                    color: Kirigami.Theme.disabledTextColor
                }
            }
        }

        Kirigami.InlineMessage {
            id: statusMessage
            Layout.fillWidth: true
            visible: false
            type: Kirigami.MessageType.Warning
        }

        Controls.Button {
            Layout.fillWidth: true
            text: qsTr("Nach Updates suchen")
            icon.name: "system-software-update"
            onClicked: updateManager.checkForUpdate()
        }
        Controls.Button {
            Layout.fillWidth: true
            text: qsTr("Änderungsprotokoll")
            icon.name: "documentinfo"
            onClicked: updateManager.fetchLatestReleaseNotes()
        }
        Controls.Button {
            Layout.fillWidth: true
            text: qsTr("Zwischenspeicher leeren")
            icon.name: "edit-clear-all"
            onClicked: {
                timetableController.clearCache()
                statusMessage.text = qsTr("Zwischenspeicher geleert.")
                statusMessage.type = Kirigami.MessageType.Positive
                statusMessage.visible = true
            }
        }
        Controls.Button {
            Layout.fillWidth: true
            text: qsTr("Quellcode")
            icon.name: "internet-web-browser"
            onClicked: updateManager.openReleasePage("https://github.com/akuras22/HEStundenplanApp")
        }
    }
}
