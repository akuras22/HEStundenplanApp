#pragma once

#include "core/Models.h"
#include <QDate>
#include <QObject>
#include <QVariantList>
#include <QVariantMap>

namespace stundenplan {

class QisRepository;
class SettingsStore;
class TimetableCache;

/**
 * Central QObject exposed to QML (Pendant zu StundenplanViewModel): orchestrates the QIS
 * repository, the offline cache and the settings store, and exposes the current week's data as
 * QML-friendly QVariantList/QVariantMap values.
 */
class TimetableController : public QObject
{
    Q_OBJECT
    Q_PROPERTY(QVariantList studiengaenge READ studiengaenge NOTIFY studiengaengeChanged)
    Q_PROPERTY(QVariantList favoriteStudiengaenge READ favoriteStudiengaenge NOTIFY favoritesChanged)
    Q_PROPERTY(QVariantMap selectedStudiengang READ selectedStudiengangVariant NOTIFY selectedStudiengangChanged)
    Q_PROPERTY(QDate weekMonday READ weekMonday NOTIFY weekMondayChanged)
    Q_PROPERTY(bool loading READ loading NOTIFY loadingChanged)
    Q_PROPERTY(bool offline READ offline NOTIFY offlineChanged)
    Q_PROPERTY(QString errorMessage READ errorMessage NOTIFY errorMessageChanged)
    Q_PROPERTY(qint64 cacheTimestamp READ cacheTimestamp NOTIFY cacheTimestampChanged)
    Q_PROPERTY(QVariantList weekEvents READ weekEvents NOTIFY weekEventsChanged)

public:
    explicit TimetableController(SettingsStore *settings, TimetableCache *cache, QObject *parent = nullptr);

    QVariantList studiengaenge() const;
    QVariantList favoriteStudiengaenge() const;
    QVariantMap selectedStudiengangVariant() const;
    QDate weekMonday() const { return m_weekMonday; }
    bool loading() const { return m_loading; }
    bool offline() const { return m_offline; }
    QString errorMessage() const { return m_errorMessage; }
    qint64 cacheTimestamp() const { return m_cacheTimestamp; }
    QVariantList weekEvents() const { return m_weekEvents; }

    Q_INVOKABLE void loadStudiengaenge();
    Q_INVOKABLE void selectStudiengang(const QString &code, const QString &abstgvnr, const QString &parallelid);
    Q_INVOKABLE void setWeekMonday(const QDate &monday);
    Q_INVOKABLE void goToday();
    Q_INVOKABLE void refresh();
    Q_INVOKABLE void clearCache();
    Q_INVOKABLE void toggleFavorite(const QString &code, const QString &abstgvnr, const QString &parallelid);
    Q_INVOKABLE bool isFavorite(const QString &code, const QString &abstgvnr, const QString &parallelid) const;
    Q_INVOKABLE void hideGroup(const QString &groupKey, bool hidden);
    Q_INVOKABLE bool isGroupHidden(const QString &groupKey) const;

    /** All currently hidden recurring event groups, as {groupKey, title} entries (best-effort
     *  title lookup from the loaded week) — for the Darstellung "Einblenden" list. */
    Q_INVOKABLE QVariantList hiddenGroups() const;

    /** Events for one weekday (Qt::Monday==1 .. Qt::Friday==5) of the loaded week, with
     *  side-by-side overlap columns already assigned and hidden groups already excluded. */
    Q_INVOKABLE QVariantList eventsForDay(int qtDayOfWeek) const;

    /** Case-insensitive room/lecturer substring search across the loaded week. */
    Q_INVOKABLE QVariantList searchEvents(const QString &query) const;

    /** Next/current event today (for the Tag-view countdown banner), or an empty map if none. */
    Q_INVOKABLE QVariantMap nextEventToday() const;

    Q_INVOKABLE QString shareTextFor(const QVariantMap &event) const;

Q_SIGNALS:
    void studiengaengeChanged();
    void favoritesChanged();
    void selectedStudiengangChanged();
    void weekMondayChanged();
    void loadingChanged();
    void offlineChanged();
    void errorMessageChanged();
    void cacheTimestampChanged();
    void weekEventsChanged();

private:
    void setLoading(bool loading);
    void setOffline(bool offline);
    void setErrorMessage(const QString &message);
    void applyEvents(const QList<TimetableEvent> &events, bool fromCache);
    std::optional<Studiengang> currentStudiengang() const;

    SettingsStore *m_settings;
    TimetableCache *m_cache;
    QisRepository *m_repository;

    QList<Studiengang> m_allStudiengaenge;
    QDate m_weekMonday;
    QList<TimetableEvent> m_events;
    QVariantList m_weekEvents;
    bool m_loading = false;
    bool m_offline = false;
    QString m_errorMessage;
    qint64 m_cacheTimestamp = 0;
};

} // namespace stundenplan
