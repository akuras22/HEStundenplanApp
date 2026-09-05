#include "QisParser.h"

#include "HtmlDocument.h"

#include <QRegularExpression>
#include <QSet>
#include <algorithm>

namespace stundenplan::QisParser {

namespace {

const QRegularExpression kParallelIdRe(QStringLiteral("k_parallel\\.parallelid=(\\d+)"));
const QRegularExpression kAbstgvnrRe(QStringLiteral("k_abstgv\\.abstgvnr=(\\d+)"));
const QRegularExpression kTimeRe(
    QStringLiteral("(\\d{1,2}:\\d{2})\\s*-\\s*(\\d{1,2}:\\d{2})\\s*(?:\\(([^)]+)\\))?"));
const QRegularExpression kPlan2Re(QStringLiteral("^plan2+$"));

int parseTimeToMinutes(const QString &hhmm)
{
    const auto parts = hhmm.split(QLatin1Char(':'));
    return parts[0].trimmed().toInt() * 60 + parts[1].trimmed().toInt();
}

/** table.select("tbody tr") — rows nested (at any depth) inside a <tbody>, with a fallback to
 *  any descendant <tr> for pages whose HTML omits an explicit <tbody>. */
QList<HtmlElement> tbodyDescendantRows(const HtmlElement &table)
{
    QList<HtmlElement> result;
    for (const auto &tbody : table.selectDescendants(QStringLiteral("tbody")))
        result += tbody.selectDescendants(QStringLiteral("tr"));
    if (result.isEmpty())
        result = table.selectDescendants(QStringLiteral("tr"));
    return result;
}

/** table.select("> tbody > tr, > tr") falling back to table.select("tr") — used for grid
 *  resolution, which needs direct rows so nested tables (event blocks) aren't double-counted. */
QList<HtmlElement> directRowsOrFallback(const HtmlElement &table)
{
    QList<HtmlElement> direct;
    for (const auto &tbody : table.selectDirectChildren(QStringLiteral("tbody")))
        direct += tbody.selectDirectChildren(QStringLiteral("tr"));
    direct += table.selectDirectChildren(QStringLiteral("tr"));
    if (!direct.isEmpty())
        return direct;
    return table.selectDescendants(QStringLiteral("tr"));
}

struct GridCell {
    int row = 0;
    int col = 0;
    HtmlElement element;
};

QList<GridCell> resolveGrid(const HtmlElement &table)
{
    const auto rows = directRowsOrFallback(table);
    QHash<int, int> occupiedUntilRow;
    QList<GridCell> result;

    for (int r = 0; r < rows.size(); ++r) {
        int c = 0;
        for (const auto &cell : rows[r].children()) {
            const QString tag = cell.tagName();
            if (tag != QStringLiteral("td") && tag != QStringLiteral("th"))
                continue;
            while (occupiedUntilRow.value(c, -1) >= r)
                ++c;
            bool ok = false;
            int colspan = cell.attr(QStringLiteral("colspan")).toInt(&ok);
            if (!ok || colspan < 1)
                colspan = 1;
            int rowspan = cell.attr(QStringLiteral("rowspan")).toInt(&ok);
            if (!ok || rowspan < 1)
                rowspan = 1;
            result.append(GridCell{r, c, cell});
            for (int cc = c; cc < c + colspan; ++cc)
                occupiedUntilRow[cc] = r + rowspan - 1;
            c += colspan;
        }
    }
    return result;
}

std::optional<TimetableEvent> parseEventTable(const HtmlElement &eventTable, Weekday day)
{
    QString title;
    if (auto link = eventTable.firstDescendant(QStringLiteral("a")); link.isValid()) {
        // Only accept a link if it sits inside a td.klein — mirrors selectFirst("td.klein a").
        for (const auto &kleinTd : eventTable.selectDescendantsByClass(QStringLiteral("td"), QStringLiteral("klein"))) {
            if (auto a = kleinTd.firstDescendant(QStringLiteral("a")); a.isValid()) {
                title = a.text().trimmed();
                break;
            }
        }
    }
    if (title.isEmpty()) {
        const auto kleinTds = eventTable.selectDescendantsByClass(QStringLiteral("td"), QStringLiteral("klein"));
        if (!kleinTds.isEmpty())
            title = kleinTds.first().text().trimmed();
    }
    if (title.isEmpty())
        return std::nullopt;

    std::optional<int> startMinutes;
    std::optional<int> endMinutes;
    QString frequency, startDate, endDate, room, category, lecturer;

    for (const auto &notiz : eventTable.selectDescendantsByClass(QStringLiteral("td"), QStringLiteral("notiz"))) {
        const QString text = notiz.text().trimmed();
        if (text.isEmpty())
            continue;

        if (!startMinutes.has_value()) {
            auto match = kTimeRe.match(text);
            if (match.hasMatch()) {
                startMinutes = parseTimeToMinutes(match.captured(1));
                endMinutes = parseTimeToMinutes(match.captured(2));
                const QString freq = match.captured(3).trimmed();
                if (!freq.isEmpty())
                    frequency = freq;
                continue;
            }
        }
        if (text.startsWith(QStringLiteral("Start:"), Qt::CaseInsensitive)) {
            const QString value = text.mid(text.indexOf(QLatin1Char(':')) + 1).trimmed();
            if (!value.isEmpty())
                startDate = value;
            continue;
        }
        if (text.startsWith(QStringLiteral("Ende:"), Qt::CaseInsensitive)) {
            const QString value = text.mid(text.indexOf(QLatin1Char(':')) + 1).trimmed();
            if (!value.isEmpty())
                endDate = value;
            continue;
        }
        if (text.startsWith(QStringLiteral("Dozent"), Qt::CaseInsensitive)) {
            const QString value = text.mid(text.indexOf(QLatin1Char(':')) + 1).trimmed();
            if (!value.isEmpty())
                lecturer = value;
            continue;
        }
        if (text.startsWith(QStringLiteral("Einrichtung"), Qt::CaseInsensitive)) {
            continue;
        }
        auto roomLink = notiz.firstDescendant(QStringLiteral("a"));
        if (roomLink.isValid() && room.isEmpty()) {
            const QString linkText = roomLink.text().trimmed();
            room = linkText.isEmpty() ? QString() : linkText;
            QString rest = text;
            if (rest.startsWith(linkText))
                rest = rest.mid(linkText.length());
            rest = rest.trimmed();
            while (!rest.isEmpty() && (rest.front() == QLatin1Char(':') || rest.front() == QLatin1Char(' ')))
                rest.removeFirst();
            while (!rest.isEmpty() && (rest.back() == QLatin1Char(':') || rest.back() == QLatin1Char(' ')))
                rest.removeLast();
            if (!rest.isEmpty())
                category = rest;
            continue;
        }
    }

    if (!startMinutes.has_value() || !endMinutes.has_value())
        return std::nullopt;

    TimetableEvent event;
    event.day = day;
    event.title = title;
    event.startMinutes = *startMinutes;
    event.endMinutes = *endMinutes;
    event.frequency = frequency;
    event.room = room;
    event.lecturer = lecturer;
    event.category = category;
    event.startDate = startDate;
    event.endDate = endDate;
    return event;
}

} // namespace

QList<Studiengang> parseStudiengangList(const QString &html)
{
    HtmlDocument doc(html);
    if (!doc.isValid())
        return {};

    HtmlElement header;
    for (const auto &th : doc.root().selectDescendants(QStringLiteral("th"))) {
        if (th.text().contains(QStringLiteral("Studiengänge"))) {
            header = th;
            break;
        }
    }
    if (!header.isValid())
        return {};

    HtmlElement table = header.closest(QStringLiteral("table"));
    if (!table.isValid())
        return {};

    QList<Studiengang> result;
    QSet<QString> seenIds;
    for (const auto &row : tbodyDescendantRows(table)) {
        const auto cells = row.selectDirectChildren(QStringLiteral("td"));
        if (cells.size() < 2)
            continue;
        const auto codeLink = cells[0].firstDescendant(QStringLiteral("a"));
        const QString code = codeLink.isValid() ? codeLink.text().trimmed() : QString();
        if (code.isEmpty())
            continue;
        const auto planLink = cells[1].firstDescendant(QStringLiteral("a"));
        if (!planLink.isValid())
            continue;
        const QString href = planLink.attr(QStringLiteral("href"));
        const auto abstgvnrMatch = kAbstgvnrRe.match(href);
        const auto parallelIdMatch = kParallelIdRe.match(href);
        if (!abstgvnrMatch.hasMatch() || !parallelIdMatch.hasMatch())
            continue;

        Studiengang s;
        s.code = code;
        s.abstgvnr = abstgvnrMatch.captured(1);
        s.parallelid = parallelIdMatch.captured(1);
        if (seenIds.contains(s.id()))
            continue;
        seenIds.insert(s.id());
        result.append(s);
    }

    std::sort(result.begin(), result.end(), [](const Studiengang &a, const Studiengang &b) {
        return a.code < b.code;
    });
    return result;
}

QList<TimetableEvent> parseTimetable(const QString &html, QString *errorMessage)
{
    static const QString kFormatError =
        QStringLiteral("Stundenplan-Tabelle wurde auf der Seite nicht gefunden (unerwartetes Seitenformat).");

    HtmlDocument doc(html);
    if (!doc.isValid()) {
        if (errorMessage)
            *errorMessage = kFormatError;
        return {};
    }

    HtmlElement montagHeader;
    for (const auto &th : doc.root().selectDescendants(QStringLiteral("th"))) {
        if (th.text().contains(QStringLiteral("Montag"))) {
            montagHeader = th;
            break;
        }
    }
    if (!montagHeader.isValid()) {
        if (errorMessage)
            *errorMessage = kFormatError;
        return {};
    }
    HtmlElement table = montagHeader.closest(QStringLiteral("table"));
    if (!table.isValid()) {
        if (errorMessage)
            *errorMessage = kFormatError;
        return {};
    }

    const auto grid = resolveGrid(table);

    QHash<int, Weekday> columnToDay{
        {2, Weekday::Monday},
        {3, Weekday::Tuesday},
        {4, Weekday::Wednesday},
        {5, Weekday::Thursday},
        {6, Weekday::Friday},
    };

    QList<TimetableEvent> events;
    for (const auto &cell : grid) {
        // Day cells are classed "plan2", or "plan22"/"plan222"/... for taller (rowspan-bumped)
        // cells; legend/axis cells never match this.
        if (!kPlan2Re.match(cell.element.className()).hasMatch())
            continue;
        if (!columnToDay.contains(cell.col))
            continue;
        const Weekday day = columnToDay.value(cell.col);
        for (const auto &eventTable : cell.element.selectDirectChildren(QStringLiteral("table"))) {
            if (auto event = parseEventTable(eventTable, day))
                events.append(*event);
        }
    }

    std::sort(events.begin(), events.end(), [](const TimetableEvent &a, const TimetableEvent &b) {
        if (a.day != b.day)
            return static_cast<int>(a.day) < static_cast<int>(b.day);
        return a.startMinutes < b.startMinutes;
    });

    // distinct() — drop exact duplicates while preserving order.
    QList<TimetableEvent> distinct;
    for (const auto &event : events) {
        if (!distinct.contains(event))
            distinct.append(event);
    }
    return distinct;
}

} // namespace stundenplan::QisParser
