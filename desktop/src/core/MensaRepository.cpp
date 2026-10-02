#include "MensaRepository.h"

#include "MensaParser.h"

#include <QNetworkReply>
#include <QNetworkRequest>
#include <QUrlQuery>

namespace stundenplan {

namespace {
const char *kUserAgent =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/120.0.0.0 Mobile Safari/537.36";
constexpr int kRequestTimeoutMs = 15000;

/** Short, actionable German text instead of Qt's raw error strings (which name URLs and
 *  internals the user can do nothing with) — same wording as the Android app's. */
QString friendlyMessage(const QNetworkReply *reply)
{
    switch (reply->error()) {
    case QNetworkReply::HostNotFoundError:
    case QNetworkReply::TemporaryNetworkFailureError:
    case QNetworkReply::NetworkSessionFailedError:
        return QStringLiteral("Keine Internetverbindung.");
    case QNetworkReply::TimeoutError: // also what setTransferTimeout() reports
        return QStringLiteral("Die Speiseplan-Seite antwortet nicht. Bitte später erneut versuchen.");
    default:
        break;
    }
    const int status = reply->attribute(QNetworkRequest::HttpStatusCodeAttribute).toInt();
    if (status >= 400)
        return QStringLiteral("Die Speiseplan-Seite ist gerade nicht erreichbar (Fehler %1).").arg(status);
    return QStringLiteral("Verbindung zur Speiseplan-Seite fehlgeschlagen.");
}
} // namespace

MensaRepository::MensaRepository(QObject *parent)
    : QObject(parent)
    , m_manager(new QNetworkAccessManager(this))
{
}

void MensaRepository::fetchDay(int locationId, const QDate &date)
{
    // The backend answers HTTP 500 without the two week parameters, even though the day alone
    // decides what it returns — any Monday works, so send the requested day's own week.
    const QDate monday = date.addDays(1 - date.dayOfWeek());
    QUrlQuery form;
    form.addQueryItem(QStringLiteral("func"), QStringLiteral("make_spl"));
    form.addQueryItem(QStringLiteral("locId"), QString::number(locationId));
    form.addQueryItem(QStringLiteral("date"), date.toString(Qt::ISODate));
    form.addQueryItem(QStringLiteral("lang"), QStringLiteral("de"));
    form.addQueryItem(QStringLiteral("startThisWeek"), monday.toString(Qt::ISODate));
    form.addQueryItem(QStringLiteral("startNextWeek"), monday.addDays(7).toString(Qt::ISODate));

    QNetworkRequest request(QUrl(QString::fromLatin1(MensaParser::kBaseUrl) + QStringLiteral("inc/ajax-php_konnektor.inc.php")));
    request.setRawHeader("User-Agent", kUserAgent);
    request.setRawHeader("Accept-Language", "de-DE,de;q=0.9");
    request.setHeader(QNetworkRequest::ContentTypeHeader, QStringLiteral("application/x-www-form-urlencoded"));
    request.setTransferTimeout(kRequestTimeoutMs);

    QNetworkReply *reply = m_manager->post(request, form.toString(QUrl::FullyEncoded).toUtf8());
    connect(reply, &QNetworkReply::finished, this, [this, reply, locationId, date]() {
        reply->deleteLater();
        if (reply->error() != QNetworkReply::NoError) {
            Q_EMIT fetchFailed(locationId, date, friendlyMessage(reply));
            return;
        }
        Q_EMIT dayFetched(locationId, date, MensaParser::parseDay(QString::fromUtf8(reply->readAll())));
    });
}

} // namespace stundenplan
