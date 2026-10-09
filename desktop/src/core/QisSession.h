#pragma once

#include <QByteArray>
#include <QDateTime>
#include <QMap>
#include <QObject>

namespace stundenplan {

/**
 * The optional QIS login (Einstellungen ▸ Hochschul-Login): since October 2026 https QIS pages
 * need the HS Esslingen SSO login (username, password and a second factor), which only a real
 * browser can do — so the login runs in a WebEngineView (LoginWebPage.qml), and only the
 * Shibboleth session cookie it leaves behind for www3.hs-esslingen.de is kept, in an owner-only
 * file. QisRepository sends it with every request; without it (or once QIS turns it away) the
 * public pages are loaded anonymously over http instead. Ported from QisSession.kt.
 */
class QisSession : public QObject
{
    Q_OBJECT
    Q_PROPERTY(bool hasLogin READ hasLogin NOTIFY changed)
    /** QIS no longer accepts the login (or its maximum lifetime has passed). */
    Q_PROPERTY(bool expired READ expired NOTIFY changed)
    Q_PROPERTY(QDateTime loggedInAt READ loggedInAt NOTIFY changed)
    Q_PROPERTY(QDateTime expiresAt READ expiresAt NOTIFY changed)
    /** When the SSO session runs out — after this a silent refresh no longer works and a full
     *  login (password + second factor) is needed again. */
    Q_PROPERTY(QDateTime ssoExpiresAt READ ssoExpiresAt NOTIFY changed)
    /** The last anonymous request was turned away with the SSO login — only logging in helps. */
    Q_PROPERTY(bool loginRequired READ loginRequired NOTIFY loginRequiredChanged)
    /** True while the invisible SSO refresh view (SilentLoginRefresh.qml) should be mounted — it
     *  renews the QIS session from the still-valid SSO session without any UI. */
    Q_PROPERTY(bool silentRefreshActive READ silentRefreshActive NOTIFY silentRefreshActiveChanged)

public:
    explicit QisSession(QObject *parent = nullptr);

    bool hasLogin() const { return !m_cookie.isEmpty(); }
    bool expired() const { return m_expired; }
    QDateTime loggedInAt() const { return m_loggedInAt; }
    QDateTime expiresAt() const;
    QDateTime ssoExpiresAt() const;
    bool loginRequired() const { return m_loginRequired; }
    bool silentRefreshActive() const { return m_silentRefreshActive; }

    /** The Cookie header to send, or empty if there is no login or it has expired. */
    QByteArray activeCookieHeader();
    void markExpired();
    void setLoginRequired(bool required);

    /** Whether the SSO session is still young enough that a fresh QIS session cookie can be
     *  fetched from it without logging in again (see SilentLoginRefresh.qml). */
    Q_INVOKABLE bool canSilentlyRefresh() const;

    /** Starts collecting the Shibboleth cookies the login page's WebEngineProfile receives. */
    Q_INVOKABLE void attachProfile(QObject *webEngineProfile);
    /** Saves the collected session cookie as the login — false if the login hasn't produced one.
     *  @param fullLogin true for a real SSO login (restarts the SSO session clock), false for a
     *   silent refresh (keeps the original clock — reusing the SSO session doesn't extend it). */
    Q_INVOKABLE bool completeLogin(bool fullLogin = true);
    /** Called by the silent refresh view when the SSO session turned out to be gone too — stops
     *  retrying until the next real login. */
    Q_INVOKABLE void failSilentRefresh();
    /** Mounts the silent refresh view if the current session has run out but the SSO session is
     *  still alive. Called on startup and whenever the session expires. */
    Q_INVOKABLE void startSilentRefreshIfPossible();
    Q_INVOKABLE void logout();

Q_SIGNALS:
    void changed();
    void loginRequiredChanged();
    void silentRefreshActiveChanged();

private:
    void load();
    void save() const;
    void setSilentRefreshActive(bool active);

    QString m_filePath;
    QByteArray m_cookie;
    QDateTime m_loggedInAt;
    // When the user last did a full SSO login — the SSO session lives this long regardless of how
    // often the QIS cookie is silently renewed from it, so it's the real re-login deadline.
    QDateTime m_ssoLoggedInAt;
    bool m_expired = false;
    bool m_loginRequired = false;
    bool m_silentRefreshActive = false;
    // Set once a silent refresh failed (SSO really gone), so we don't keep retrying until the next
    // real login. Not persisted: a fresh process gets one more attempt.
    bool m_silentRefreshExhausted = false;
    // Cookie name -> value, as the login page receives them (see attachProfile).
    QMap<QByteArray, QByteArray> m_collectedCookies;
};

} // namespace stundenplan
