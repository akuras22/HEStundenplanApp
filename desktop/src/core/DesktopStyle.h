#pragma once

namespace stundenplan::DesktopStyle {

/** True when running under a GNOME session (XDG_CURRENT_DESKTOP/DESKTOP_SESSION). */
bool isGnomeSession();

/**
 * Call before constructing QGuiApplication (QQuickStyle::setStyle's documented requirement).
 * org.kde.desktop (Breeze-shaped widgets) everywhere except GNOME, where there is no equivalent
 * "native GTK-shaped" QQC2 style in the Qt/KDE ecosystem — Fusion is used instead as the closest
 * flat, non-KDE-looking built-in style, recolored by applyGnomePalette() below.
 */
void selectQuickControlsStyle();

/**
 * Call after QGuiApplication is constructed (needs the D-Bus session bus) and before creating any
 * window. No-op outside GNOME — under GNOME, builds a QPalette approximating Adwaita's own colors
 * (querying the XDG desktop portal for the user's light/dark preference) and applies it globally,
 * so Fusion-rendered controls (Button, CheckBox, ComboBox, ...) pick up GNOME-appropriate colors
 * instead of Qt's generic default gray. This does not reproduce Adwaita's actual widget shapes
 * (rounded buttons, switch style, ...) — that would need a full GTK4/libadwaita rewrite — but it
 * stops the app looking like a stray KDE window color-wise.
 */
void applyGnomePalette();

} // namespace stundenplan::DesktopStyle
