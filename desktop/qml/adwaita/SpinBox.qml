import QtQuick
import QtQuick.Templates as T

// GtkSpinButton layout: the value on the left, flat "−" and "+" buttons at the right end.
T.SpinBox {
    id: control

    implicitWidth: Math.max(implicitBackgroundWidth + leftInset + rightInset,
                            contentItem.implicitWidth + leftPadding + rightPadding)
    implicitHeight: Math.max(implicitBackgroundHeight + topInset + bottomInset,
                             implicitContentHeight + topPadding + bottomPadding)

    leftPadding: 9
    rightPadding: 2 * Adw.controlHeight + 4
    opacity: enabled ? 1 : 0.5

    validator: IntValidator {
        locale: control.locale.name
        bottom: Math.min(control.from, control.to)
        top: Math.max(control.from, control.to)
    }

    contentItem: TextInput {
        z: 2
        text: control.displayText
        font: control.font
        color: Adw.fg
        selectionColor: Qt.alpha(Adw.accentBg, 0.35)
        selectedTextColor: color
        verticalAlignment: Qt.AlignVCenter
        readOnly: !control.editable
        validator: control.validator
        inputMethodHints: control.inputMethodHints
        clip: width < implicitWidth
    }

    component StepButton: Item {
        id: stepButton
        required property bool plus
        required property bool hovered
        required property bool pressed

        implicitWidth: Adw.controlHeight
        implicitHeight: Adw.controlHeight
        height: control.height
        opacity: enabled ? 1 : 0.4

        Rectangle {
            anchors.fill: parent
            anchors.margins: 3
            radius: Adw.buttonRadius - 3
            color: Adw.overlay(stepButton.pressed ? 0.16 : stepButton.hovered ? 0.07 : 0)
        }
        Rectangle {
            anchors.centerIn: parent
            width: 10
            height: 2
            radius: 1
            color: Adw.fg
        }
        Rectangle {
            anchors.centerIn: parent
            visible: stepButton.plus
            width: 2
            height: 10
            radius: 1
            color: Adw.fg
        }
    }

    up.indicator: StepButton {
        x: control.width - width
        plus: true
        hovered: control.up.hovered
        pressed: control.up.pressed
        enabled: control.value < control.to
    }

    down.indicator: StepButton {
        x: control.width - 2 * width
        plus: false
        hovered: control.down.hovered
        pressed: control.down.pressed
        enabled: control.value > control.from
    }

    background: Rectangle {
        implicitWidth: 130
        implicitHeight: Adw.controlHeight
        radius: Adw.buttonRadius
        color: Adw.overlay(0.1)
        border.width: control.activeFocus ? 2 : 0
        border.color: Adw.accentBg
    }
}
