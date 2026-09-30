pragma Singleton
import QtQuick
import de.hsesslingen.stundenplan.platform

// libadwaita's design tokens (colors, radii, sizes), shared by every control of this style and by
// the app's own GNOME-look pieces (see components/AppTheme.qml). Values are libadwaita's defaults;
// light/dark and the accent follow the desktop live via DesktopStyle.
QtObject {
    readonly property bool dark: DesktopStyle.dark

    readonly property color accentBg: DesktopStyle.accentColor
    readonly property color accentFg: "#ffffff"
    // Accent as a *text* color needs more contrast against the window than the fill color has.
    readonly property color accentText: dark ? Qt.lighter(accentBg, 1.5) : Qt.darker(accentBg, 1.35)

    readonly property color fg: dark ? "#ffffff" : "#333338"
    readonly property color dimFg: overlay(0.55)
    readonly property color windowBg: dark ? "#222226" : "#fafafb"
    readonly property color viewBg: dark ? "#1d1d20" : "#ffffff"
    readonly property color cardBg: dark ? Qt.rgba(1, 1, 1, 0.08) : "#ffffff"
    // libadwaita gives light cards a faint shadow instead of a border; a hairline reads the same.
    readonly property color cardBorder: dark ? "transparent" : Qt.rgba(0, 0, 0.02, 0.1)
    readonly property color popoverBg: dark ? "#36363a" : "#ffffff"
    readonly property color dialogBg: dark ? "#36363a" : "#fafafb"
    readonly property color borderColor: overlay(0.15)
    readonly property color windowBorder: dark ? Qt.rgba(1, 1, 1, 0.14) : Qt.rgba(0, 0, 0.02, 0.22)
    readonly property color shade: Qt.rgba(0, 0, 0.02, dark ? 0.36 : 0.12)

    readonly property color destructiveBg: dark ? "#c01c28" : "#e01b24"
    readonly property color errorText: dark ? "#ff938c" : "#c30000"
    readonly property color successText: dark ? "#78e9ab" : "#007c3d"
    readonly property color warningText: dark ? "#ffc252" : "#905400"

    readonly property int buttonRadius: 9
    readonly property int cardRadius: 12
    readonly property int popoverRadius: 12
    readonly property int dialogRadius: 15
    readonly property int windowRadius: 15
    readonly property int controlHeight: 34
    readonly property int headerBarHeight: 47
    readonly property int rowHeight: 50

    /** libadwaita's alpha(currentColor, a): the text color at the given opacity. */
    function overlay(alpha: real): color {
        return Qt.rgba(fg.r, fg.g, fg.b, alpha)
    }
}
