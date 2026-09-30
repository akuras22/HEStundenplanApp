import QtQuick
import QtQuick.Shapes
import QtQuick.Templates as T

T.MenuItem {
    id: control

    implicitWidth: Math.max(implicitBackgroundWidth + leftInset + rightInset,
                            implicitContentWidth + leftPadding + rightPadding)
    implicitHeight: Math.max(implicitBackgroundHeight + topInset + bottomInset,
                             implicitContentHeight + topPadding + bottomPadding,
                             implicitIndicatorHeight + topPadding + bottomPadding)

    horizontalPadding: 12
    verticalPadding: 6
    spacing: 8
    icon.width: 16
    icon.height: 16
    opacity: enabled ? 1 : 0.5

    contentItem: ButtonContent {
        control: control
        alignment: Qt.AlignLeft
    }

    indicator: Shape {
        x: control.width - width - control.rightPadding
        y: control.topPadding + (control.availableHeight - height) / 2
        implicitWidth: 16
        implicitHeight: 16
        visible: control.checkable && control.checked
        preferredRendererType: Shape.CurveRenderer
        ShapePath {
            strokeColor: Adw.fg
            strokeWidth: 2
            fillColor: "transparent"
            capStyle: ShapePath.RoundCap
            joinStyle: ShapePath.RoundJoin
            startX: 3; startY: 8.5
            PathLine { x: 6.5; y: 12 }
            PathLine { x: 13; y: 4.5 }
        }
    }

    background: Rectangle {
        implicitWidth: 168
        implicitHeight: 32
        radius: 6
        color: Adw.overlay(control.down ? 0.16 : control.highlighted || control.hovered ? 0.08 : 0)
    }
}
