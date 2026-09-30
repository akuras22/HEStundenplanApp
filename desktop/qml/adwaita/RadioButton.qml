import QtQuick
import QtQuick.Templates as T

T.RadioButton {
    id: control

    implicitWidth: Math.max(implicitBackgroundWidth + leftInset + rightInset,
                            implicitContentWidth + leftPadding + rightPadding)
    implicitHeight: Math.max(implicitBackgroundHeight + topInset + bottomInset,
                             implicitContentHeight + topPadding + bottomPadding,
                             implicitIndicatorHeight + topPadding + bottomPadding)

    padding: 6
    spacing: 8
    opacity: enabled ? 1 : 0.5

    indicator: Rectangle {
        implicitWidth: 20
        implicitHeight: 20
        x: control.text ? (control.mirrored ? control.width - width - control.rightPadding : control.leftPadding)
                        : control.leftPadding + (control.availableWidth - width) / 2
        y: control.topPadding + (control.availableHeight - height) / 2
        radius: 10
        color: control.checked ? (control.hovered ? Qt.lighter(Adw.accentBg, 1.1) : Adw.accentBg) : "transparent"
        border.width: control.checked ? 0 : 2
        border.color: Adw.overlay(control.down ? 0.4 : control.hovered ? 0.3 : 0.2)

        Rectangle {
            anchors.centerIn: parent
            visible: control.checked
            width: 8
            height: 8
            radius: 4
            color: Adw.accentFg
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
