#pragma once

#include <QNetworkAccessManager>
#include <QObject>
#include <QString>
#include <optional>

namespace stundenplan {

struct UpdateInfo {
    int versionCode = 0;    // parsed from the release tag "v<run number>"
    QString versionName;
    QString releaseUrl;
    QString releaseNotes;
};

/**
 * OTA-update-notice source: GitHub Releases on the project repo. The repo ships one combined
 * release per push to main (see .github/workflows/release.yml), tagged "v<run number>" and
 * carrying both the Android APK and the desktop packages — so this mirrors the Android
 * UpdateManager.kt exactly: compare the tag's numeric suffix against our own compiled-in
 * APP_VERSION_CODE (set from the same github.run_number at CI build time; 0 for local/dev
 * builds, which never reports an update). Unlike Android, there's no APK asset to point at, so
 * this only ever opens the release page in the browser.
 */
class UpdateManager : public QObject
{
    Q_OBJECT
public:
    explicit UpdateManager(QObject *parent = nullptr);

    /** Emits updateAvailable() only if the latest release is newer than APP_VERSION_CODE. */
    void checkForUpdate();

    /** Always fetches the latest release notes, regardless of version — for the in-app changelog. */
    void fetchLatestReleaseNotes();

    Q_INVOKABLE void openReleasePage(const QString &url) const;

Q_SIGNALS:
    void updateAvailable(const stundenplan::UpdateInfo &info);
    void releaseNotesFetched(const stundenplan::UpdateInfo &info);
    void updateCheckFailed(const QString &message);

private:
    void fetchLatestRelease(const std::function<void(std::optional<UpdateInfo>)> &callback);

    QNetworkAccessManager *m_manager;
};

} // namespace stundenplan

Q_DECLARE_METATYPE(stundenplan::UpdateInfo)
