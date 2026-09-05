#pragma once

#include "Models.h"
#include <QDate>
#include <QList>
#include <QNetworkAccessManager>
#include <QObject>

namespace stundenplan {

/**
 * Talks directly to HS Esslingen's public QIS/LSF pages. Nothing is cached to disk here: every
 * call hits the website live, matching what a browser would show at that moment (see
 * TimetableCache for the separate offline-fallback layer). Ported from QisRepository.kt.
 */
class QisRepository : public QObject
{
    Q_OBJECT
public:
    explicit QisRepository(QObject *parent = nullptr);

    /** Fetches the full public catalog of Studiengang timetables (all faculties/semesters). */
    void fetchStudiengaenge();

    /** Fetches the real timetable for the week containing `weekMonday`, live. */
    void fetchTimetable(const Studiengang &studiengang, const QDate &weekMonday);

Q_SIGNALS:
    void studiengaengeFetched(const QList<stundenplan::Studiengang> &studiengaenge);
    void timetableFetched(const QList<stundenplan::TimetableEvent> &events);
    /** `forTimetable` distinguishes which of the two requests above failed. */
    void fetchFailed(const QString &message, bool forTimetable);

private:
    void execute(const QUrl &url, const std::function<void(const QString &)> &onSuccess, bool forTimetable);

    QNetworkAccessManager *m_manager;
};

} // namespace stundenplan
