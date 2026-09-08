#pragma once

#include <QNetworkAccessManager>
#include <QObject>
#include <QString>
#include <QVariantMap>
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
    Q_PROPERTY(QString appVersionName READ appVersionName CONSTANT)
public:
    explicit UpdateManager(QObject *parent = nullptr);

    /** Emits updateAvailable() only if the latest release is newer than APP_VERSION_CODE. */
    Q_INVOKABLE void checkForUpdate();

    /** Always fetches the latest release notes, regardless of version — for the in-app changelog. */
    Q_INVOKABLE void fetchLatestReleaseNotes();

    Q_INVOKABLE void openReleasePage(const QString &url) const;

    /** The human-readable version shown in "Über die App" — compiled in from the same
     *  APP_VERSION_NAME CI passes as the release title, so it can't drift out of sync with it. */
    static QString appVersionName();

Q_SIGNALS:
    // Plain UpdateInfo structs aren't readable from QML (no Q_GADGET/Q_PROPERTY, so a field access
    // like `info.releaseNotes` just silently evaluates to undefined) — emit QVariantMap instead,
    // whose keys QML can read directly as object properties.
    void updateAvailable(const QVariantMap &info);
    void releaseNotesFetched(const QVariantMap &info);
    void updateCheckFailed(const QString &message);
    /** Update check succeeded but the installed version is already current — without this,
     *  clicking "Nach Updates suchen" while up to date looked exactly like a dead button. */
    void alreadyUpToDate();

private:
    void fetchLatestRelease(const std::function<void(std::optional<UpdateInfo>)> &callback);

    QNetworkAccessManager *m_manager;
};

} // namespace stundenplan
