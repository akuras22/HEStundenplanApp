import QtQuick
import QtQuick.Layouts
import org.kde.kirigami as Kirigami
import de.hsesslingen.stundenplan.desktop

// A small segmented switch (Woche/Tag) that we fully paint ourselves.
// KDE look: a pill whose selected segment is filled with the accent color.
// GNOME look: an AdwToggleGroup — a gray trough whose selected segment is a raised neutral tile,
// text only.
Rectangle {
    id: root

    property var model: []
    property int currentIndex: 0
    signal activated(int index)

    readonly property int inset: AppTheme.gnome ? 2 : 3

    Layout.fillWidth: true
    implicitWidth: Kirigami.Units.gridUnit * 12
    implicitHeight: AppTheme.gnome ? 34 : Kirigami.Units.gridUnit * 2.2
    radius: AppTheme.gnome ? AppTheme.controlRadius : height / 2
    color: Qt.rgba(AppTheme.textColor.r, AppTheme.textColor.g, AppTheme.textColor.b, AppTheme.gnome ? 0.1 : 0.08)
    border.color: Qt.rgba(AppTheme.textColor.r, AppTheme.textColor.g, AppTheme.textColor.b, 0.12)
    border.width: AppTheme.gnome ? 0 : 1

    Row {
        anchors.fill: parent
        anchors.margins: root.inset
        spacing: root.inset

        Repeater {
            model: root.model
            delegate: Rectangle {
                id: segment
                required property int index
                required property var modelData
                readonly property bool selected: root.currentIndex === index
                readonly property color contentColor: selected && !AppTheme.gnome ? "#ffffff" : AppTheme.textColor
                readonly property string iconName: AppTheme.icon(modelData.icon ?? "")

                width: (parent.width - (root.model.length - 1) * root.inset) / root.model.length
                height: parent.height
                radius: AppTheme.gnome ? root.radius - root.inset : height / 2
                color: !selected ? (hover.hovered && AppTheme.gnome ? Qt.rgba(AppTheme.textColor.r, AppTheme.textColor.g, AppTheme.textColor.b, 0.06) : "transparent")
                     : !AppTheme.gnome ? AppTheme.accentColor
                     : AppTheme.dark ? Qt.rgba(1, 1, 1, 0.2) : "#ffffff"
                border.width: selected && AppTheme.gnome && !AppTheme.dark ? 1 : 0
                border.color: Qt.rgba(0, 0, 0, 0.1)

                Behavior on color { ColorAnimation { duration: 120 } }

                RowLayout {
                    anchors.centerIn: parent
                    spacing: Kirigami.Units.smallSpacing
                    Kirigami.Icon {
                        visible: segment.iconName !== ""
                        source: segment.iconName
                        implicitWidth: Kirigami.Units.iconSizes.small
                        implicitHeight: Kirigami.Units.iconSizes.small
                        color: segment.contentColor
                    }
                    Text {
                        text: segment.modelData.text
                        color: segment.contentColor
                        font.bold: segment.selected || AppTheme.gnome
                    }
                }

                HoverHandler {
                    id: hover
                }
                MouseArea {
                    anchors.fill: parent
                    onClicked: root.activated(segment.index)
                }
            }
        }
    }
}
