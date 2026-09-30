import QtQuick
import QtQuick.Layouts
import QtQuick.Controls as Controls
import org.kde.kirigami as Kirigami
import de.hsesslingen.stundenplan.platform
import de.hsesslingen.stundenplan.adwaita as Adwaita

// GNOME look only: a libadwaita-style header bar that replaces both Kirigami's page toolbar and
// the window manager's title bar. Back button and window buttons at the edges, the current page's
// title centered, its actions as flat buttons on the right; dragging it moves the window.
// Pages feed it through the same title/actions properties Kirigami's own toolbar uses under KDE,
// plus an optional headerBarTitle component for a custom title widget.
Item {
    id: root

    required property Kirigami.ApplicationWindow window

    readonly property Item page: window.pageStack.currentItem
    readonly property bool windowButtonsVisible: DesktopStyle.clientSideDecorations
                                                 && window.visibility !== Window.FullScreen

    function toggleMaximized() {
        if (window.visibility === Window.Maximized)
            window.showNormal()
        else
            window.showMaximized()
    }

    implicitHeight: Adwaita.Adw.headerBarHeight

    Rectangle {
        anchors.fill: parent
        color: Adwaita.Adw.windowBg
    }

    // Dragging the bar moves the window, double-clicking it toggles maximized. A MouseArea below
    // the buttons (not pointer handlers): it only ever sees presses no button took, whereas a
    // DragHandler here also gets a say in presses on the buttons and eats their clicks.
    MouseArea {
        property point pressPos
        anchors.fill: parent
        enabled: DesktopStyle.clientSideDecorations
        acceptedButtons: Qt.LeftButton
        onPressed: (mouse) => pressPos = Qt.point(mouse.x, mouse.y)
        onPositionChanged: (mouse) => {
            const distance = Math.abs(mouse.x - pressPos.x) + Math.abs(mouse.y - pressPos.y)
            if (pressed && distance >= Application.styleHints.startDragDistance)
                root.window.startSystemMove()
        }
        onDoubleClicked: root.toggleMaximized()
    }

    component WindowButtons: Row {
        id: buttons
        required property list<string> model
        spacing: 3
        visible: root.windowButtonsVisible && model.length > 0

        Repeater {
            model: buttons.model
            delegate: Controls.AbstractButton {
                id: windowButton
                required property string modelData
                implicitWidth: Adwaita.Adw.controlHeight
                implicitHeight: Adwaita.Adw.controlHeight
                focusPolicy: Qt.NoFocus
                onClicked: {
                    if (modelData === "close")
                        root.window.close()
                    else if (modelData === "minimize")
                        root.window.showMinimized()
                    else
                        root.toggleMaximized()
                }
                background: Item {
                    Rectangle {
                        anchors.centerIn: parent
                        width: 24
                        height: 24
                        radius: 12
                        color: Adwaita.Adw.overlay(windowButton.down ? 0.3 : windowButton.hovered ? 0.15 : 0.1)
                    }
                }
                contentItem: Item {
                    Kirigami.Icon {
                        anchors.centerIn: parent
                        width: 16
                        height: 16
                        color: Adwaita.Adw.fg
                        isMask: true
                        source: windowButton.modelData === "close" ? "window-close-symbolic"
                              : windowButton.modelData === "minimize" ? "window-minimize-symbolic"
                              : root.window.visibility === Window.Maximized ? "window-restore-symbolic"
                              : "window-maximize-symbolic"
                    }
                }
            }
        }
    }

    // Like GTK, fade the bar's content while the window is not the active one.
    readonly property real contentOpacity: window.active ? 1 : 0.5

    RowLayout {
        id: leading
        opacity: root.contentOpacity
        anchors.left: parent.left
        anchors.leftMargin: 6
        anchors.verticalCenter: parent.verticalCenter
        spacing: 6

        WindowButtons {
            model: DesktopStyle.windowButtonsLeft
        }
        Controls.ToolButton {
            visible: root.window.pageStack.currentIndex > 0
            icon.name: "go-previous-symbolic"
            onClicked: root.window.pageStack.pop()
            Controls.ToolTip.text: qsTr("Zurück")
            Controls.ToolTip.visible: hovered
        }
    }

    RowLayout {
        id: trailing
        opacity: root.contentOpacity
        anchors.right: parent.right
        anchors.rightMargin: 6
        anchors.verticalCenter: parent.verticalCenter
        spacing: 6

        Repeater {
            model: root.page?.actions ?? []
            delegate: Controls.ToolButton {
                id: actionButton
                required property Kirigami.Action modelData
                readonly property bool hasIcon: modelData.icon.name !== ""
                visible: modelData.visible
                enabled: modelData.enabled
                text: modelData.text
                icon.name: modelData.icon.name
                display: hasIcon ? Controls.AbstractButton.IconOnly : Controls.AbstractButton.TextOnly
                onClicked: modelData.trigger()
                Controls.ToolTip.text: modelData.text
                Controls.ToolTip.visible: hovered && hasIcon
            }
        }
        WindowButtons {
            model: DesktopStyle.windowButtonsRight
        }
    }

    // Centered on the whole bar like GTK does; when the two sides leave no room for that (narrow
    // window), centered in whatever space is left between them instead.
    Loader {
        id: titleLoader
        readonly property real leftEdge: leading.x + leading.width + 12
        readonly property real rightEdge: trailing.x - 12
        readonly property real centeredX: Math.round((root.width - implicitWidth) / 2)
        readonly property bool fitsCentered: centeredX >= leftEdge && centeredX + implicitWidth <= rightEdge

        opacity: root.contentOpacity
        anchors.verticalCenter: parent.verticalCenter
        width: Math.min(implicitWidth, Math.max(0, rightEdge - leftEdge))
        x: fitsCentered ? centeredX : Math.round(leftEdge + (rightEdge - leftEdge - width) / 2)
        sourceComponent: root.page?.headerBarTitle ?? defaultTitle
    }

    Component {
        id: defaultTitle
        Text {
            text: root.page?.title ?? ""
            color: Adwaita.Adw.fg
            font.bold: true
            elide: Text.ElideRight
            horizontalAlignment: Text.AlignHCenter
        }
    }
}
