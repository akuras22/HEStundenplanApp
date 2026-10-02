#pragma once

#include "core/TransitParser.h"
#include <QObject>
#include <QTimer>
#include <QVariantList>
#include <QVariantMap>

namespace stundenplan {

class SettingsStore;
class TimetableController;
class TransitRepository;

/**
 * QObject behind the Abfahrten view (Pendant zum Abfahrten-Teil von StundenplanViewModel): the
 * chosen VVS stop and its live departure board — from now, or from when today's last lecture
 * ends — refreshed every minute while the view is on screen.
 */
class TransitController : public QObject
{
    Q_OBJECT
    /** Campus stops: [{id, name, shortName, campus}]. */
    Q_PROPERTY(QVariantList presets READ presets CONSTANT)
    Q_PROPERTY(QVariantList recentStops READ recentStops NOTIFY stopChanged)
    /** {id, name, shortName}; empty until a stop is picked (or derivable from the chosen Mensa). */
    Q_PROPERTY(QVariantMap stop READ stop NOTIFY stopChanged)
    /** Set by the view while it's on screen — nothing is fetched (or auto-refreshed) otherwise. */
    Q_PROPERTY(bool active READ active WRITE setActive NOTIFY activeChanged)
    /** "Nach der Vorlesung": departures from when today's last lecture ends instead of now. */
    Q_PROPERTY(bool afterLecture READ afterLecture WRITE setAfterLecture NOTIFY afterLectureChanged)
    /** "17:15", or empty when today has no lecture left — then only "Jetzt" is offered. */
    Q_PROPERTY(QString lectureEndLabel READ lectureEndLabel NOTIFY boardChanged)
    /** [{line, productClass, destination, platform, plannedLabel, delay (-1 = no real-time data),
     *   cancelled, expectedMsecs, notices: [{title, text}]}] */
    Q_PROPERTY(QVariantList departures READ departures NOTIFY boardChanged)
    Q_PROPERTY(bool loading READ loading NOTIFY boardChanged)
    Q_PROPERTY(QString errorMessage READ errorMessage NOTIFY boardChanged)
    /** "Stand 14:52", or empty before the first answer. */
    Q_PROPERTY(QString updatedLabel READ updatedLabel NOTIFY boardChanged)
    Q_PROPERTY(QVariantList searchResults READ searchResults NOTIFY searchChanged)
    Q_PROPERTY(bool searching READ searching NOTIFY searchChanged)
    Q_PROPERTY(QString searchError READ searchError NOTIFY searchChanged)

public:
    TransitController(SettingsStore *settings, TimetableController *timetable, QObject *parent = nullptr);

    QVariantList presets() const;
    QVariantList recentStops() const;
    QVariantMap stop() const;
    bool active() const { return m_active; }
    void setActive(bool active);
    bool afterLecture() const { return m_afterLecture; }
    void setAfterLecture(bool afterLecture);
    QString lectureEndLabel() const;
    QVariantList departures() const { return m_departures; }
    bool loading() const { return m_loading; }
    QString errorMessage() const { return m_errorMessage; }
    QString updatedLabel() const;
    QVariantList searchResults() const { return m_searchResults; }
    bool searching() const { return m_searching; }
    QString searchError() const { return m_searchError; }

    Q_INVOKABLE void setStop(const QString &id, const QString &name, const QString &shortName);
    Q_INVOKABLE void refresh();
    Q_INVOKABLE void search(const QString &query);

Q_SIGNALS:
    void stopChanged();
    void activeChanged();
    void afterLectureChanged();
    void boardChanged();
    void searchChanged();

private:
    std::optional<TransitStop> currentStop() const;
    QDateTime boardTime() const;

    SettingsStore *m_settings;
    TimetableController *m_timetable;
    TransitRepository *m_repository;
    QTimer m_refreshTimer;
    bool m_active = false;
    bool m_afterLecture = false;
    QString m_boardKey; // stop id + time the shown departures belong to
    QVariantList m_departures;
    bool m_loading = false;
    QString m_errorMessage;
    QDateTime m_updatedAt;
    QVariantList m_searchResults;
    bool m_searching = false;
    QString m_searchError;
};

} // namespace stundenplan
