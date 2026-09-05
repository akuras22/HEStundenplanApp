pragma Singleton
import QtQuick
import org.kde.kirigami as Kirigami

// Resolves the user's Darstellung choices (accent preset/custom color, forced theme mode) into
// concrete colors. Kirigami's own chrome (Page background, headers, dialogs) always follows the
// system Plasma color scheme by design and cannot be overridden per-app — see the notes in
// PlanPage.qml — so this singleton only drives colors for components we paint ourselves (the
// Woche/Tag switcher, the "today" date chip, the grid).
QtObject {
    readonly property var _presetColors: ["", "#2E9E5B", "#8B5CF6", "#E0762F", "#E0473D", "#E0508F", ""]
    readonly property color defaultAccent: "#0381FE"

    readonly property int accentPreset: settingsStore ? settingsStore.accentPreset : 0
    readonly property color customAccentColor: settingsStore ? settingsStore.customAccentColor : "transparent"

    readonly property color accentColor: {
        if (accentPreset === 6)
            return customAccentColor.a > 0 ? customAccentColor : defaultAccent
        if (accentPreset === 0)
            return defaultAccent
        return _presetColors[accentPreset]
    }

    readonly property int themeMode: settingsStore ? settingsStore.themeMode : 0
    readonly property bool forcedLight: themeMode === 1
    readonly property bool forcedDark: themeMode === 2

    readonly property color backgroundColor: forcedLight ? "#f5f5f5" : (forcedDark ? "#1b1b1f" : Kirigami.Theme.backgroundColor)
    readonly property color surfaceColor: forcedLight ? "#ffffff" : (forcedDark ? "#26262b" : Kirigami.Theme.backgroundColor)
    readonly property color textColor: forcedLight ? "#1b1b1f" : (forcedDark ? "#f2f2f2" : Kirigami.Theme.textColor)
    readonly property color disabledTextColor: forcedLight ? "#6e6e6e" : (forcedDark ? "#9a9a9a" : Kirigami.Theme.disabledTextColor)
}
