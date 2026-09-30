import QtQuick
import QtQuick.Templates as T

T.TextField {
    id: control

    implicitWidth: implicitBackgroundWidth + leftInset + rightInset
                   || Math.max(contentWidth, placeholder.implicitWidth) + leftPadding + rightPadding
    implicitHeight: Math.max(implicitBackgroundHeight + topInset + bottomInset,
                             contentHeight + topPadding + bottomPadding,
                             placeholder.implicitHeight + topPadding + bottomPadding)

    leftPadding: 9
    rightPadding: 9
    topPadding: 6
    bottomPadding: 6

    color: Adw.fg
    selectionColor: Qt.alpha(Adw.accentBg, 0.35)
    selectedTextColor: color
    placeholderTextColor: Adw.overlay(0.5)
    verticalAlignment: TextInput.AlignVCenter
    opacity: enabled ? 1 : 0.5

    Text {
        id: placeholder
        x: control.leftPadding
        y: control.topPadding
        width: control.width - (control.leftPadding + control.rightPadding)
        height: control.height - (control.topPadding + control.bottomPadding)
        text: control.placeholderText
        font: control.font
        color: control.placeholderTextColor
        verticalAlignment: control.verticalAlignment
        horizontalAlignment: control.horizontalAlignment
        visible: !control.length && !control.preeditText
        elide: Text.ElideRight
        renderType: control.renderType
    }

    background: Rectangle {
        implicitWidth: 200
        implicitHeight: Adw.controlHeight
        radius: Adw.buttonRadius
        color: Adw.overlay(control.hovered && !control.activeFocus ? 0.12 : 0.1)
        // libadwaita draws the focus outline *inside* entries.
        border.width: control.activeFocus ? 2 : 0
        border.color: Adw.accentBg
    }
}
