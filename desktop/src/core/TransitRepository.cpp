#include "TransitRepository.h"

#include <QNetworkRequest>
#include <QUrlQuery>

namespace stundenplan {

namespace {
const char *kBase = "https://www3.vvs.de/mngvvs/";
constexpr int kRequestTimeoutMs = 15000;

QString friendlyMessage(const QNetworkReply *reply)
{
    switch (reply->error()) {
    case QNetworkReply::HostNotFoundError:
    case QNetworkReply::TemporaryNetworkFailureError:
    case QNetworkReply::NetworkSessionFailedError:
        return QStringLiteral("Keine Internetverbindung.");
    case QNetworkReply::TimeoutError:
        return QStringLiteral("Die VVS-Auskunft antwortet nicht. Bitte später erneut versuchen.");
    default:
        break;
    }
    const int status = reply->attribute(QNetworkRequest::HttpStatusCodeAttribute).toInt();
    if (status >= 400)
        return QStringLiteral("Die VVS-Auskunft ist gerade nicht erreichbar (Fehler %1).").arg(status);
    return QStringLiteral("Verbindung zur VVS-Auskunft fehlgeschlagen.");
}

QUrlQuery baseQuery()
{
    QUrlQuery query;
    query.addQueryItem(QStringLiteral("SpEncId"), QStringLiteral("0"));
    query.addQueryItem(QStringLiteral("coordOutputFormat"), QStringLiteral("EPSG:4326"));
    query.addQueryItem(QStringLiteral("outputFormat"), QStringLiteral("rapidJSON"));
    return query;
}
} // namespace

TransitRepository::TransitRepository(QObject *parent)
    : QObject(parent)
    , m_manager(new QNetworkAccessManager(this))
{
}

QNetworkReply *TransitRepository::get(const QUrl &url,
                                      const std::function<void(const QByteArray &)> &onSuccess,
                                      const std::function<void(const QString &)> &onError)
{
    QNetworkRequest request(url);
    request.setRawHeader("User-Agent", "HEStundenplan-desktop (+https://github.com/akuras22/HEStundenplanApp)");
    request.setTransferTimeout(kRequestTimeoutMs);
    QNetworkReply *reply = m_manager->get(request);
    connect(reply, &QNetworkReply::finished, this, [reply, onSuccess, onError]() {
        reply->deleteLater();
        if (reply->error() == QNetworkReply::OperationCanceledError)
            return; // superseded by a newer request, see fetchDepartures/searchStops
        if (reply->error() != QNetworkReply::NoError) {
            onError(friendlyMessage(reply));
            return;
        }
        onSuccess(reply->readAll());
    });
    return reply;
}

void TransitRepository::fetchDepartures(const QString &stopId,
                                        const QDateTime &at,
                                        const std::function<void(const QList<Departure> &)> &onSuccess,
                                        const std::function<void(const QString &)> &onError)
{
    // Switching stops (or "Jetzt" / "Nach der Vorlesung") quickly must not let an older, slower
    // answer overwrite the newer one.
    if (m_pendingDepartures)
        m_pendingDepartures->abort();

    QUrlQuery query = baseQuery();
    query.addQueryItem(QStringLiteral("language"), QStringLiteral("de"));
    query.addQueryItem(QStringLiteral("type_dm"), QStringLiteral("any"));
    query.addQueryItem(QStringLiteral("name_dm"), stopId);
    query.addQueryItem(QStringLiteral("mode"), QStringLiteral("direct"));
    // Only this stop — without it EFA mixes in departures from stops nearby.
    query.addQueryItem(QStringLiteral("deleteAssignedStops_dm"), QStringLiteral("1"));
    query.addQueryItem(QStringLiteral("useRealtime"), QStringLiteral("1"));
    query.addQueryItem(QStringLiteral("itdDateTimeDepArr"), QStringLiteral("dep"));
    query.addQueryItem(QStringLiteral("limit"), QStringLiteral("30"));
    if (at.isValid()) {
        const QDateTime local = at.toLocalTime();
        query.addQueryItem(QStringLiteral("itdDate"), local.toString(QStringLiteral("yyyyMMdd")));
        query.addQueryItem(QStringLiteral("itdTime"), local.toString(QStringLiteral("HHmm")));
    }
    QUrl url(QString::fromLatin1(kBase) + QStringLiteral("XML_DM_REQUEST"));
    url.setQuery(query);
    m_pendingDepartures = get(
        url, [onSuccess](const QByteArray &body) { onSuccess(TransitParser::parseDepartures(body)); }, onError);
}

void TransitRepository::searchStops(const QString &query,
                                    const std::function<void(const QList<TransitStop> &)> &onSuccess,
                                    const std::function<void(const QString &)> &onError)
{
    if (m_pendingSearch)
        m_pendingSearch->abort();

    QUrlQuery urlQuery = baseQuery();
    urlQuery.addQueryItem(QStringLiteral("locationServerActive"), QStringLiteral("1"));
    urlQuery.addQueryItem(QStringLiteral("type_sf"), QStringLiteral("any"));
    urlQuery.addQueryItem(QStringLiteral("name_sf"), query);
    // Stops only — no addresses or points of interest.
    urlQuery.addQueryItem(QStringLiteral("anyObjFilter_sf"), QStringLiteral("2"));
    QUrl url(QString::fromLatin1(kBase) + QStringLiteral("XML_STOPFINDER_REQUEST"));
    url.setQuery(urlQuery);
    m_pendingSearch = get(
        url, [onSuccess](const QByteArray &body) { onSuccess(TransitParser::parseStops(body)); }, onError);
}

} // namespace stundenplan
