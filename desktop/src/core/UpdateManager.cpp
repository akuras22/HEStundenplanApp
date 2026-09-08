#include "UpdateManager.h"

#include <QDesktopServices>
#include <QJsonArray>
#include <QJsonDocument>
#include <QJsonObject>
#include <QNetworkReply>
#include <QNetworkRequest>
#include <QUrl>

#ifndef APP_VERSION_CODE
#define APP_VERSION_CODE 0
#endif
#ifndef APP_VERSION_NAME
#define APP_VERSION_NAME "dev"
#endif

namespace stundenplan {

namespace {
const char *kReleasesUrl = "https://api.github.com/repos/akuras22/HEStundenplanApp/releases/latest";

QVariantMap toVariantMap(const UpdateInfo &info)
{
    QVariantMap map;
    map[QStringLiteral("versionCode")] = info.versionCode;
    map[QStringLiteral("versionName")] = info.versionName;
    map[QStringLiteral("releaseUrl")] = info.releaseUrl;
    map[QStringLiteral("releaseNotes")] = info.releaseNotes;
    return map;
}
} // namespace

UpdateManager::UpdateManager(QObject *parent)
    : QObject(parent)
    , m_manager(new QNetworkAccessManager(this))
{
}

void UpdateManager::fetchLatestRelease(const std::function<void(std::optional<UpdateInfo>)> &callback)
{
    QNetworkRequest request{QUrl(QString::fromLatin1(kReleasesUrl))};
    request.setRawHeader("Accept", "application/vnd.github+json");

    QNetworkReply *reply = m_manager->get(request);
    connect(reply, &QNetworkReply::finished, this, [reply, callback]() {
        reply->deleteLater();
        if (reply->error() != QNetworkReply::NoError) {
            callback(std::nullopt);
            return;
        }
        const auto doc = QJsonDocument::fromJson(reply->readAll());
        if (!doc.isObject()) {
            callback(std::nullopt);
            return;
        }
        const QJsonObject json = doc.object();
        const QString tag = json.value(QStringLiteral("tag_name")).toString();
        // Tags are "v<github.run_number>" (see .github/workflows/release.yml), same convention
        // as the Android app's BuildConfig.VERSION_CODE — a plain increasing integer, not semver.
        bool ok = false;
        const int remoteVersionCode = QStringView(tag).mid(tag.startsWith(QLatin1Char('v')) ? 1 : 0).toInt(&ok);
        if (!ok) {
            callback(std::nullopt);
            return;
        }
        UpdateInfo info;
        info.versionCode = remoteVersionCode;
        const QString name = json.value(QStringLiteral("name")).toString();
        info.versionName = name.isEmpty() ? tag : name;
        info.releaseUrl = json.value(QStringLiteral("html_url")).toString();
        info.releaseNotes = json.value(QStringLiteral("body")).toString();
        callback(info);
    });
}

void UpdateManager::checkForUpdate()
{
    fetchLatestRelease([this](std::optional<UpdateInfo> info) {
        if (!info.has_value()) {
            Q_EMIT updateCheckFailed(QStringLiteral("Update-Prüfung fehlgeschlagen."));
            return;
        }
        if (info->versionCode > APP_VERSION_CODE) {
            Q_EMIT updateAvailable(toVariantMap(*info));
        } else {
            // Otherwise a click on "Nach Updates suchen" had no visible effect at all —
            // silence reads as "the button doesn't do anything", not "you're already current".
            Q_EMIT alreadyUpToDate();
        }
    });
}

QString UpdateManager::appVersionName()
{
    return QStringLiteral(APP_VERSION_NAME);
}

void UpdateManager::fetchLatestReleaseNotes()
{
    fetchLatestRelease([this](std::optional<UpdateInfo> info) {
        if (info.has_value())
            Q_EMIT releaseNotesFetched(toVariantMap(*info));
        else
            Q_EMIT updateCheckFailed(QStringLiteral("Änderungsprotokoll konnte nicht geladen werden."));
    });
}

void UpdateManager::openReleasePage(const QString &url) const
{
    QDesktopServices::openUrl(QUrl(url));
}

} // namespace stundenplan
