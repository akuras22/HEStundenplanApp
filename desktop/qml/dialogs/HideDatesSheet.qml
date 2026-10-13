import QtQuick
import QtQuick.Layouts
import QtQuick.Controls as Controls
import org.kde.kirigami as Kirigami
import de.hsesslingen.stundenplan.desktop
import "../components"

// "Termine ausblenden": pick any number of dates on which this weekly slot is hidden (e.g. every
// "Keine Vorlesung" Wednesday from the Vorlesungskalender). Dates already hidden come pre-checked,
// so unchecking one brings that lecture back. See TimetableEvent::candidateDates.
AppSheet {
    id: root

    property var eventData: ({})
    property var choices: []

    title: qsTr("Termine ausblenden")

    onOpened: choices = timetableController.occurrenceChoices(eventData)

    function setAll(checked) {
        for (let i = 0; i < dateRepeater.count; ++i)
            dateRepeater.itemAt(i).checked = checked
    }

    ColumnLayout {
        Layout.preferredWidth: Kirigami.Units.gridUnit * 20
        spacing: Kirigami.Units.smallSpacing

        Controls.Label {
            Layout.fillWidth: true
            text: (root.eventData.title || "") + " · " + (root.eventData.dayLabel || "") + ", "
                  + (root.eventData.startLabel || "") + " – " + (root.eventData.endLabel || "")
            color: AppTheme.disabledTextColor
            wrapMode: Text.Wrap
        }

        RowLayout {
            Layout.fillWidth: true
            Item { Layout.fillWidth: true }
            Controls.Button {
                text: qsTr("Keine")
                flat: true
                onClicked: root.setAll(false)
            }
            Controls.Button {
                text: qsTr("Alle")
                flat: true
                onClicked: root.setAll(true)
            }
        }

        Controls.Label {
            Layout.fillWidth: true
            visible: root.choices.length === 0
            text: qsTr("Keine Termine gefunden.")
        }

        ColumnLayout {
            Layout.fillWidth: true
            spacing: 0
            Repeater {
                id: dateRepeater
                model: root.choices
                delegate: Controls.CheckBox {
                    required property var modelData
                    Layout.fillWidth: true
                    text: modelData.label
                    font.bold: modelData.current
                    checked: modelData.hidden
                }
            }
        }

        RowLayout {
            Layout.fillWidth: true
            Item { Layout.fillWidth: true }
            Controls.Button {
                text: qsTr("Abbrechen")
                onClicked: root.close()
            }
            Controls.Button {
                text: qsTr("Speichern")
                highlighted: true
                onClicked: {
                    const hide = []
                    const show = []
                    for (let i = 0; i < dateRepeater.count; ++i)
                        (dateRepeater.itemAt(i).checked ? hide : show).push(root.choices[i].key)
                    timetableController.updateHiddenOccurrences(hide, show)
                    root.close()
                }
            }
        }
    }
}
