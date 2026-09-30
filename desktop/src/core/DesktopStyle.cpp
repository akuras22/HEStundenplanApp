#include "DesktopStyle.h"

#include <QDBusArgument>
#include <QDBusConnection>
#include <QDBusInterface>
#include <QDBusReply>
#include <QGuiApplication>
#include <QIcon>
#include <QPalette>
#include <QQuickStyle>
#include <QStyleHints>

namespace stundenplan {

namespace {

const QString kPortalService = QStringLiteral("org.freedesktop.portal.Desktop");
const QString kPortalPath = QStringLiteral("/org/freedesktop/portal/desktop");
const QString kPortalSettings = QStringLiteral("org.freedesktop.portal.Settings");
const QString kAppearance = QStringLiteral("org.freedesktop.appearance");
const QString kWmPreferences = QStringLiteral("org.gnome.desktop.wm.preferences");

// libadwaita's default accent (blue) — used until/unless the portal reports the user's own.
const QColor kDefaultAccent(0x35, 0x84, 0xe4);

/**
 * Settings.Read wraps its result in a variant inside the reply's own variant (unlike ReadOne and
 * the SettingChanged signal, which carry just one) — peel off however many layers there are.
 * Not doing so is why dark mode was never detected before: the outer variant converts to 0.
 */
QVariant unwrap(QVariant value)
{
    while (value.userType() == qMetaTypeId<QDBusVariant>())
        value = value.value<QDBusVariant>().variant();
    return value;
}

QVariant readPortalSetting(const QString &group, const QString &key)
{
    QDBusInterface portal(kPortalService, kPortalPath, kPortalSettings);
    if (!portal.isValid())
        return {};
    const QDBusReply<QDBusVariant> reply = portal.call(QStringLiteral("Read"), group, key);
    return reply.isValid() ? unwrap(QVariant::fromValue(reply.value())) : QVariant();
}

} // namespace

bool DesktopStyle::isGnomeLook()
{
    const QString forced = qEnvironmentVariable("HESTUNDENPLAN_LOOK").toLower();
    if (forced == QLatin1String("kde"))
        return false;
    if (forced == QLatin1String("gnome"))
        return true;

    const QStringList desktops = qEnvironmentVariable("XDG_CURRENT_DESKTOP").toLower().split(QLatin1Char(':'));
    const bool plasma = desktops.contains(QStringLiteral("kde")) || qEnvironmentVariableIsSet("KDE_FULL_SESSION");
    return !plasma;
}

void DesktopStyle::selectQuickControlsStyle()
{
    if (isGnomeLook()) {
        // Our own style module (qml/adwaita, compiled into the binary); anything it does not
        // implement itself comes from Qt's plain "Basic" style.
        QQuickStyle::setStyle(QStringLiteral("de.hsesslingen.stundenplan.adwaita"));
        QQuickStyle::setFallbackStyle(QStringLiteral("Basic"));
    } else {
        QQuickStyle::setStyle(QStringLiteral("org.kde.desktop"));
    }
}

DesktopStyle::DesktopStyle(QObject *parent)
    : QObject(parent)
    , m_accentColor(kDefaultAccent)
    , m_buttonsRight{QStringLiteral("close")}
{
    if (!isGnomeLook())
        return;

    // Every icon name the GNOME look uses is an Adwaita "-symbolic" one. Keep the user's icon
    // theme if the platform theme reports one, but make sure Adwaita is there to fall back on —
    // without a GNOME-aware Qt platform theme the default would be the near-empty "hicolor".
    const QString iconTheme = QIcon::themeName();
    if (iconTheme.isEmpty() || iconTheme == QLatin1String("hicolor"))
        QIcon::setThemeName(QStringLiteral("Adwaita"));
    else if (iconTheme != QLatin1String("Adwaita"))
        QIcon::setFallbackThemeName(QStringLiteral("Adwaita"));

    // org.freedesktop.appearance/color-scheme: 0 = no preference, 1 = prefer dark, 2 = prefer
    // light. Without a settings portal, fall back to whatever Qt's platform theme knows.
    const QVariant scheme = readPortalSetting(kAppearance, QStringLiteral("color-scheme"));
    m_darkFromPortal = scheme.isValid();
    m_dark = m_darkFromPortal ? scheme.toUInt() == 1
                              : QGuiApplication::styleHints()->colorScheme() == Qt::ColorScheme::Dark;
    connect(QGuiApplication::styleHints(), &QStyleHints::colorSchemeChanged, this, [this](Qt::ColorScheme scheme) {
        if (!m_darkFromPortal)
            setDark(scheme == Qt::ColorScheme::Dark);
    });

    setAccentColor(readPortalSetting(kAppearance, QStringLiteral("accent-color")));
    setButtonLayout(readPortalSetting(kWmPreferences, QStringLiteral("button-layout")).toString());
    applyAdwaitaPalette();

    QDBusConnection::sessionBus().connect(kPortalService, kPortalPath, kPortalSettings, QStringLiteral("SettingChanged"),
                                          this, SLOT(onPortalSettingChanged(QString, QString, QDBusVariant)));
}

bool DesktopStyle::clientSideDecorations() const
{
    return isGnomeLook() && qEnvironmentVariable("HESTUNDENPLAN_CSD") != QLatin1String("0");
}

void DesktopStyle::onPortalSettingChanged(const QString &group, const QString &key, const QDBusVariant &value)
{
    const QVariant v = unwrap(QVariant::fromValue(value));
    if (group == kAppearance && key == QLatin1String("color-scheme")) {
        m_darkFromPortal = true;
        setDark(v.toUInt() == 1);
    } else if (group == kAppearance && key == QLatin1String("accent-color")) {
        setAccentColor(v);
    } else if (group == kWmPreferences && key == QLatin1String("button-layout")) {
        setButtonLayout(v.toString());
    }
}

void DesktopStyle::setDark(bool dark)
{
    if (m_dark == dark)
        return;
    m_dark = dark;
    applyAdwaitaPalette();
    Q_EMIT darkChanged();
}

void DesktopStyle::setAccentColor(const QVariant &portalValue)
{
    // (ddd) sRGB in [0, 1]; the spec says out-of-range values mean "no accent color set".
    QColor accent = kDefaultAccent;
    if (portalValue.userType() == qMetaTypeId<QDBusArgument>()) {
        const auto arg = portalValue.value<QDBusArgument>();
        double r = -1, g = -1, b = -1;
        arg.beginStructure();
        arg >> r >> g >> b;
        arg.endStructure();
        const auto inRange = [](double c) { return c >= 0.0 && c <= 1.0; };
        if (inRange(r) && inRange(g) && inRange(b))
            accent = QColor::fromRgbF(float(r), float(g), float(b));
    }
    if (accent == m_accentColor)
        return;
    m_accentColor = accent;
    applyAdwaitaPalette();
    Q_EMIT accentColorChanged();
}

void DesktopStyle::setButtonLayout(const QString &layout)
{
    // e.g. "appmenu:minimize,maximize,close" — buttons left of the colon go on the left side of
    // the header bar. Anything but the three real window buttons (appmenu, icon, ...) is ignored.
    const auto buttonsOf = [](const QString &side) {
        QStringList buttons;
        const QStringList tokens = side.split(QLatin1Char(','), Qt::SkipEmptyParts);
        for (const QString &token : tokens) {
            if (token == QLatin1String("minimize") || token == QLatin1String("maximize") || token == QLatin1String("close"))
                buttons << token;
        }
        return buttons;
    };
    QStringList left;
    QStringList right{QStringLiteral("close")};
    if (!layout.isEmpty()) {
        left = buttonsOf(layout.section(QLatin1Char(':'), 0, 0));
        right = buttonsOf(layout.section(QLatin1Char(':'), 1));
    }
    if (left == m_buttonsLeft && right == m_buttonsRight)
        return;
    m_buttonsLeft = left;
    m_buttonsRight = right;
    Q_EMIT windowButtonsChanged();
}

void DesktopStyle::applyAdwaitaPalette()
{
    // libadwaita's own default colors. Kirigami derives its whole Theme from the application
    // palette when no KDE platform plugin is active (see Kirigami's styles/org.kde.desktop/
    // Theme.qml), so this is what colors every Kirigami.Theme.* lookup in the GNOME look.
    const QColor window = m_dark ? QColor(0x22, 0x22, 0x26) : QColor(0xfa, 0xfa, 0xfb);
    const QColor view = m_dark ? QColor(0x1d, 0x1d, 0x20) : QColor(0xff, 0xff, 0xff);
    const QColor text = m_dark ? QColor(0xff, 0xff, 0xff) : QColor(0x33, 0x33, 0x38);
    const QColor button = m_dark ? QColor(0x38, 0x38, 0x3c) : QColor(0xe6, 0xe6, 0xe7);
    const QColor popover = m_dark ? QColor(0x36, 0x36, 0x3a) : QColor(0xff, 0xff, 0xff);
    const QColor link = m_dark ? QColor(0x81, 0xd0, 0xff) : QColor(0x04, 0x61, 0xbe);
    QColor dimText = text;
    dimText.setAlphaF(0.5f);

    // Set every role explicitly (in all color groups): a role left unset would keep resolving
    // against the Qt platform theme's palette, which is how a dark GTK theme used to bleed white
    // text into an otherwise light window.
    QPalette palette;
    const auto set = [&palette](QPalette::ColorRole role, const QColor &color) {
        palette.setColor(QPalette::Active, role, color);
        palette.setColor(QPalette::Inactive, role, color);
        palette.setColor(QPalette::Disabled, role, color);
    };
    set(QPalette::Window, window);
    set(QPalette::WindowText, text);
    set(QPalette::Base, view);
    set(QPalette::AlternateBase, window);
    set(QPalette::Text, text);
    set(QPalette::Button, button);
    set(QPalette::ButtonText, text);
    set(QPalette::BrightText, QColor(0xff, 0xff, 0xff));
    set(QPalette::ToolTipBase, popover);
    set(QPalette::ToolTipText, text);
    set(QPalette::Highlight, m_accentColor);
    set(QPalette::HighlightedText, QColor(0xff, 0xff, 0xff));
    set(QPalette::Accent, m_accentColor);
    set(QPalette::Link, link);
    set(QPalette::LinkVisited, link);
    set(QPalette::PlaceholderText, dimText);
    set(QPalette::Light, button.lighter(120));
    set(QPalette::Midlight, button.lighter(110));
    set(QPalette::Mid, button.darker(120));
    set(QPalette::Dark, button.darker(150));
    set(QPalette::Shadow, QColor(0x00, 0x00, 0x00));
    for (const auto role : {QPalette::WindowText, QPalette::Text, QPalette::ButtonText})
        palette.setColor(QPalette::Disabled, role, dimText);
    QGuiApplication::setPalette(palette);
}

} // namespace stundenplan
