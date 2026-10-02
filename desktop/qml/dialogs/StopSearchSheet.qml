import QtQuick
import QtQuick.Layouts
import QtQuick.Controls as Controls
import org.kde.kirigami as Kirigami
import de.hsesslingen.stundenplan.desktop
import "../components"

// "Andere Haltestelle suchen": any stop in the VVS network, searched as you type.
AppSheet {
    id: root

    title: qsTr("Haltestelle suchen")

    onOpened: {
        searchField.text = ""
        searchField.forceActiveFocus()
    }

    ColumnLayout {
        Layout.preferredWidth: Kirigami.Units.gridUnit * 22
        spacing: Kirigami.Units.smallSpacing

        Controls.TextField {
            id: searchField
            Layout.fillWidth: true
            placeholderText: qsTr("z. B. Esslingen Bahnhof")
            // Waits for typing to pause instead of searching on every keystroke.
            onTextChanged: searchDelay.restart()
        }
        Timer {
            id: searchDelay
            interval: 300
            onTriggered: transitController.search(searchField.text)
        }

        Controls.Label {
            Layout.fillWidth: true
            visible: !!transitController.searchError
            text: transitController.searchError
            color: AppTheme.errorColor
            wrapMode: Text.Wrap
        }
        Controls.Label {
            Layout.fillWidth: true
            visible: searchField.text.trim().length >= 2 && !transitController.searching
                     && !transitController.searchError && transitController.searchResults.length === 0
            text: qsTr("Keine Haltestelle gefunden")
            color: AppTheme.disabledTextColor
        }
        Controls.BusyIndicator {
            Layout.alignment: Qt.AlignHCenter
            visible: transitController.searching && transitController.searchResults.length === 0
            running: visible
        }

        Repeater {
            model: searchField.text.trim().length >= 2 ? transitController.searchResults : []
            delegate: Controls.ItemDelegate {
                required property var modelData
                Layout.fillWidth: true
                text: modelData.name
                onClicked: {
                    transitController.setStop(modelData.id, modelData.name, modelData.shortName)
                    root.close()
                }
            }
        }
    }
}
