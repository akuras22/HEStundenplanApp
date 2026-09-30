pragma Singleton
import QtQuick
import org.kde.kirigami as Kirigami
import de.hsesslingen.stundenplan.platform
import de.hsesslingen.stundenplan.adwaita as Adwaita

// Design tokens for everything we paint ourselves (the grid, cards, the Woche/Tag switcher, ...),
// resolved for whichever of the two looks is active (see DesktopStyle.h):
// - KDE look: mirrors the live Plasma theme/accent through Kirigami.Theme, Breeze-sized shapes
//   and Breeze icon names.
// - GNOME look: libadwaita's colors, radii and sizes (adwaita/Adw.qml) and Adwaita's symbolic icons.
QtObject {
    id: root

    readonly property bool gnome: DesktopStyle.gnome

    readonly property color accentColor: Kirigami.Theme.highlightColor
    readonly property color backgroundColor: Kirigami.Theme.backgroundColor
    readonly property color surfaceColor: Kirigami.Theme.backgroundColor
    readonly property color textColor: Kirigami.Theme.textColor
    readonly property color disabledTextColor: gnome ? Adwaita.Adw.dimFg : Kirigami.Theme.disabledTextColor
    readonly property bool dark: gnome ? Adwaita.Adw.dark : backgroundColor.hslLightness < 0.5

    // Cards ("boxed lists" in GNOME terms) group rows on the settings pages.
    readonly property color cardColor: gnome ? Adwaita.Adw.cardBg : viewColors.Kirigami.Theme.backgroundColor
    readonly property color cardBorderColor: gnome ? Adwaita.Adw.cardBorder : separatorColor
    readonly property color separatorColor: gnome ? Adwaita.Adw.overlay(0.1)
                                                  : Qt.rgba(textColor.r, textColor.g, textColor.b, 0.2)
    readonly property int cardRadius: gnome ? Adwaita.Adw.cardRadius : Kirigami.Units.cornerRadius
    readonly property int controlRadius: gnome ? Adwaita.Adw.buttonRadius : Kirigami.Units.cornerRadius
    readonly property int rowHeight: gnome ? Adwaita.Adw.rowHeight : Kirigami.Units.gridUnit * 2.5
    readonly property int pageMaxWidth: gnome ? 640 : Kirigami.Units.gridUnit * 36
    readonly property int pageMargin: gnome ? 12 : Kirigami.Units.largeSpacing

    readonly property color positiveColor: gnome ? Adwaita.Adw.successText : Kirigami.Theme.positiveTextColor
    readonly property color warningColor: gnome ? Adwaita.Adw.warningText : Kirigami.Theme.neutralTextColor
    readonly property color errorColor: gnome ? Adwaita.Adw.errorText : Kirigami.Theme.negativeTextColor

    // The View color set (list/card background) — Kirigami.Theme on this object is the Window set.
    readonly property QtObject viewColors: QtObject {
        Kirigami.Theme.inherit: false
        Kirigami.Theme.colorSet: Kirigami.Theme.View
    }

    // Breeze icon name -> Adwaita symbolic equivalent. "" means GNOME shows no icon there
    // (e.g. a text button instead), which callers handle by falling back to the label.
    readonly property var gnomeIcons: ({
        "search": "system-search-symbolic",
        "go-jump-today": "",
        "view-refresh": "view-refresh-symbolic",
        "configure": "applications-system-symbolic",
        "go-previous": "go-previous-symbolic",
        "go-next": "go-next-symbolic",
        "arrow-down": "pan-down-symbolic",
        "view-calendar-week": "",
        "view-calendar-day": "",
        "view-calendar-list": "view-list-bullet-symbolic",
        "notifications": "preferences-system-notifications-symbolic",
        "preferences-desktop-theme": "applications-graphics-symbolic",
        "help-about": "help-about-symbolic",
        "system-software-update": "software-update-available-symbolic",
        "documentinfo": "text-x-generic-symbolic",
        "edit-clear-all": "user-trash-symbolic",
        "internet-web-browser": "web-browser-symbolic",
        "edit-copy": "edit-copy-symbolic",
        "edit-undo": "edit-undo-symbolic",
        "list-add": "list-add-symbolic",
    })

    function icon(name: string): string {
        if (!gnome)
            return name
        const mapped = gnomeIcons[name]
        return mapped === undefined ? name : mapped
    }
}
