import QtQuick
import QtQuick.Shapes
import QtQuick.Templates as T

// AdwSpinner: a single rotating arc.
T.BusyIndicator {
    id: control

    implicitWidth: Math.max(implicitBackgroundWidth + leftInset + rightInset,
                            implicitContentWidth + leftPadding + rightPadding)
    implicitHeight: Math.max(implicitBackgroundHeight + topInset + bottomInset,
                             implicitContentHeight + topPadding + bottomPadding)

    padding: 6

    contentItem: Item {
        implicitWidth: 32
        implicitHeight: 32
        opacity: control.running ? 1 : 0
        Behavior on opacity { NumberAnimation { duration: 150 } }

        Shape {
            id: arc
            readonly property real stroke: Math.max(2, width / 10)
            anchors.centerIn: parent
            width: Math.min(parent.width, parent.height)
            height: width
            preferredRendererType: Shape.CurveRenderer

            ShapePath {
                strokeColor: Adw.overlay(0.55)
                strokeWidth: arc.stroke
                fillColor: "transparent"
                capStyle: ShapePath.RoundCap
                PathAngleArc {
                    centerX: arc.width / 2
                    centerY: arc.height / 2
                    radiusX: (arc.width - arc.stroke) / 2
                    radiusY: radiusX
                    startAngle: 0
                    sweepAngle: 250
                }
            }

            RotationAnimator on rotation {
                running: control.running && control.visible
                from: 0
                to: 360
                duration: 900
                loops: Animation.Infinite
            }
        }
    }
}
