#include "TransitController.h"

#include "TimetableController.h"
#include "core/SettingsStore.h"
#include "core/TransitRepository.h"

#include <QTime>

namespace stundenplan {

namespace {

struct Preset {
    TransitStop stop;
    QString campus;
};

// The stops next to the campuses, offered up front in every stop picker. Same list as
// TransitStops.presets in the Android app.
const QList<Preset> &presetList()
{
    static const QList<Preset> presets = {
        {{QStringLiteral("de:08116:3998"), QStringLiteral("Esslingen (N), Hochschulzentrum"), QStringLiteral("Hochschulzentrum")},
         QStringLiteral("Campus Flandernstraße")},
        {{QStringLiteral("de:08116:4022"), QStringLiteral("Esslingen (N), Hochschule"), QStringLiteral("Hochschule")},
         QStringLiteral("Campus Stadtmitte")},
        {{QStringLiteral("de:08116:7800"), QStringLiteral("Esslingen (N), Bahnhof"), QStringLiteral("Esslingen Bahnhof")},
         QStringLiteral("S-Bahn & Regionalzüge")},
        {{QStringLiteral("de:08117:3301"), QStringLiteral("Göppingen, Hochschule"), QStringLiteral("Hochschule Göppingen")},
         QStringLiteral("Campus Göppingen")},
        {{QStringLiteral("de:08117:154"), QStringLiteral("Göppingen, Bahnhof"), QStringLiteral("Göppingen Bahnhof")},
         QStringLiteral("Regionalzüge")},
    };
    return presets;
}

QVariantMap stopToVariant(const TransitStop &stop)
{
    QVariantMap m;
    m[QStringLiteral("id")] = stop.id;
    m[QStringLiteral("name")] = stop.name;
    m[QStringLiteral("shortName")] = stop.shortName;
    return m;
}

QVariantMap departureToVariant(const Departure &departure)
{
    QVariantList notices;
    for (const auto &notice : departure.notices) {
        QVariantMap n;
        n[QStringLiteral("title")] = notice.title;
        n[QStringLiteral("text")] = notice.text;
        notices.append(n);
    }
    QVariantMap m;
    m[QStringLiteral("line")] = departure.line;
    m[QStringLiteral("productClass")] = departure.productClass;
    m[QStringLiteral("destination")] = departure.destination;
    m[QStringLiteral("platform")] = departure.platform;
    m[QStringLiteral("plannedLabel")] = departure.planned.toLocalTime().toString(QStringLiteral("HH:mm"));
    m[QStringLiteral("delay")] = departure.estimated.isValid() ? int(departure.planned.secsTo(departure.estimated) / 60) : -1;
    m[QStringLiteral("cancelled")] = departure.cancelled;
    m[QStringLiteral("expectedMsecs")] = departure.expected().toMSecsSinceEpoch();
    m[QStringLiteral("notices")] = notices;
    return m;
}

} // namespace

TransitController::TransitController(SettingsStore *settings, TimetableController *timetable, QObject *parent)
    : QObject(parent)
    , m_settings(settings)
    , m_timetable(timetable)
    , m_repository(new TransitRepository(this))
{
    m_refreshTimer.setInterval(60 * 1000);
    connect(&m_refreshTimer, &QTimer::timeout, this, &TransitController::refresh);
    connect(m_settings, &SettingsStore::transitStopChanged, this, [this]() {
        Q_EMIT stopChanged();
        refresh();
    });
    // No stop picked yet: the campus stop follows the chosen Mensa.
    connect(m_settings, &SettingsStore::mensaLocationIdChanged, this, [this]() {
        if (!m_settings->transitStop()) {
            Q_EMIT stopChanged();
            refresh();
        }
    });
    // A week (re)load is when today's lectures can first become known.
    connect(m_timetable, &TimetableController::weekEventsChanged, this, &TransitController::boardChanged);
}

std::optional<TransitStop> TransitController::currentStop() const
{
    if (auto stop = m_settings->transitStop())
        return stop;
    switch (m_settings->mensaLocationId()) {
    case 6:
        return presetList().at(0).stop;
    case 9:
        return presetList().at(1).stop;
    case 13:
        return presetList().at(3).stop;
    default:
        return std::nullopt;
    }
}

QVariantList TransitController::presets() const
{
    QVariantList list;
    for (const auto &preset : presetList()) {
        QVariantMap m = stopToVariant(preset.stop);
        m[QStringLiteral("campus")] = preset.campus;
        list.append(m);
    }
    return list;
}

QVariantList TransitController::recentStops() const
{
    QVariantList list;
    for (const auto &stop : m_settings->recentTransitStops())
        list.append(stopToVariant(stop));
    return list;
}

QVariantMap TransitController::stop() const
{
    if (auto stop = currentStop())
        return stopToVariant(*stop);
    return {};
}

void TransitController::setActive(bool active)
{
    if (m_active == active)
        return;
    m_active = active;
    Q_EMIT activeChanged();
    if (m_active) {
        refresh();
        m_refreshTimer.start();
    } else {
        m_refreshTimer.stop();
    }
}

void TransitController::setAfterLecture(bool afterLecture)
{
    if (m_afterLecture == afterLecture)
        return;
    m_afterLecture = afterLecture;
    Q_EMIT afterLectureChanged();
    refresh();
}

QString TransitController::lectureEndLabel() const
{
    const int end = m_timetable->todaysLastLectureEndMinutes();
    if (end < 0 || end <= QTime::currentTime().msecsSinceStartOfDay() / 60000)
        return {};
    return QTime(end / 60, end % 60).toString(QStringLiteral("HH:mm"));
}

QDateTime TransitController::boardTime() const
{
    if (!m_afterLecture)
        return {};
    const QString label = lectureEndLabel();
    if (label.isEmpty())
        return {};
    return QDateTime(QDate::currentDate(), QTime::fromString(label, QStringLiteral("HH:mm")));
}

QString TransitController::updatedLabel() const
{
    return m_updatedAt.isValid() ? QStringLiteral("Stand %1").arg(m_updatedAt.toString(QStringLiteral("HH:mm"))) : QString();
}

void TransitController::setStop(const QString &id, const QString &name, const QString &shortName)
{
    const TransitStop stop{id, name, shortName};
    bool isPreset = false;
    for (const auto &preset : presetList())
        isPreset = isPreset || preset.stop.id == id;
    // SettingsStore's change signal triggers the refresh (see the constructor).
    m_settings->setTransitStop(stop, isPreset);
}

void TransitController::refresh()
{
    const auto stop = currentStop();
    if (!m_active || !stop)
        return;
    const QDateTime at = boardTime();
    const QString key = stop->id + QLatin1Char('|') + (at.isValid() ? at.toString(Qt::ISODate) : QString());
    // A different stop or time: drop the old board right away rather than showing it under the
    // new heading until the answer arrives.
    if (key != m_boardKey) {
        m_boardKey = key;
        m_departures.clear();
        m_updatedAt = {};
    }
    m_loading = true;
    m_errorMessage.clear();
    Q_EMIT boardChanged();
    m_repository->fetchDepartures(
        stop->id,
        at,
        [this, key](const QList<Departure> &departures) {
            if (key != m_boardKey)
                return;
            m_departures.clear();
            for (const auto &departure : departures)
                m_departures.append(departureToVariant(departure));
            m_loading = false;
            m_updatedAt = QDateTime::currentDateTime();
            Q_EMIT boardChanged();
        },
        [this, key](const QString &message) {
            if (key != m_boardKey)
                return;
            m_loading = false;
            m_errorMessage = message;
            Q_EMIT boardChanged();
        });
}

void TransitController::search(const QString &query)
{
    const QString trimmed = query.trimmed();
    m_searchError.clear();
    if (trimmed.size() < 2) {
        m_searchResults.clear();
        m_searching = false;
        Q_EMIT searchChanged();
        return;
    }
    m_searching = true;
    Q_EMIT searchChanged();
    m_repository->searchStops(
        trimmed,
        [this](const QList<TransitStop> &stops) {
            m_searchResults.clear();
            for (const auto &stop : stops)
                m_searchResults.append(stopToVariant(stop));
            m_searching = false;
            Q_EMIT searchChanged();
        },
        [this](const QString &message) {
            m_searchResults.clear();
            m_searching = false;
            m_searchError = message;
            Q_EMIT searchChanged();
        });
}

} // namespace stundenplan
