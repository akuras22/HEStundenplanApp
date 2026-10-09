#include "QisRepository.h"

#include "QisParser.h"
#include "QisSession.h"

#include <QNetworkReply>
#include <QNetworkRequest>
#include <QUrlQuery>

namespace stundenplan {

namespace {
// Without a login: plain http on purpose — since October 2026 the https side of QIS redirects
// every request to the HS Esslingen SSO login, while http still serves the public pages (QIS's
// own links are http too).
const char *kHttpBase = "http://www3.hs-esslingen.de/qislsf/rds";
// With a login (see QisSession): https plus its session cookie.
const char *kHttpsBase = "https://www3.hs-esslingen.de/qislsf/rds";
const char *kUserAgent =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/120.0.0.0 Mobile Safari/537.36";
constexpr int kRequestTimeoutMs = 15000;

bool isSsoLogin(const QUrl &url)
{
    const QString host = url.host();
    return host == QLatin1String("idp.hs-esslingen.de") || host == QLatin1String("login.hs-esslingen.de");
}
} // namespace

QisRepository::QisRepository(QisSession *session, QObject *parent)
    : QObject(parent)
    , m_manager(new QNetworkAccessManager(this))
    , m_session(session)
{
}

QNetworkReply *QisRepository::execute(const QUrlQuery &query, const std::function<void(const QString &)> &onSuccess, bool forTimetable)
{
    // Over https with the login if there is one, otherwise (or once QIS turns it away) over http.
    const QByteArray cookie = m_session ? m_session->activeCookieHeader() : QByteArray();
    QUrl url(QString::fromLatin1(cookie.isEmpty() ? kHttpBase : kHttpsBase));
    url.setQuery(query);

    QNetworkRequest request(url);
    request.setRawHeader("User-Agent", kUserAgent);
    request.setRawHeader("Accept-Language", "de-DE,de;q=0.9");
    if (!cookie.isEmpty())
        request.setRawHeader("Cookie", cookie);
    // Without this, a stalled connection (bad campus wifi, a hung proxy) leaves the UI's loading
    // spinner running forever instead of surfacing an error the offline-cache fallback can act on.
    request.setTransferTimeout(kRequestTimeoutMs);

    QNetworkReply *reply = m_manager->get(request);
    const bool withLogin = !cookie.isEmpty();
    connect(reply, &QNetworkReply::finished, this, [this, reply, query, onSuccess, forTimetable, url, withLogin]() {
        reply->deleteLater();
        if (reply->error() == QNetworkReply::OperationCanceledError) {
            // Superseded by a newer request (see fetchTimetable's abort-the-previous-one logic) —
            // not a real failure, so no fetchFailed here; the newer request's own callback wins.
            return;
        }
        if (reply->error() == QNetworkReply::NoError && isSsoLogin(reply->url())) {
            if (withLogin) {
                // The session ran out on QIS's side (idle too long, or ended early) —
                // Einstellungen shows that, and this request carries on without it.
                m_session->markExpired();
                QNetworkReply *retry = execute(query, onSuccess, forTimetable);
                if (forTimetable)
                    m_pendingTimetableReply = retry;
                return;
            }
            if (m_session)
                m_session->setLoginRequired(true);
            Q_EMIT fetchFailed(QStringLiteral("Die Hochschule verlangt eine Anmeldung. Melde dich unter Einstellungen ▸ "
                                              "Hochschul-Login an."),
                                forTimetable);
            return;
        }
        if (reply->error() != QNetworkReply::NoError) {
            Q_EMIT fetchFailed(QStringLiteral("HTTP-Fehler beim Laden von %1: %2").arg(url.toString(), reply->errorString()),
                                forTimetable);
            return;
        }
        const QByteArray body = reply->readAll();
        if (body.isEmpty()) {
            Q_EMIT fetchFailed(QStringLiteral("Leere Antwort von %1").arg(url.toString()), forTimetable);
            return;
        }
        if (m_session)
            m_session->setLoginRequired(false);
        onSuccess(QString::fromUtf8(body));
    });
    return reply;
}

void QisRepository::fetchStudiengaenge()
{
    QUrlQuery query;
    query.addQueryItem(QStringLiteral("state"), QStringLiteral("verpublish"));
    query.addQueryItem(QStringLiteral("publishContainer"), QStringLiteral("stgPlanList"));
    query.addQueryItem(QStringLiteral("navigationPosition"), QStringLiteral("lectures,curriculaschedulesList"));
    query.addQueryItem(QStringLiteral("breadcrumb"), QStringLiteral("curriculaschedules"));
    query.addQueryItem(QStringLiteral("topitem"), QStringLiteral("lectures"));
    query.addQueryItem(QStringLiteral("subitem"), QStringLiteral("curriculaschedulesList"));

    execute(
        query,
        [this](const QString &html) {
            Q_EMIT studiengaengeFetched(QisParser::parseStudiengangList(html));
        },
        false);
}

void QisRepository::fetchTimetable(const Studiengang &studiengang, const QDate &weekMonday)
{
    // A rapid string of "next week" clicks would otherwise leave several requests in flight;
    // whichever happened to respond last would win regardless of which week it was actually for.
    if (m_pendingTimetableReply)
        m_pendingTimetableReply->abort();

    int isoYear = 0;
    const int isoWeek = weekMonday.weekNumber(&isoYear);

    QUrlQuery query;
    query.addQueryItem(QStringLiteral("state"), QStringLiteral("wplan"));
    query.addQueryItem(QStringLiteral("act"), QStringLiteral("stg"));
    query.addQueryItem(QStringLiteral("pool"), QStringLiteral("stg"));
    query.addQueryItem(QStringLiteral("show"), QStringLiteral("plan"));
    query.addQueryItem(QStringLiteral("P.vx"), QStringLiteral("lang"));
    query.addQueryItem(QStringLiteral("week"), QStringLiteral("%1_%2").arg(isoWeek).arg(isoYear));
    query.addQueryItem(QStringLiteral("k_parallel.parallelid"), studiengang.parallelid);
    query.addQueryItem(QStringLiteral("k_abstgv.abstgvnr"), studiengang.abstgvnr);
    query.addQueryItem(QStringLiteral("noDBAction"), QStringLiteral("y"));

    m_pendingTimetableReply = execute(
        query,
        [this](const QString &html) {
            QString errorMessage;
            const auto events = QisParser::parseTimetable(html, &errorMessage);
            if (!errorMessage.isEmpty())
                Q_EMIT fetchFailed(errorMessage, true);
            else
                Q_EMIT timetableFetched(events);
        },
        true);
}

} // namespace stundenplan
