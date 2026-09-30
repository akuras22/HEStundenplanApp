import QtQuick
import QtQuick.Shapes
import QtQuick.Templates as T

T.CheckBox {
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
        radius: 6
        color: control.checkState !== Qt.Unchecked
               ? (control.hovered ? Qt.lighter(Adw.accentBg, 1.1) : Adw.accentBg) : "transparent"
        border.width: control.checkState === Qt.Unchecked ? 2 : 0
        border.color: Adw.overlay(control.down ? 0.4 : control.hovered ? 0.3 : 0.2)

        Shape {
            anchors.fill: parent
            visible: control.checkState === Qt.Checked
            preferredRendererType: Shape.CurveRenderer
            ShapePath {
                strokeColor: Adw.accentFg
                strokeWidth: 2
                fillColor: "transparent"
                capStyle: ShapePath.RoundCap
                joinStyle: ShapePath.RoundJoin
                startX: 5.5; startY: 10.5
                PathLine { x: 8.5; y: 13.5 }
                PathLine { x: 14.5; y: 7 }
            }
        }
        Rectangle {
            anchors.centerIn: parent
            visible: control.checkState === Qt.PartiallyChecked
            width: 10
            height: 2
            radius: 1
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
