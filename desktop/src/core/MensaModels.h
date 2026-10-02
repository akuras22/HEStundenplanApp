#pragma once

#include <QList>
#include <QMetaType>
#include <QString>
#include <QStringList>
#include <optional>

namespace stundenplan {

/** One canteen of the Studierendenwerk Stuttgart, as listed in the "ORT" dropdown of its
 *  Speiseplan page. `id` is the site's own locId; the list is fixed rather than scraped, since
 *  the site's own labels come all-caps with broken umlauts. Ported from MensaModels.kt. */
struct MensaLocation {
    int id = 0;
    QString name;
    QString shortName;
    /** The three sites at Hochschule Esslingen campuses — listed first in every picker. */
    bool atHsEsslingen = false;
};

const QList<MensaLocation> &mensaLocations();
std::optional<MensaLocation> mensaLocationById(int id);

/** A marker icon on a meal ("vegan", "Schwein", …): `code` is the icon's file name on the site
 *  (VG, V, VR, S, R, RS, G, MSC, …), `title` its tooltip text. */
struct MensaLabel {
    QString code;
    QString title;

    bool isVeggie() const;
};

struct NutritionValue {
    QString label;
    QString value;
    bool isSubItem = false;
};

struct MensaMeal {
    QString category;
    QString subcategory;    // empty == null
    QString name;
    QString imageUrl;       // empty == no photo
    /** Studierende / Bedienstete / Gäste, as printed (e.g. "2,99") — usually all three. */
    QStringList prices;
    QList<MensaLabel> labels;
    /** Allergen and additive codes as printed in the meal's "(Ei, Sl, 3)" line. */
    QStringList additiveCodes;
    QString nutritionBasis; // e.g. "100 g"; empty == null
    QList<NutritionValue> nutrition;

    QStringList allergens() const;
    QStringList additives() const;
    /** Codes neither legend knows (the site added a new one) — shown raw rather than dropped. */
    QStringList unknownCodes() const;
};

/** One day's menu at one location. `closedMessage` is set (and `meals` empty) when the site has
 *  nothing for that day — weekends, holidays, or days not planned yet. */
struct MensaDay {
    QList<MensaMeal> meals;
    QString closedMessage;
};

} // namespace stundenplan

Q_DECLARE_METATYPE(stundenplan::MensaDay)
