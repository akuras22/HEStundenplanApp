import QtQuick
import QtQuick.Layouts
import org.kde.kirigami as Kirigami
import de.hsesslingen.stundenplan.desktop

// A small two-segment switch (Woche/Tag) that we fully paint ourselves, so the user's chosen
// accent color actually shows up here — Kirigami.NavigationTabBar looks native but always follows
// the system accent regardless of app-level overrides (see AppTheme.qml).
Rectangle {
    id: root

    property var model: []
    property int currentIndex: 0
    signal activated(int index)

    Layout.fillWidth: true
    implicitHeight: Kirigami.Units.gridUnit * 2.2
    radius: height / 2
    color: Qt.rgba(AppTheme.textColor.r, AppTheme.textColor.g, AppTheme.textColor.b, 0.08)
    border.color: Qt.rgba(AppTheme.textColor.r, AppTheme.textColor.g, AppTheme.textColor.b, 0.12)
    border.width: 1

    Row {
        anchors.fill: parent
        anchors.margins: 3
        spacing: 3

        Repeater {
            model: root.model
            delegate: Rectangle {
                id: segment
                required property int index
                required property var modelData
                readonly property bool selected: root.currentIndex === index

                width: (root.width - 6 - 3) / 2
                height: parent.height
                radius: height / 2
                color: selected ? AppTheme.accentColor : "transparent"

                Behavior on color { ColorAnimation { duration: 120 } }

                RowLayout {
                    anchors.centerIn: parent
                    spacing: Kirigami.Units.smallSpacing
                    Kirigami.Icon {
                        source: segment.modelData.icon
                        implicitWidth: Kirigami.Units.iconSizes.small
                        implicitHeight: Kirigami.Units.iconSizes.small
                        color: segment.selected ? "#ffffff" : AppTheme.textColor
                    }
                    Text {
                        text: segment.modelData.text
                        color: segment.selected ? "#ffffff" : AppTheme.textColor
                        font.bold: segment.selected
                    }
                }

                MouseArea {
                    anchors.fill: parent
                    onClicked: root.activated(segment.index)
                }
            }
        }
    }
}
