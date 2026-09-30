import QtQuick
import QtQuick.Templates as T

T.Switch {
    id: control

    implicitWidth: Math.max(implicitBackgroundWidth + leftInset + rightInset,
                            implicitContentWidth + leftPadding + rightPadding)
    implicitHeight: Math.max(implicitBackgroundHeight + topInset + bottomInset,
                             implicitContentHeight + topPadding + bottomPadding,
                             implicitIndicatorHeight + topPadding + bottomPadding)

    padding: 4
    spacing: 8
    opacity: enabled ? 1 : 0.5

    indicator: Rectangle {
        implicitWidth: 46
        implicitHeight: 26
        x: control.text ? (control.mirrored ? control.width - width - control.rightPadding : control.leftPadding)
                        : control.leftPadding + (control.availableWidth - width) / 2
        y: control.topPadding + (control.availableHeight - height) / 2
        radius: height / 2
        color: control.checked ? (control.hovered ? Qt.lighter(Adw.accentBg, 1.1) : Adw.accentBg)
                               : Adw.overlay(control.hovered ? 0.2 : 0.15)
        Behavior on color { ColorAnimation { duration: 150 } }

        Rectangle {
            x: Math.max(3, Math.min(parent.width - width - 3, control.visualPosition * parent.width - width / 2))
            y: 3
            width: 20
            height: 20
            radius: 10
            color: "#ffffff"
            border.width: 1
            border.color: Qt.rgba(0, 0, 0, 0.1)
            Behavior on x {
                enabled: !control.down
                NumberAnimation { duration: 150; easing.type: Easing.OutCubic }
            }
        }

        FocusRing { visible: control.visualFocus }
    }

    contentItem: Text {
        leftPadding: control.indicator && !control.mirrored ? control.indicator.width + control.spacing : 0
        rightPadding: control.indicator && control.mirrored ? control.indicator.width + control.spacing : 0
        text: control.text
        font: control.font
        color: Adw.fg
        elide: Text.ElideRight
        verticalAlignment: Text.AlignVCenter
    }
}
