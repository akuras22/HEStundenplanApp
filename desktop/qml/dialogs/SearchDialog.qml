import QtQuick
import QtQuick.Layouts
import QtQuick.Controls as Controls
import org.kde.kirigami as Kirigami
import de.hsesslingen.stundenplan.desktop
import "../components"

AppSheet {
    id: root

    signal eventSelected(var eventData)

    // On every opening, not just once at creation — the sheet is created with the page, long
    // before it is first shown.
    onOpened: queryField.forceActiveFocus()

    title: qsTr("Suche")

    ColumnLayout {
        Layout.preferredWidth: Kirigami.Units.gridUnit * 22
        spacing: Kirigami.Units.smallSpacing

        Controls.TextField {
            id: queryField
            Layout.fillWidth: true
            placeholderText: qsTr("Raum oder Dozent…")
            onTextChanged: resultsModel.refresh()
        }

        QtObject {
            id: resultsModel
            property var results: []
            function refresh() {
                results = timetableController.searchEvents(queryField.text)
            }
        }

        Controls.Label {
            visible: queryField.text.length > 0 && resultsModel.results.length === 0
            text: qsTr("Keine Treffer in der aktuellen Woche.")
            color: Kirigami.Theme.disabledTextColor
        }

        Repeater {
            model: resultsModel.results
            delegate: Controls.ItemDelegate {
                id: resultDelegate
                required property var modelData
                Layout.fillWidth: true

                contentItem: ColumnLayout {
                    spacing: 0
                    Controls.Label {
                        Layout.fillWidth: true
                        text: resultDelegate.modelData.title
                        font.bold: true
                    }
                    Controls.Label {
                        Layout.fillWidth: true
                        text: resultDelegate.modelData.dayLabel + ", " + resultDelegate.modelData.startLabel + "–" + resultDelegate.modelData.endLabel
                              + (resultDelegate.modelData.room ? " · " + resultDelegate.modelData.room : "")
                        color: Kirigami.Theme.disabledTextColor
                        font.pixelSize: Kirigami.Theme.smallFont.pixelSize
                    }
                }

                onClicked: {
                    root.eventSelected(modelData)
                    root.close()
                }
            }
        }
    }
}
