#include "MensaModels.h"

#include <QHash>

namespace stundenplan {

namespace {

// The site's own legend ("FILTER ALLERGENE | ZUSATZSTOFFE"), which only lives on its main page —
// the per-day responses carry just the codes.
const QHash<QString, QString> &allergenNames()
{
    static const QHash<QString, QString> names = {
        {QStringLiteral("Ei"), QStringLiteral("Ei")},
        {QStringLiteral("En"), QStringLiteral("Erdnuss")},
        {QStringLiteral("Fi"), QStringLiteral("Fisch")},
        {QStringLiteral("GlD"), QStringLiteral("Dinkel")},
        {QStringLiteral("GlG"), QStringLiteral("Gerste")},
        {QStringLiteral("GlH"), QStringLiteral("Hafer")},
        {QStringLiteral("GlKW"), QStringLiteral("Khorasan-Weizen")},
        {QStringLiteral("GlR"), QStringLiteral("Roggen")},
        {QStringLiteral("GlW"), QStringLiteral("Weizen")},
        {QStringLiteral("Kr"), QStringLiteral("Krebstiere")},
        {QStringLiteral("La"), QStringLiteral("Milch und Laktose")},
        {QStringLiteral("Lu"), QStringLiteral("Lupine")},
        {QStringLiteral("NuC"), QStringLiteral("Cashewnüsse")},
        {QStringLiteral("NuH"), QStringLiteral("Haselnüsse")},
        {QStringLiteral("NuM"), QStringLiteral("Mandeln")},
        {QStringLiteral("NuMa"), QStringLiteral("Macadamianüsse")},
        {QStringLiteral("NuPa"), QStringLiteral("Paranüsse")},
        {QStringLiteral("NuPe"), QStringLiteral("Pecannüsse")},
        {QStringLiteral("NuPi"), QStringLiteral("Pistazien")},
        {QStringLiteral("NuW"), QStringLiteral("Walnüsse")},
        {QStringLiteral("Se"), QStringLiteral("Sesam")},
        {QStringLiteral("Sf"), QStringLiteral("Senf")},
        {QStringLiteral("Sl"), QStringLiteral("Sellerie")},
        {QStringLiteral("So"), QStringLiteral("Soja")},
        {QStringLiteral("Sw"), QStringLiteral("Schwefeldioxid und Sulfite")},
        {QStringLiteral("Wt"), QStringLiteral("Weichtiere")},
    };
    return names;
}

const QHash<QString, QString> &additiveNames()
{
    static const QHash<QString, QString> names = {
        {QStringLiteral("1"), QStringLiteral("mit Konservierungsstoff")},
        {QStringLiteral("2"), QStringLiteral("mit Farbstoff")},
        {QStringLiteral("3"), QStringLiteral("mit Antioxidationsmittel")},
        {QStringLiteral("4"), QStringLiteral("mit Geschmacksverstärker")},
        {QStringLiteral("5"), QStringLiteral("geschwefelt")},
        {QStringLiteral("6"), QStringLiteral("gewachst")},
        {QStringLiteral("7"), QStringLiteral("mit Phosphat")},
        {QStringLiteral("8"), QStringLiteral("mit Süßungsmittel")},
        {QStringLiteral("9"), QStringLiteral("enthält eine Phenylalaninquelle")},
        {QStringLiteral("10"), QStringLiteral("geschwärzt")},
        {QStringLiteral("11"), QStringLiteral("mit Alkohol")},
        {QStringLiteral("12"), QStringLiteral("mit Nitritpökelsalz")},
        {QStringLiteral("13"), QStringLiteral("mit Nitrat")},
        {QStringLiteral("14"), QStringLiteral("kann Aktivität und Aufmerksamkeit bei Kindern beeinträchtigen")},
        {QStringLiteral("30"), QStringLiteral("aus Fleischstücken zusammengefügt")},
        {QStringLiteral("31"), QStringLiteral("aus Fischstücken zusammengefügt")},
        {QStringLiteral("GeR"), QStringLiteral("enthält Rindergelatine")},
        {QStringLiteral("Ges"), QStringLiteral("enthält Schweinegelatine")},
    };
    return names;
}

QStringList lookUp(const QStringList &codes, const QHash<QString, QString> &names)
{
    QStringList result;
    for (const auto &code : codes) {
        const auto it = names.constFind(code);
        if (it != names.constEnd())
            result.append(it.value());
    }
    return result;
}

} // namespace

const QList<MensaLocation> &mensaLocations()
{
    static const QList<MensaLocation> locations = {
        {6, QStringLiteral("Mensa Esslingen Flandernstraße"), QStringLiteral("Flandernstraße"), true},
        {9, QStringLiteral("Mensa Esslingen Stadtmitte"), QStringLiteral("Stadtmitte"), true},
        {13, QStringLiteral("Essensausgabe Göppingen"), QStringLiteral("Göppingen"), true},
        {2, QStringLiteral("Mensa Stuttgart-Vaihingen"), QStringLiteral("Vaihingen"), false},
        {16, QStringLiteral("Mensa Central"), QStringLiteral("Central"), false},
        {4, QStringLiteral("Mensa HMDK"), QStringLiteral("HMDK"), false},
        {7, QStringLiteral("Mensa ABK"), QStringLiteral("ABK"), false},
        {1, QStringLiteral("Mensa Ludwigsburg"), QStringLiteral("Ludwigsburg"), false},
        {12, QStringLiteral("Mensa Horb"), QStringLiteral("Horb"), false},
    };
    return locations;
}

std::optional<MensaLocation> mensaLocationById(int id)
{
    for (const auto &location : mensaLocations()) {
        if (location.id == id)
            return location;
    }
    return std::nullopt;
}

bool MensaLabel::isVeggie() const
{
    return code == QLatin1String("VG") || code == QLatin1String("V") || code == QLatin1String("VR");
}

QStringList MensaMeal::allergens() const
{
    return lookUp(additiveCodes, allergenNames());
}

QStringList MensaMeal::additives() const
{
    return lookUp(additiveCodes, additiveNames());
}

QStringList MensaMeal::unknownCodes() const
{
    QStringList result;
    for (const auto &code : additiveCodes) {
        if (!allergenNames().contains(code) && !additiveNames().contains(code))
            result.append(code);
    }
    return result;
}

} // namespace stundenplan
