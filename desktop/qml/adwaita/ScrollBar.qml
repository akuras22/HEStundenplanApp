import QtQuick
import QtQuick.Templates as T

// GTK overlay scrollbar: a thin translucent pill that only shows while scrolling or hovered, and
// thickens under the pointer.
T.ScrollBar {
    id: control

    implicitWidth: Math.max(implicitBackgroundWidth + leftInset + rightInset,
                            implicitContentWidth + leftPadding + rightPadding)
    implicitHeight: Math.max(implicitBackgroundHeight + topInset + bottomInset,
                             implicitContentHeight + topPadding + bottomPadding)

    padding: 3
    // Reported as not visible while there is nothing to scroll: Kirigami's sheets look at this
    // to decide whether their header needs a separator line.
    visible: control.policy === T.ScrollBar.AlwaysOn || (control.policy === T.ScrollBar.AsNeeded && control.size < 1.0)
    minimumSize: orientation === Qt.Horizontal ? height / width : width / height

    contentItem: Rectangle {
        readonly property int thickness: control.hovered || control.pressed ? 8 : 4
        implicitWidth: control.orientation === Qt.Vertical ? thickness : 40
        implicitHeight: control.orientation === Qt.Vertical ? 40 : thickness
        radius: Math.min(width, height) / 2
        color: Adw.overlay(control.pressed ? 0.6 : control.hovered ? 0.45 : 0.3)
        opacity: 0

        Behavior on implicitWidth { NumberAnimation { duration: 100 } }
        Behavior on implicitHeight { NumberAnimation { duration: 100 } }

        states: State {
            name: "active"
            when: control.policy === T.ScrollBar.AlwaysOn || (control.active && control.size < 1.0)
            PropertyChanges { control.contentItem.opacity: 1 }
        }
        transitions: Transition {
            from: "active"
            SequentialAnimation {
                PauseAnimation { duration: 600 }
                NumberAnimation { target: control.contentItem; property: "opacity"; to: 0; duration: 200 }
            }
        }
    }
}
