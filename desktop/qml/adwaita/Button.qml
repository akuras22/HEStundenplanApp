import QtQuick
import QtQuick.Templates as T

T.Button {
    id: control

    readonly property bool iconOnly: text === "" || (display === T.AbstractButton.IconOnly && icon.name !== "")

    implicitWidth: Math.max(implicitBackgroundWidth + leftInset + rightInset,
                            implicitContentWidth + leftPadding + rightPadding)
    implicitHeight: Math.max(implicitBackgroundHeight + topInset + bottomInset,
                             implicitContentHeight + topPadding + bottomPadding)

    horizontalPadding: iconOnly ? 9 : 17
    verticalPadding: 5
    spacing: 6
    font.bold: true
    icon.width: 16
    icon.height: 16
    opacity: enabled ? 1 : 0.5

    contentItem: ButtonContent {
        control: control
        color: control.highlighted ? Adw.accentFg : Adw.fg
    }

    background: Rectangle {
        implicitWidth: Adw.controlHeight
        implicitHeight: Adw.controlHeight
        radius: Adw.buttonRadius
        // highlighted = libadwaita's "suggested-action", flat = its "flat" style class.
        color: {
            if (control.highlighted)
                return control.down ? Qt.darker(Adw.accentBg, 1.15) : control.hovered ? Qt.lighter(Adw.accentBg, 1.1) : Adw.accentBg
            if (control.flat)
                return Adw.overlay(control.down || control.checked ? 0.16 : control.hovered ? 0.07 : 0)
            return Adw.overlay(control.down || control.checked ? 0.3 : control.hovered ? 0.15 : 0.1)
        }
        Behavior on color { ColorAnimation { duration: 100 } }

        FocusRing { visible: control.visualFocus }
    }
}
