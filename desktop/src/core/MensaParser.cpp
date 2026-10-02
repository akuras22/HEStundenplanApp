#include "MensaParser.h"

#include "HtmlDocument.h"

#include <QRegularExpression>
#include <QUrl>

namespace stundenplan {

namespace {

const QString kDefaultClosedMessage = QStringLiteral("Für diesen Tag gibt es keinen Speiseplan.");

bool hasClass(const HtmlElement &element, const QString &wanted)
{
    const auto tokens = element.className().split(QLatin1Char(' '), Qt::SkipEmptyParts);
    return tokens.contains(wanted);
}

HtmlElement firstByClass(const HtmlElement &element, const QString &tag, const QString &className)
{
    const auto all = element.selectDescendantsByClass(tag, className);
    return all.isEmpty() ? HtmlElement() : all.first();
}

/** "VEGANER RENNER" -> "Veganer Renner" — the site prints every category in caps. */
QString titleCase(const QString &raw)
{
    QStringList words = raw.trimmed().toLower().split(QLatin1Char(' '), Qt::SkipEmptyParts);
    for (auto &word : words)
        word[0] = word[0].toUpper();
    return words.join(QLatin1Char(' '));
}

QString withoutNbsp(QString text)
{
    return text.replace(QChar(0x00A0), QLatin1Char(' '));
}

std::optional<MensaMeal> parseMeal(const HtmlElement &row, const QString &category, const QString &subcategory)
{
    MensaMeal meal;
    meal.category = category;
    meal.subcategory = subcategory;

    // Each meal is rendered twice (phone and desktop layout); the first span is the phone
    // layout's title, the cleanest copy of the name.
    meal.name = row.firstDescendant(QStringLiteral("span")).text().trimmed();
    if (meal.name.isEmpty())
        return std::nullopt;

    const HtmlElement photo = firstByClass(row, QStringLiteral("img"), QStringLiteral("smallFoto"));
    const QString src = photo.attr(QStringLiteral("src")).trimmed();
    if (!src.isEmpty())
        meal.imageUrl = QUrl(QString::fromLatin1(MensaParser::kBaseUrl)).resolved(QUrl(src)).toString();

    static const QRegularExpression priceRe(QStringLiteral("€\\s*(\\d+,\\d{2}(?:\\s*/\\s*\\d+,\\d{2})*)"));
    const auto priceMatch = priceRe.match(withoutNbsp(row.text()));
    if (priceMatch.hasMatch()) {
        for (const auto &price : priceMatch.captured(1).split(QLatin1Char('/'), Qt::SkipEmptyParts)) {
            if (!price.trimmed().isEmpty())
                meal.prices.append(price.trimmed());
        }
    }

    // iconLarge is the desktop layout's copy; the phone layout repeats them as plain "icon".
    static const QRegularExpression iconCodeRe(QStringLiteral("/([^/?]+)\\.png"));
    // "MSC-zertifizierter Fisch (MSC-C-51632)" — the certificate number means nothing on a label.
    static const QRegularExpression trailingParensRe(QStringLiteral("\\s*\\([^)]*\\)\\s*$"));
    for (const auto &icon : row.selectDescendantsByClass(QStringLiteral("img"), QStringLiteral("iconLarge"))) {
        const auto codeMatch = iconCodeRe.match(icon.attr(QStringLiteral("src")));
        if (!codeMatch.hasMatch())
            continue;
        MensaLabel label;
        label.code = codeMatch.captured(1);
        label.title = icon.attr(QStringLiteral("title")).remove(trailingParensRe).trimmed();
        if (label.title.isEmpty())
            label.title = label.code;
        meal.labels.append(label);
    }

    const HtmlElement azn = firstByClass(row, QStringLiteral("div"), QStringLiteral("azn"));
    if (azn) {
        QString codes = azn.ownText().trimmed();
        if (codes.startsWith(QLatin1Char('(')))
            codes.remove(0, 1);
        if (codes.endsWith(QLatin1Char(')')))
            codes.chop(1);
        for (const auto &code : codes.split(QLatin1Char(','), Qt::SkipEmptyParts)) {
            if (!code.trimmed().isEmpty())
                meal.additiveCodes.append(code.trimmed());
        }

        for (const auto &div : azn.selectDescendants(QStringLiteral("div"))) {
            if (!div.ownText().trimmed().startsWith(QStringLiteral("Nährwerte")))
                continue;
            const QStringList lines = div.textLines();
            if (!lines.isEmpty()) {
                QString basis = lines.first();
                basis.remove(QStringLiteral("Nährwerte pro"));
                meal.nutritionBasis = basis.trimmed();
            }
            for (qsizetype i = 1; i < lines.size(); ++i) {
                const QString &line = lines.at(i);
                const qsizetype sep = line.indexOf(QLatin1Char(':'));
                if (sep <= 0)
                    continue;
                const QString rawLabel = line.left(sep).trimmed();
                const QString value = line.mid(sep + 1).trimmed();
                if (value.isEmpty())
                    continue;
                NutritionValue nutrition;
                nutrition.isSubItem = rawLabel.startsWith(QLatin1Char('-'));
                nutrition.label = nutrition.isSubItem ? rawLabel.mid(1).trimmed() : rawLabel;
                nutrition.value = value;
                meal.nutrition.append(nutrition);
            }
            break;
        }
    }

    return meal;
}

} // namespace

MensaDay MensaParser::parseDay(const QString &html)
{
    MensaDay day;
    HtmlDocument doc(html);
    QString category;
    QString subcategory;
    QString noDataText;

    if (doc.isValid()) {
        for (const auto &row : doc.root().selectDescendantsByClass(QStringLiteral("div"), QStringLiteral("row"))) {
            if (hasClass(row, QStringLiteral("gruppenkopf"))) {
                category = titleCase(firstByClass(row, QStringLiteral("div"), QStringLiteral("gruppenname")).text());
                subcategory.clear();
            } else if (hasClass(row, QStringLiteral("untergruppenkopf"))) {
                subcategory = titleCase(firstByClass(row, QStringLiteral("div"), QStringLiteral("gruppenname")).text());
            } else if (hasClass(row, QStringLiteral("splMeal"))) {
                if (auto meal = parseMeal(row, category, subcategory))
                    day.meals.append(*meal);
            } else if (hasClass(row, QStringLiteral("nodata"))) {
                noDataText = row.text().trimmed();
            }
        }
    }

    if (day.meals.isEmpty())
        day.closedMessage = noDataText.isEmpty() ? kDefaultClosedMessage : noDataText;
    return day;
}

} // namespace stundenplan
