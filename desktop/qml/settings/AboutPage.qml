import QtQuick
import QtQuick.Layouts
import QtQuick.Controls as Controls
import org.kde.kirigami as Kirigami
import de.hsesslingen.stundenplan.desktop
import "../components"
import "../dialogs"

PrefPage {
    id: root
    title: qsTr("Über die App")

    UpdateDialog {
        id: updateDialog
    }
    ChangelogDialog {
        id: changelogDialog
    }

    function showStatus(text, type) {
        statusMessage.text = text
        statusMessage.type = type
        statusMessage.visible = true
        statusHideTimer.restart()
    }

    Timer {
        id: statusHideTimer
        interval: 4000
        onTriggered: statusMessage.visible = false
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
            root.showStatus(message, Kirigami.MessageType.Warning)
        }
        function onAlreadyUpToDate() {
            root.showStatus(qsTr("Du hast bereits die neueste Version."), Kirigami.MessageType.Positive)
        }
    }

    ColumnLayout {
        Layout.fillWidth: true
        Layout.topMargin: Kirigami.Units.largeSpacing
        spacing: Kirigami.Units.smallSpacing

        Kirigami.Icon {
            source: "org.hsesslingen.stundenplan.desktop"
            Layout.alignment: Qt.AlignHCenter
            Layout.preferredWidth: Kirigami.Units.iconSizes.enormous
            Layout.preferredHeight: Kirigami.Units.iconSizes.enormous
        }
        Kirigami.Heading {
            Layout.fillWidth: true
            horizontalAlignment: Text.AlignHCenter
            text: qsTr("HS Esslingen Stundenplan")
            level: 1
            font.weight: Font.Bold
            wrapMode: Text.Wrap
        }
        Controls.Label {
            Layout.fillWidth: true
            horizontalAlignment: Text.AlignHCenter
            text: qsTr("Version %1 (Desktop)").arg(updateManager.appVersionName)
            color: AppTheme.disabledTextColor
        }
    }

    Banner {
        id: statusMessage
        visible: false
        type: Kirigami.MessageType.Warning
    }

    PrefGroup {
        PrefRow {
            text: qsTr("Nach Updates suchen")
            icon.name: AppTheme.icon("system-software-update")
            activatable: true
            onClicked: updateManager.checkForUpdate()
        }
        PrefRow {
            text: qsTr("Änderungsprotokoll")
            icon.name: AppTheme.icon("documentinfo")
            activatable: true
            onClicked: updateManager.fetchLatestReleaseNotes()
        }
        PrefRow {
            text: qsTr("Quellcode")
            icon.name: AppTheme.icon("internet-web-browser")
            activatable: true
            navigates: true
            onClicked: updateManager.openReleasePage("https://github.com/akuras22/HEStundenplanApp")
        }
    }

    PrefGroup {
        PrefRow {
            text: qsTr("Zwischenspeicher leeren")
            subtitle: qsTr("Gespeicherte Stundenpläne und Speiseplan-Fotos entfernen; sie werden beim nächsten Öffnen neu geladen")
            icon.name: AppTheme.icon("edit-clear-all")
            activatable: true
            onClicked: {
                timetableController.clearCache()
                mensaController.clearCache()
                root.showStatus(qsTr("Zwischenspeicher geleert."), Kirigami.MessageType.Positive)
            }
        }
    }
}
