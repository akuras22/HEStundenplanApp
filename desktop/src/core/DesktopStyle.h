#pragma once

#include <QColor>
#include <QDBusVariant>
#include <QObject>
#include <QStringList>

namespace stundenplan {

/**
 * Decides which of the app's two looks is used and feeds the GNOME one with live system settings.
 *
 * - KDE look: only under a Plasma session. The org.kde.desktop QQC2 style (Breeze-shaped widgets,
 *   Plasma's own palette and accent), Kirigami's toolbar, server-side window decoration.
 * - GNOME look: everywhere else, so it is the default. Our own Adwaita-shaped QQC2 style
 *   (qml/adwaita), an Adwaita QPalette, and a client-side header bar with window buttons like a
 *   libadwaita app — there is no GTK in here, it is all drawn in QML.
 *
 * Exposed to QML as the DesktopStyle singleton (module de.hsesslingen.stundenplan.platform).
 * dark/accentColor/windowButtons* are only meaningful for the GNOME look; under KDE the QML side
 * reads everything from Kirigami.Theme instead.
 */
class DesktopStyle : public QObject
{
    Q_OBJECT
    Q_PROPERTY(bool gnome READ isGnomeLook CONSTANT)
    Q_PROPERTY(bool clientSideDecorations READ clientSideDecorations CONSTANT)
    Q_PROPERTY(bool dark READ isDark NOTIFY darkChanged)
    Q_PROPERTY(QColor accentColor READ accentColor NOTIFY accentColorChanged)
    Q_PROPERTY(QStringList windowButtonsLeft READ windowButtonsLeft NOTIFY windowButtonsChanged)
    Q_PROPERTY(QStringList windowButtonsRight READ windowButtonsRight NOTIFY windowButtonsChanged)

public:
    /**
     * True unless running under KDE Plasma (XDG_CURRENT_DESKTOP/KDE_FULL_SESSION), so GNOME,
     * and any desktop we do not know, get the GNOME look. HESTUNDENPLAN_LOOK=kde|gnome overrides
     * the detection, mainly for testing one look from inside the other desktop.
     */
    static bool isGnomeLook();

    /** Call before constructing QGuiApplication (QQuickStyle::setStyle's documented requirement). */
    static void selectQuickControlsStyle();

    /** Construct after QGuiApplication (needs the D-Bus session bus) and before any window. */
    explicit DesktopStyle(QObject *parent = nullptr);

    /** False with HESTUNDENPLAN_CSD=0, which keeps the window manager's own title bar. */
    bool clientSideDecorations() const;
    bool isDark() const { return m_dark; }
    QColor accentColor() const { return m_accentColor; }
    /** Window buttons ("minimize", "maximize", "close") per GNOME's button-layout setting. */
    QStringList windowButtonsLeft() const { return m_buttonsLeft; }
    QStringList windowButtonsRight() const { return m_buttonsRight; }

Q_SIGNALS:
    void darkChanged();
    void accentColorChanged();
    void windowButtonsChanged();

private Q_SLOTS:
    void onPortalSettingChanged(const QString &group, const QString &key, const QDBusVariant &value);

private:
    void setDark(bool dark);
    void setAccentColor(const QVariant &portalValue);
    void setButtonLayout(const QString &layout);
    void applyAdwaitaPalette();

    bool m_dark = false;
    bool m_darkFromPortal = false;
    QColor m_accentColor;
    QStringList m_buttonsLeft;
    QStringList m_buttonsRight;
};

} // namespace stundenplan
