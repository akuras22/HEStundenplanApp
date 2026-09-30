#include "DesktopStyle.h"

#include <QDBusInterface>
#include <QDBusReply>
#include <QDBusVariant>
#include <QGuiApplication>
#include <QPalette>
#include <QQuickStyle>

namespace stundenplan::DesktopStyle {

bool isGnomeSession()
{
    const QString desktop = qEnvironmentVariable("XDG_CURRENT_DESKTOP").toLower();
    const QString session = qEnvironmentVariable("DESKTOP_SESSION").toLower();
    return desktop.contains(QStringLiteral("gnome")) || session.contains(QStringLiteral("gnome"));
}

void selectQuickControlsStyle()
{
    QQuickStyle::setStyle(isGnomeSession() ? QStringLiteral("Fusion") : QStringLiteral("org.kde.desktop"));
}

namespace {

// Adwaita's actual default palette values (light and dark), close to what libadwaita ships —
// Fusion can't reproduce Adwaita's rounded widget shapes, but matching its colors at least keeps
// the app from looking like a stray KDE/Breeze window dropped onto GNOME.
struct AdwaitaColors {
    QColor window, base, text, button, highlight, disabledText;
};

AdwaitaColors lightColors()
{
    return {QColor(0xfa, 0xfa, 0xfa), QColor(0xff, 0xff, 0xff), QColor(0x1e, 0x1e, 0x1e),
            QColor(0xfd, 0xfd, 0xfd), QColor(0x35, 0x84, 0xe4), QColor(0x9a, 0x9a, 0x9a)};
}

AdwaitaColors darkColors()
{
    return {QColor(0x24, 0x24, 0x24), QColor(0x1e, 0x1e, 0x1e), QColor(0xff, 0xff, 0xff),
            QColor(0x33, 0x33, 0x33), QColor(0x35, 0x84, 0xe4), QColor(0x8c, 0x8c, 0x8c)};
}

/** org.freedesktop.appearance/color-scheme: 0 = no preference, 1 = prefer dark, 2 = prefer light. */
bool prefersDarkViaPortal()
{
    QDBusInterface portal(QStringLiteral("org.freedesktop.portal.Desktop"),
                          QStringLiteral("/org/freedesktop/portal/desktop"),
                          QStringLiteral("org.freedesktop.portal.Settings"));
    if (!portal.isValid())
        return false;
    const QDBusReply<QDBusVariant> reply =
        portal.call(QStringLiteral("Read"), QStringLiteral("org.freedesktop.appearance"), QStringLiteral("color-scheme"));
    if (!reply.isValid())
        return false;
    return reply.value().variant().toUInt() == 1;
}

} // namespace

void applyGnomePalette()
{
    if (!isGnomeSession())
        return;

    const AdwaitaColors c = prefersDarkViaPortal() ? darkColors() : lightColors();
    QPalette palette = QGuiApplication::palette();
    palette.setColor(QPalette::Window, c.window);
    palette.setColor(QPalette::Base, c.base);
    palette.setColor(QPalette::AlternateBase, c.window);
    palette.setColor(QPalette::WindowText, c.text);
    palette.setColor(QPalette::Text, c.text);
    palette.setColor(QPalette::Button, c.button);
    palette.setColor(QPalette::ButtonText, c.text);
    palette.setColor(QPalette::Highlight, c.highlight);
    palette.setColor(QPalette::HighlightedText, QColor(0xff, 0xff, 0xff));
    palette.setColor(QPalette::PlaceholderText, c.disabledText);
    palette.setColor(QPalette::Disabled, QPalette::Text, c.disabledText);
    palette.setColor(QPalette::Disabled, QPalette::WindowText, c.disabledText);
    palette.setColor(QPalette::Accent, c.highlight);
    QGuiApplication::setPalette(palette);
}

} // namespace stundenplan::DesktopStyle
