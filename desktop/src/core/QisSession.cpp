#include "QisSession.h"

#include <QDir>
#include <QFile>
#include <QJsonDocument>
#include <QJsonObject>
#include <QNetworkCookie>
#include <QQuickWebEngineProfile>
#include <QStandardPaths>
#include <QWebEngineCookieStore>

namespace stundenplan {

namespace {
const char *kQisHost = "www3.hs-esslingen.de";
// The Shibboleth session of www3.hs-esslingen.de ends after 8 hours at the latest (it can end
// earlier after a long idle stretch, which QisRepository notices on its own).
constexpr qint64 kMaxLifetimeSecs = 8 * 60 * 60;
// The HS Esslingen SSO session (Keycloak) lasts at most 10 hours from the login — measured in a
// browser, with no "stay signed in" to extend it. Within this window a new QIS session cookie can
// be minted silently; after it, a full login is needed again.
constexpr qint64 kSsoLifetimeSecs = 10 * 60 * 60;
} // namespace

QisSession::QisSession(QObject *parent)
    : QObject(parent)
{
    const QString dataDir = QStandardPaths::writableLocation(QStandardPaths::AppDataLocation);
    QDir().mkpath(dataDir);
    m_filePath = dataDir + QStringLiteral("/qis-session.json");
    load();
}

QDateTime QisSession::expiresAt() const
{
    return m_loggedInAt.isValid() ? m_loggedInAt.addSecs(kMaxLifetimeSecs) : QDateTime();
}

QDateTime QisSession::ssoExpiresAt() const
{
    return m_ssoLoggedInAt.isValid() ? m_ssoLoggedInAt.addSecs(kSsoLifetimeSecs) : QDateTime();
}

bool QisSession::canSilentlyRefresh() const
{
    return m_ssoLoggedInAt.isValid() && QDateTime::currentDateTime() < ssoExpiresAt();
}

QByteArray QisSession::activeCookieHeader()
{
    if (m_cookie.isEmpty() || m_expired)
        return {};
    if (QDateTime::currentDateTime() >= expiresAt()) {
        markExpired();
        return {};
    }
    return m_cookie;
}

void QisSession::markExpired()
{
    if (m_cookie.isEmpty() || m_expired)
        return;
    m_expired = true;
    save();
    Q_EMIT changed();
    // Renew it silently from the still-valid SSO session rather than making the user log in again.
    startSilentRefreshIfPossible();
}

void QisSession::setSilentRefreshActive(bool active)
{
    if (m_silentRefreshActive == active)
        return;
    m_silentRefreshActive = active;
    Q_EMIT silentRefreshActiveChanged();
}

void QisSession::startSilentRefreshIfPossible()
{
    const bool needsRefresh = !m_cookie.isEmpty() && (m_expired || QDateTime::currentDateTime() >= expiresAt());
    if (needsRefresh && !m_silentRefreshExhausted && !m_silentRefreshActive && canSilentlyRefresh())
        setSilentRefreshActive(true);
}

void QisSession::failSilentRefresh()
{
    m_silentRefreshExhausted = true;
    setSilentRefreshActive(false);
    markExpired();
}

void QisSession::setLoginRequired(bool required)
{
    if (m_loginRequired == required)
        return;
    m_loginRequired = required;
    Q_EMIT loginRequiredChanged();
}

void QisSession::attachProfile(QObject *webEngineProfile)
{
    auto *profile = qobject_cast<QQuickWebEngineProfile *>(webEngineProfile);
    if (!profile)
        return;
    m_collectedCookies.clear();
    connect(profile->cookieStore(), &QWebEngineCookieStore::cookieAdded, this, [this](const QNetworkCookie &cookie) {
        const QString domain = cookie.domain();
        const bool forQis = domain.isEmpty() || domain == QLatin1String(kQisHost) || domain == QLatin1Char('.') + QLatin1String(kQisHost);
        if (forQis && cookie.name().startsWith("_shibsession_"))
            m_collectedCookies.insert(cookie.name(), cookie.value());
    });
}

bool QisSession::completeLogin(bool fullLogin)
{
    if (m_collectedCookies.isEmpty())
        return false;
    QList<QByteArray> pairs;
    for (auto it = m_collectedCookies.cbegin(); it != m_collectedCookies.cend(); ++it)
        pairs.append(it.key() + '=' + it.value());
    m_cookie = pairs.join("; ");
    const QDateTime now = QDateTime::currentDateTime();
    m_loggedInAt = now;
    // A full login (re)starts the SSO session clock; a silent refresh keeps the original one,
    // since reusing the SSO session doesn't extend its maximum lifetime.
    if (fullLogin || !m_ssoLoggedInAt.isValid())
        m_ssoLoggedInAt = now;
    m_expired = false;
    m_silentRefreshExhausted = false;
    save();
    Q_EMIT changed();
    setSilentRefreshActive(false);
    setLoginRequired(false);
    return true;
}

void QisSession::logout()
{
    m_cookie.clear();
    m_loggedInAt = QDateTime();
    m_ssoLoggedInAt = QDateTime();
    m_expired = false;
    m_silentRefreshExhausted = false;
    setSilentRefreshActive(false);
    QFile::remove(m_filePath);
    Q_EMIT changed();
}

void QisSession::load()
{
    QFile file(m_filePath);
    if (!file.open(QIODevice::ReadOnly))
        return;
    const QJsonObject obj = QJsonDocument::fromJson(file.readAll()).object();
    m_cookie = obj.value(QStringLiteral("cookie")).toString().toUtf8();
    m_loggedInAt = QDateTime::fromMSecsSinceEpoch(obj.value(QStringLiteral("loggedInAt")).toInteger());
    // Older session files predate the separate SSO clock — treat the login time as its start.
    m_ssoLoggedInAt = obj.contains(QStringLiteral("ssoLoggedInAt"))
        ? QDateTime::fromMSecsSinceEpoch(obj.value(QStringLiteral("ssoLoggedInAt")).toInteger())
        : m_loggedInAt;
    m_expired = obj.value(QStringLiteral("expired")).toBool();
}

void QisSession::save() const
{
    QJsonObject obj;
    obj[QStringLiteral("cookie")] = QString::fromUtf8(m_cookie);
    obj[QStringLiteral("loggedInAt")] = m_loggedInAt.toMSecsSinceEpoch();
    obj[QStringLiteral("ssoLoggedInAt")] = m_ssoLoggedInAt.toMSecsSinceEpoch();
    obj[QStringLiteral("expired")] = m_expired;
    // Owner-only from the start: it holds a live session cookie.
    QFile file(m_filePath);
    if (file.open(QIODevice::WriteOnly | QIODevice::Truncate, QFileDevice::ReadOwner | QFileDevice::WriteOwner))
        file.write(QJsonDocument(obj).toJson(QJsonDocument::Compact));
}

} // namespace stundenplan
