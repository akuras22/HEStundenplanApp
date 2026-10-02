#pragma once

#include "core/MensaModels.h"
#include <QDate>
#include <QHash>
#include <QObject>
#include <QVariantList>
#include <QVariantMap>
#include <optional>

namespace stundenplan {

class MensaRepository;
class SettingsStore;

/**
 * QObject behind the Mensa view (Pendant zum Mensa-Teil von StundenplanViewModel): the chosen
 * Mensa, the shown day, and that day's Speiseplan as QML-friendly values. Days are fetched live
 * and kept for the session only — the Speiseplan changes during the day (sold-out dishes get
 * swapped), so there is deliberately no offline copy of it.
 */
class MensaController : public QObject
{
    Q_OBJECT
    Q_PROPERTY(QVariantList locations READ locations CONSTANT)
    /** 0 until the user has picked a Mensa. */
    Q_PROPERTY(int locationId READ locationId WRITE setLocationId NOTIFY locationIdChanged)
    Q_PROPERTY(QVariantMap location READ location NOTIFY locationIdChanged)
    /** Set by the view while it's on screen — nothing is fetched before anyone looks. */
    Q_PROPERTY(bool active READ active WRITE setActive NOTIFY activeChanged)
    Q_PROPERTY(QDate date READ date NOTIFY dateChanged)
    /** "Montag, 5. Oktober 2026". */
    Q_PROPERTY(QString dateTitle READ dateTitle NOTIFY dateChanged)
    Q_PROPERTY(bool isToday READ isToday NOTIFY dateChanged)
    /** Monday..Friday of date's week, as {date, dayLabel, dateLabel} for the day switcher. */
    Q_PROPERTY(QVariantList weekDays READ weekDays NOTIFY dateChanged)
    /** Index of date within weekDays (0 = Monday). */
    Q_PROPERTY(int dayIndex READ dayIndex NOTIFY dateChanged)
    /** The shown day's meals grouped by category, in the site's order: [{name, meals: [...]}]. */
    Q_PROPERTY(QVariantList categories READ categories NOTIFY dayChanged)
    Q_PROPERTY(bool hasDay READ hasDay NOTIFY dayChanged)
    Q_PROPERTY(QString closedMessage READ closedMessage NOTIFY dayChanged)
    Q_PROPERTY(bool loading READ loading NOTIFY dayChanged)
    Q_PROPERTY(QString errorMessage READ errorMessage NOTIFY dayChanged)

public:
    explicit MensaController(SettingsStore *settings, QObject *parent = nullptr);

    QVariantList locations() const;
    int locationId() const;
    void setLocationId(int id);
    QVariantMap location() const;
    bool active() const { return m_active; }
    void setActive(bool active);
    QDate date() const { return m_date; }
    QString dateTitle() const;
    bool isToday() const { return m_date == QDate::currentDate(); }
    QVariantList weekDays() const;
    int dayIndex() const { return m_date.dayOfWeek() - 1; }
    QVariantList categories() const;
    bool hasDay() const;
    QString closedMessage() const;
    bool loading() const;
    QString errorMessage() const;

    /** Weekends snap forward to the following Monday — the Mensen are closed then anyway. */
    Q_INVOKABLE void setDate(const QDate &date);
    /** Jumps to the weekday at `index` (0 = Monday) of the shown week. */
    Q_INVOKABLE void selectWeekday(int index);
    /** Steps by weekdays, skipping Saturday/Sunday. */
    Q_INVOKABLE void stepDay(int delta);
    Q_INVOKABLE void goToday();
    Q_INVOKABLE void refresh();
    /** Forgets every fetched day and empties the photo disk cache ("Zwischenspeicher leeren"). */
    Q_INVOKABLE void clearCache();

Q_SIGNALS:
    void locationIdChanged();
    void activeChanged();
    void dateChanged();
    void dayChanged();

private:
    struct Entry {
        std::optional<MensaDay> day;
        QString error;
        bool loading = false;
    };

    static QString keyFor(int locationId, const QDate &date);
    const Entry *currentEntry() const;
    void load(const QDate &date, bool force);
    void loadCurrent(bool force = false);

    SettingsStore *m_settings;
    MensaRepository *m_repository;
    QHash<QString, Entry> m_entries;
    QDate m_date;
    bool m_active = false;
};

} // namespace stundenplan
