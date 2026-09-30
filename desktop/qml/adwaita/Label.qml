import QtQuick
import QtQuick.Templates as T
import org.kde.kirigami.platform as Platform

// Follows Kirigami's color-set context (rather than Adw.fg) so Kirigami's own components, which
// put labels on selection/header backgrounds, stay readable.
T.Label {
    color: Platform.Theme.textColor
    linkColor: Adw.accentText
}
