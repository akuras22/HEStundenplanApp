#include "QisRepository.h"

#include "QisParser.h"

#include <QNetworkReply>
#include <QNetworkRequest>
#include <QUrlQuery>

namespace stundenplan {

namespace {
const char *kBase = "https://www3.hs-esslingen.de/qislsf/rds";
const char *kUserAgent =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/120.0.0.0 Mobile Safari/537.36";
constexpr int kRequestTimeoutMs = 15000;
} // namespace

QisRepository::QisRepository(QObject *parent)
    : QObject(parent)
    , m_manager(new QNetworkAccessManager(this))
{
}

QNetworkReply *QisRepository::execute(const QUrl &url, const std::function<void(const QString &)> &onSuccess, bool forTimetable)
{
    QNetworkRequest request(url);
    request.setRawHeader("User-Agent", kUserAgent);
    request.setRawHeader("Accept-Language", "de-DE,de;q=0.9");
    // Without this, a stalled connection (bad campus wifi, a hung proxy) leaves the UI's loading
    // spinner running forever instead of surfacing an error the offline-cache fallback can act on.
    request.setTransferTimeout(kRequestTimeoutMs);

    QNetworkReply *reply = m_manager->get(request);
    connect(reply, &QNetworkReply::finished, this, [this, reply, onSuccess, forTimetable, url]() {
        reply->deleteLater();
        if (reply->error() == QNetworkReply::OperationCanceledError) {
            // Superseded by a newer request (see fetchTimetable's abort-the-previous-one logic) —
            // not a real failure, so no fetchFailed here; the newer request's own callback wins.
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
        onSuccess(QString::fromUtf8(body));
    });
    return reply;
}

void QisRepository::fetchStudiengaenge()
{
    QUrl url(QString::fromLatin1(kBase));
    QUrlQuery query;
    query.addQueryItem(QStringLiteral("state"), QStringLiteral("verpublish"));
    query.addQueryItem(QStringLiteral("publishContainer"), QStringLiteral("stgPlanList"));
    query.addQueryItem(QStringLiteral("navigationPosition"), QStringLiteral("lectures,curriculaschedulesList"));
    query.addQueryItem(QStringLiteral("breadcrumb"), QStringLiteral("curriculaschedules"));
    query.addQueryItem(QStringLiteral("topitem"), QStringLiteral("lectures"));
    query.addQueryItem(QStringLiteral("subitem"), QStringLiteral("curriculaschedulesList"));
    url.setQuery(query);

    execute(
        url,
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

    QUrl url(QString::fromLatin1(kBase));
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
    url.setQuery(query);

    m_pendingTimetableReply = execute(
        url,
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
