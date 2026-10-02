#pragma once

#include "Models.h"
#include "TransitParser.h"
#include <KConfigGroup>
#include <KSharedConfig>
#include <QObject>
#include <QSet>
#include <QVariantList>
#include <optional>

namespace stundenplan {

/**
 * Persists user settings via KConfig (~/.config/hestundenplanrc) — the chosen/favorite
 * Studiengänge, hidden recurring event groups, appearance and notification preferences. Never any
 * timetable data itself (see TimetableCache for that). Ported from SettingsStore.kt.
 *
 * Unlike the Android original, there is no theme/accent-color override here: Kirigami's chrome
 * always follows the system Plasma theme and accent regardless of what an app requests, so this
 * app just does too rather than offer a setting that only ever partially applied.
 */
class SettingsStore : public QObject
{
    Q_OBJECT
    Q_PROPERTY(bool defaultViewIsDay READ defaultViewIsDay WRITE setDefaultViewIsDay NOTIFY defaultViewIsDayChanged)
    Q_PROPERTY(bool blockShowTime READ blockShowTime WRITE setBlockShowTime NOTIFY blockShowTimeChanged)
    Q_PROPERTY(bool blockShowRoom READ blockShowRoom WRITE setBlockShowRoom NOTIFY blockShowRoomChanged)
    Q_PROPERTY(bool blockShowLecturer READ blockShowLecturer WRITE setBlockShowLecturer NOTIFY blockShowLecturerChanged)
    Q_PROPERTY(bool remindersEnabled READ remindersEnabled WRITE setRemindersEnabled NOTIFY remindersEnabledChanged)
    Q_PROPERTY(QVariantList reminderLeadMinutes READ reminderLeadMinutes WRITE setReminderLeadMinutes NOTIFY
                   reminderLeadMinutesChanged)
    Q_PROPERTY(int mensaLocationId READ mensaLocationId WRITE setMensaLocationId NOTIFY mensaLocationIdChanged)
    Q_PROPERTY(bool runInBackground READ runInBackground WRITE setRunInBackground NOTIFY runInBackgroundChanged)
    Q_PROPERTY(bool autostart READ autostart WRITE setAutostart NOTIFY autostartChanged)

public:
    explicit SettingsStore(QObject *parent = nullptr);

    std::optional<Studiengang> selectedStudiengang() const;
    Q_INVOKABLE void setSelectedStudiengang(const Studiengang &studiengang);

    QList<Studiengang> favoriteStudiengaenge() const;
    Q_INVOKABLE void setFavorite(const Studiengang &studiengang, bool favorite);
    Q_INVOKABLE bool isFavorite(const Studiengang &studiengang) const;

    QSet<QString> hiddenEventKeys() const;
    Q_INVOKABLE void setHidden(const QString &groupKey, bool hidden);

    qint64 lastUpdateCheckAt() const;
    void setLastUpdateCheckAt(qint64 msecsSinceEpoch);

    bool remindersEnabled() const;
    void setRemindersEnabled(bool enabled);

    bool hasNotifiedToday(const QString &date, const QString &groupKey) const;
    void markNotifiedToday(const QString &date, const QString &groupKey);

    QSet<int> reminderLeadMinutesSet() const;
    QVariantList reminderLeadMinutes() const;
    void setReminderLeadMinutes(const QVariantList &minutes);

    bool defaultViewIsDay() const;
    void setDefaultViewIsDay(bool isDay);

    bool blockShowTime() const;
    void setBlockShowTime(bool show);
    bool blockShowRoom() const;
    void setBlockShowRoom(bool show);
    bool blockShowLecturer() const;
    void setBlockShowLecturer(bool show);

    Q_INVOKABLE void resetAppearance();

    /** The Mensa whose Speiseplan the Mensa view shows (a mensaLocations() id) — 0 until the
     *  user picks one, since there's no way to tell which campus they're on. */
    int mensaLocationId() const;
    void setMensaLocationId(int id);

    /** Keep running (for reminders) once the window is closed — see BackgroundService. */
    bool runInBackground() const;
    void setRunInBackground(bool enabled);
    /** Start hidden at login (only together with runInBackground). */
    bool autostart() const;
    void setAutostart(bool enabled);
    bool backgroundHintShown() const;
    void setBackgroundHintShown(bool shown);

    /** The stop the Abfahrten view shows — nullopt until one is picked. */
    std::optional<TransitStop> transitStop() const;
    /** Also remembers `stop` among the recent ones when it isn't a campus preset. */
    void setTransitStop(const TransitStop &stop, bool isPreset);
    /** Stops picked through the search, newest first. */
    QList<TransitStop> recentTransitStops() const;

Q_SIGNALS:
    void defaultViewIsDayChanged();
    void blockShowTimeChanged();
    void blockShowRoomChanged();
    void blockShowLecturerChanged();
    void remindersEnabledChanged();
    void reminderLeadMinutesChanged();
    void favoritesChanged();
    void hiddenEventKeysChanged();
    void selectedStudiengangChanged();
    void mensaLocationIdChanged();
    void transitStopChanged();
    void runInBackgroundChanged();
    void autostartChanged();

private:
    KConfigGroup group(const QString &name) const;
    KSharedConfigPtr m_config;
};

} // namespace stundenplan
