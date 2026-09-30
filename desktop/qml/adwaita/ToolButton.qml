import QtQuick
import QtQuick.Templates as T

// libadwaita's flat header-bar button: no background until hovered.
T.ToolButton {
    id: control

    readonly property bool iconOnly: text === "" || (display === T.AbstractButton.IconOnly && icon.name !== "")

    implicitWidth: Math.max(implicitBackgroundWidth + leftInset + rightInset,
                            implicitContentWidth + leftPadding + rightPadding)
    implicitHeight: Math.max(implicitBackgroundHeight + topInset + bottomInset,
                             implicitContentHeight + topPadding + bottomPadding)

    horizontalPadding: iconOnly ? 9 : 10
    verticalPadding: 5
    spacing: 6
    font.bold: true
    icon.width: 16
    icon.height: 16
    opacity: enabled ? 1 : 0.5

    contentItem: ButtonContent {
        control: control
    }

    background: Rectangle {
        implicitWidth: Adw.controlHeight
        implicitHeight: Adw.controlHeight
        radius: Adw.buttonRadius
        color: Adw.overlay(control.down || control.checked ? 0.16 : control.hovered ? 0.07 : 0)
        Behavior on color { ColorAnimation { duration: 100 } }

        FocusRing { visible: control.visualFocus }
    }
}
