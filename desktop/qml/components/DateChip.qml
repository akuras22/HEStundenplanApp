import QtQuick
import QtQuick.Layouts
import QtQuick.Controls as Controls
import org.kde.kirigami as Kirigami
import de.hsesslingen.stundenplan.desktop

// One weekday header chip in the week grid ("Mo 12."-style), highlighted when it is today.
RowLayout {
    id: root

    property alias dayLabel: dayText.text
    property alias dateLabel: dateText.text
    property bool isToday: false

    spacing: Kirigami.Units.smallSpacing

    Rectangle {
        Layout.fillWidth: true
        implicitHeight: Kirigami.Units.gridUnit * 2.2
        radius: Kirigami.Units.cornerRadius
        color: root.isToday ? AppTheme.accentColor : "transparent"

        ColumnLayout {
            anchors.centerIn: parent
            spacing: 0
            Controls.Label {
                id: dayText
                Layout.alignment: Qt.AlignHCenter
                font.pixelSize: Kirigami.Theme.smallFont.pixelSize
                color: root.isToday ? "#ffffff" : Kirigami.Theme.disabledTextColor
            }
            Controls.Label {
                id: dateText
                Layout.alignment: Qt.AlignHCenter
                font.bold: true
                color: root.isToday ? "#ffffff" : Kirigami.Theme.textColor
            }
        }
    }
}
