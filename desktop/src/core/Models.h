#pragma once

#include <QDate>
#include <QMetaType>
#include <QString>
#include <optional>

namespace stundenplan {

enum class Weekday {
    Monday,
    Tuesday,
    Wednesday,
    Thursday,
    Friday,
};

QString weekdayGermanLabel(Weekday day);
int weekdayQtDayOfWeek(Weekday day); // Qt::Monday == 1 .. Qt::Friday == 5
std::optional<Weekday> weekdayFromDate(const QDate &date);

struct Studiengang {
    QString code;
    QString abstgvnr;
    QString parallelid;

    QString id() const { return abstgvnr + QLatin1Char('-') + parallelid; }

    bool operator==(const Studiengang &other) const
    {
        return code == other.code && abstgvnr == other.abstgvnr && parallelid == other.parallelid;
    }
};

struct TimetableEvent {
    Weekday day = Weekday::Monday;
    QString title;
    int startMinutes = 0;
    int endMinutes = 0;
    QString frequency;   // empty == null
    QString room;        // empty == null
    QString lecturer;     // empty == null
    QString category;     // empty == null
    QString startDate;    // empty == null
    QString endDate;      // empty == null

    QString startLabel() const;
    QString endLabel() const;
    bool appliesOn(const QDate &date) const;

    /** Stable identity for "this recurring group" across weeks (see Models.kt groupKey). */
    QString groupKey() const;

    bool operator==(const TimetableEvent &other) const
    {
        return day == other.day && title == other.title && startMinutes == other.startMinutes
            && endMinutes == other.endMinutes && frequency == other.frequency && room == other.room
            && lecturer == other.lecturer && category == other.category && startDate == other.startDate
            && endDate == other.endDate;
    }
};

} // namespace stundenplan

Q_DECLARE_METATYPE(stundenplan::Studiengang)
Q_DECLARE_METATYPE(stundenplan::TimetableEvent)
