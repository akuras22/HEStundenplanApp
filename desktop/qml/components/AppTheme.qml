pragma Singleton
import QtQuick
import org.kde.kirigami as Kirigami

// Colors for the components we paint ourselves (the Woche/Tag switcher, the "today" date chip,
// the grid) — all just mirror the live system Plasma theme/accent. There used to be app-level
// Hell/System/Dunkel and accent-color overrides here, but Kirigami's own chrome (Page background,
// headers, dialogs, native controls) always follows the system theme and accent regardless of
// what an app requests, so a different choice for our own custom-drawn bits only ever produced a
// mismatched, half-themed look.
QtObject {
    readonly property color accentColor: Kirigami.Theme.highlightColor
    readonly property color backgroundColor: Kirigami.Theme.backgroundColor
    readonly property color surfaceColor: Kirigami.Theme.backgroundColor
    readonly property color textColor: Kirigami.Theme.textColor
    readonly property color disabledTextColor: Kirigami.Theme.disabledTextColor
}
