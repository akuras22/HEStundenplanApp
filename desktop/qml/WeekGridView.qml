import QtQuick
import QtQuick.Layouts
import QtQuick.Controls as Controls
import org.kde.kirigami as Kirigami
import "components"

// Untis-style week grid: 5 weekday columns x hour rows, overlapping events packed side-by-side
// (see TimetableController::eventsForDay, which already applies the overlap layout).
ColumnLayout {
    id: root

    property date weekMonday
    signal eventClicked(var eventData)

    readonly property int dayStartMinutes: 8 * 60
    readonly property int dayEndMinutes: 19 * 60
    readonly property real pixelsPerMinute: 1.1
    readonly property real gridHeight: (dayEndMinutes - dayStartMinutes) * pixelsPerMinute
    readonly property var dayNames: [qsTr("Mo"), qsTr("Di"), qsTr("Mi"), qsTr("Do"), qsTr("Fr")]

    function dateForColumn(index) {
        var d = new Date(weekMonday)
        d.setDate(d.getDate() + index)
        return d
    }

    function isSameDate(a, b) {
        return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate()
    }

    spacing: 0

    RowLayout {
        Layout.fillWidth: true
        spacing: Kirigami.Units.smallSpacing
        Item { Layout.preferredWidth: Kirigami.Units.gridUnit * 2.5 }
        Repeater {
            model: 5
            delegate: DateChip {
                Layout.fillWidth: true
                dayLabel: root.dayNames[index]
                dateLabel: root.dateForColumn(index).getDate().toString()
                isToday: root.isSameDate(root.dateForColumn(index), new Date())
            }
        }
    }

    Controls.ScrollView {
        Layout.fillWidth: true
        Layout.fillHeight: true

        RowLayout {
            width: root.width
            spacing: Kirigami.Units.smallSpacing

            // Time axis
            ColumnLayout {
                Layout.preferredWidth: Kirigami.Units.gridUnit * 2.5
                Layout.alignment: Qt.AlignTop
                spacing: 0
                Repeater {
                    model: (root.dayEndMinutes - root.dayStartMinutes) / 60
                    delegate: Item {
                        Layout.preferredWidth: Kirigami.Units.gridUnit * 2.5
                        Layout.preferredHeight: 60 * root.pixelsPerMinute
                        Controls.Label {
                            anchors.top: parent.top
                            anchors.right: parent.right
                            text: (root.dayStartMinutes / 60 + index) + ":00"
                            font.pixelSize: Kirigami.Theme.smallFont.pixelSize
                            color: Kirigami.Theme.disabledTextColor
                        }
                    }
                }
            }

            Repeater {
                model: 5
                delegate: Item {
                    id: dayColumn
                    required property int index
                    Layout.fillWidth: true
                    Layout.preferredHeight: root.gridHeight

                    // Hour gridlines
                    Column {
                        anchors.fill: parent
                        Repeater {
                            model: (root.dayEndMinutes - root.dayStartMinutes) / 60
                            delegate: Rectangle {
                                width: dayColumn.width
                                height: 60 * root.pixelsPerMinute
                                color: "transparent"
                                Rectangle {
                                    width: parent.width
                                    height: 1
                                    color: Qt.rgba(Kirigami.Theme.textColor.r, Kirigami.Theme.textColor.g, Kirigami.Theme.textColor.b, 0.15)
                                }
                            }
                        }
                    }

                    Repeater {
                        model: timetableController.eventsForDay(dayColumn.index + 1)
                        delegate: EventBlock {
                            required property var modelData
                            eventData: modelData
                            width: dayColumn.width / modelData.columnCount
                            x: (modelData.column) * width
                            y: (modelData.startMinutes - root.dayStartMinutes) * root.pixelsPerMinute
                            height: Math.max(24, (modelData.endMinutes - modelData.startMinutes) * root.pixelsPerMinute)
                            onClicked: root.eventClicked(modelData)
                        }
                    }

                    NowLine {
                        visible: root.isSameDate(root.dateForColumn(dayColumn.index), new Date())
                        width: dayColumn.width
                        y0: {
                            var now = new Date()
                            return (now.getHours() * 60 + now.getMinutes() - root.dayStartMinutes) * root.pixelsPerMinute
                        }
                    }
                }
            }
        }
    }
}
