import QtQuick
import QtQuick.Layouts
import QtQuick.Controls as Controls
import org.kde.kirigami as Kirigami
import "components"

// Single-day grid, swiped per weekday — same time-grid concept as WeekGridView but one wide
// column, plus a "starts in X min." countdown banner for today.
ColumnLayout {
    id: root

    property date currentDate
    signal eventClicked(var eventData)

    readonly property int dayStartMinutes: 8 * 60
    readonly property int dayEndMinutes: 19 * 60
    readonly property real pixelsPerMinute: 1.6
    readonly property real gridHeight: (dayEndMinutes - dayStartMinutes) * pixelsPerMinute
    readonly property bool isToday: {
        var now = new Date()
        return now.getFullYear() === currentDate.getFullYear() && now.getMonth() === currentDate.getMonth()
            && now.getDate() === currentDate.getDate()
    }
    readonly property int qtDayOfWeek: {
        var jsDay = currentDate.getDay() // 0=Sunday..6=Saturday
        return jsDay === 0 ? 7 : jsDay
    }

    spacing: Kirigami.Units.smallSpacing

    RowLayout {
        Layout.fillWidth: true
        Kirigami.Heading {
            level: 3
            text: Qt.formatDate(root.currentDate, "dddd, d. MMMM yyyy")
        }
        Item { Layout.fillWidth: true }
        Kirigami.Chip {
            visible: root.isToday
            text: qsTr("Heute")
            checkable: false
            closable: false
        }
    }

    Kirigami.InlineMessage {
        id: countdownBanner
        Layout.fillWidth: true
        type: Kirigami.MessageType.Positive
        visible: root.isToday && !!nextEvent.title
        text: nextEvent.title
              ? (nextEvent.minutesUntil <= 0
                 ? qsTr("Läuft gerade: %1").arg(nextEvent.title)
                 : qsTr("%1 in %2 Min.").arg(nextEvent.title).arg(nextEvent.minutesUntil))
              : ""

        readonly property var raw: root.isToday ? timetableController.nextEventToday() : ({})
        readonly property var nextEvent: {
            if (!raw || !raw.title)
                return ({})
            var now = new Date()
            var nowMinutes = now.getHours() * 60 + now.getMinutes()
            return { title: raw.title, minutesUntil: raw.startMinutes - nowMinutes }
        }
    }

    Controls.ScrollView {
        Layout.fillWidth: true
        Layout.fillHeight: true

        RowLayout {
            width: root.width
            spacing: Kirigami.Units.smallSpacing

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

            Item {
                id: dayColumn
                Layout.fillWidth: true
                Layout.preferredHeight: root.gridHeight

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
                    model: timetableController.eventsForDay(root.qtDayOfWeek)
                    delegate: EventBlock {
                        required property var modelData
                        eventData: modelData
                        width: dayColumn.width / modelData.columnCount
                        x: modelData.column * width
                        y: (modelData.startMinutes - root.dayStartMinutes) * root.pixelsPerMinute
                        height: Math.max(28, (modelData.endMinutes - modelData.startMinutes) * root.pixelsPerMinute)
                        onClicked: root.eventClicked(modelData)
                    }
                }

                NowLine {
                    visible: root.isToday
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
