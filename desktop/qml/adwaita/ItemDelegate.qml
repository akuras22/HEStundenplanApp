import QtQuick
import QtQuick.Templates as T

T.ItemDelegate {
    id: control

    implicitWidth: Math.max(implicitBackgroundWidth + leftInset + rightInset,
                            implicitContentWidth + leftPadding + rightPadding)
    implicitHeight: Math.max(implicitBackgroundHeight + topInset + bottomInset,
                             implicitContentHeight + topPadding + bottomPadding,
                             implicitIndicatorHeight + topPadding + bottomPadding)

    horizontalPadding: 12
    verticalPadding: 8
    spacing: 12
    icon.width: 16
    icon.height: 16
    opacity: enabled ? 1 : 0.5

    contentItem: ButtonContent {
        control: control
        alignment: Qt.AlignLeft
    }

    background: Rectangle {
        implicitWidth: 100
        implicitHeight: 40
        color: Adw.overlay(control.down ? 0.12 : control.highlighted ? 0.1 : control.hovered ? 0.05 : 0)

        FocusRing {
            anchors.margins: 0
            visible: control.visualFocus
        }
    }
}
