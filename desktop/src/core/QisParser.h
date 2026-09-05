#pragma once

#include "Models.h"
#include <QList>
#include <QString>

namespace stundenplan {

/**
 * Parsing for HS Esslingen's QIS/LSF pages. The HTML is old-school table-layout markup with no
 * stable ids/classes beyond a handful of presentation classes ("plan1"/"plan2"/"notiz"/"klein"),
 * so parsing leans on structural selectors and regexes rather than ids — ported 1:1 in structure
 * from the original Kotlin/Jsoup implementation. KDE conventions build with -fno-exceptions, so
 * (unlike the Kotlin original) failure is reported via `errorMessage` rather than a thrown error.
 */
namespace QisParser {

/** Parses the public "Studiengangpläne (Liste)" catalog page into selectable entries. */
QList<Studiengang> parseStudiengangList(const QString &html);

/** Parses a "wplan" weekly timetable page (show=plan, P.vx=lang) into individual events. Returns
 *  an empty list and sets `*errorMessage` if the page doesn't have the expected timetable table. */
QList<TimetableEvent> parseTimetable(const QString &html, QString *errorMessage = nullptr);

} // namespace QisParser

} // namespace stundenplan
