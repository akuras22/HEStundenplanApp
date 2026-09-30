import QtQuick
import QtQuick.Templates as T
import org.kde.kirigami.primitives as Primitives

// GTK popover menu: rounded, shadowed surface with padded, rounded items.
T.Menu {
    id: control

    implicitWidth: Math.max(implicitBackgroundWidth + leftInset + rightInset,
                            contentWidth + leftPadding + rightPadding)
    implicitHeight: Math.max(implicitBackgroundHeight + topInset + bottomInset,
                             contentHeight + topPadding + bottomPadding)

    margins: 6
    padding: 6
    overlap: 1

    delegate: MenuItem {}

    contentItem: ListView {
        implicitHeight: contentHeight
        model: control.contentModel
        interactive: Window.window ? contentHeight + control.topPadding + control.bottomPadding > control.height : false
        clip: true
        currentIndex: control.currentIndex
    }

    enter: Transition {
        NumberAnimation { property: "opacity"; from: 0; to: 1; duration: 100 }
    }
    exit: Transition {
        NumberAnimation { property: "opacity"; from: 1; to: 0; duration: 100 }
    }

    background: Primitives.ShadowedRectangle {
        implicitWidth: 180
        implicitHeight: Adw.controlHeight
        radius: Adw.popoverRadius
        color: Adw.popoverBg
        border.width: 1
        border.color: Adw.overlay(Adw.dark ? 0.12 : 0.1)
        shadow.size: 18
        shadow.yOffset: 3
        shadow.color: Qt.rgba(0, 0, 0, Adw.dark ? 0.45 : 0.22)
    }
}
