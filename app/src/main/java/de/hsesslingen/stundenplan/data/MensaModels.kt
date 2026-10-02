package de.hsesslingen.stundenplan.data

/** One canteen of the Studierendenwerk Stuttgart, as listed in the "ORT" dropdown of its
 *  Speiseplan page. [id] is the site's own `locId`; the list is fixed rather than scraped, since
 *  the site's own labels come all-caps with broken umlauts ("ESSENSAUSGABE GöPPINGEN"). */
data class MensaLocation(
    val id: Int,
    val name: String,
    val shortName: String,
    /** The three sites at Hochschule Esslingen campuses — listed first in every picker. */
    val atHsEsslingen: Boolean,
)

object MensaLocations {
    val all: List<MensaLocation> = listOf(
        MensaLocation(6, "Mensa Esslingen Flandernstraße", "Flandernstraße", atHsEsslingen = true),
        MensaLocation(9, "Mensa Esslingen Stadtmitte", "Stadtmitte", atHsEsslingen = true),
        MensaLocation(13, "Essensausgabe Göppingen", "Göppingen", atHsEsslingen = true),
        MensaLocation(2, "Mensa Stuttgart-Vaihingen", "Vaihingen", atHsEsslingen = false),
        MensaLocation(16, "Mensa Central", "Central", atHsEsslingen = false),
        MensaLocation(4, "Mensa HMDK", "HMDK", atHsEsslingen = false),
        MensaLocation(7, "Mensa ABK", "ABK", atHsEsslingen = false),
        MensaLocation(1, "Mensa Ludwigsburg", "Ludwigsburg", atHsEsslingen = false),
        MensaLocation(12, "Mensa Horb", "Horb", atHsEsslingen = false),
    )

    fun byId(id: Int): MensaLocation? = all.firstOrNull { it.id == id }
}

/** A marker icon on a meal ("vegan", "Schwein", …) — [code] is the icon's file name on the site
 *  (VG, V, VR, S, R, RS, G, MSC, …), [title] its tooltip text. */
data class MensaLabel(val code: String, val title: String) {
    val isVeggie: Boolean get() = code == "VG" || code == "V" || code == "VR"
}

data class NutritionValue(val label: String, val value: String, val isSubItem: Boolean)

data class MensaMeal(
    val category: String,
    val subcategory: String?,
    val name: String,
    val imageUrl: String?,
    /** Studierende / Bedienstete / Gäste, as printed (e.g. "2,99") — usually all three. */
    val prices: List<String>,
    val labels: List<MensaLabel>,
    /** Allergen and additive codes as printed in the meal's "(Ei, Sl, 3)" line. */
    val additiveCodes: List<String>,
    /** What [nutrition] refers to, e.g. "100 g". */
    val nutritionBasis: String?,
    val nutrition: List<NutritionValue>,
) {
    val allergens: List<String> get() = additiveCodes.filter { it in ALLERGENS }.map { ALLERGENS.getValue(it) }
    val additives: List<String> get() = additiveCodes.filter { it in ADDITIVES }.map { ADDITIVES.getValue(it) }
    /** Codes neither legend knows (the site added a new one) — shown raw rather than dropped. */
    val unknownCodes: List<String> get() = additiveCodes.filter { it !in ALLERGENS && it !in ADDITIVES }

    companion object {
        // The site's own legend ("FILTER ALLERGENE | ZUSATZSTOFFE"), which only lives on the main
        // page — the per-day responses carry just the codes.
        val ALLERGENS: Map<String, String> = linkedMapOf(
            "Ei" to "Ei",
            "En" to "Erdnuss",
            "Fi" to "Fisch",
            "GlD" to "Dinkel",
            "GlG" to "Gerste",
            "GlH" to "Hafer",
            "GlKW" to "Khorasan-Weizen",
            "GlR" to "Roggen",
            "GlW" to "Weizen",
            "Kr" to "Krebstiere",
            "La" to "Milch und Laktose",
            "Lu" to "Lupine",
            "NuC" to "Cashewnüsse",
            "NuH" to "Haselnüsse",
            "NuM" to "Mandeln",
            "NuMa" to "Macadamianüsse",
            "NuPa" to "Paranüsse",
            "NuPe" to "Pecannüsse",
            "NuPi" to "Pistazien",
            "NuW" to "Walnüsse",
            "Se" to "Sesam",
            "Sf" to "Senf",
            "Sl" to "Sellerie",
            "So" to "Soja",
            "Sw" to "Schwefeldioxid und Sulfite",
            "Wt" to "Weichtiere",
        )
        val ADDITIVES: Map<String, String> = linkedMapOf(
            "1" to "mit Konservierungsstoff",
            "2" to "mit Farbstoff",
            "3" to "mit Antioxidationsmittel",
            "4" to "mit Geschmacksverstärker",
            "5" to "geschwefelt",
            "6" to "gewachst",
            "7" to "mit Phosphat",
            "8" to "mit Süßungsmittel",
            "9" to "enthält eine Phenylalaninquelle",
            "10" to "geschwärzt",
            "11" to "mit Alkohol",
            "12" to "mit Nitritpökelsalz",
            "13" to "mit Nitrat",
            "14" to "kann Aktivität und Aufmerksamkeit bei Kindern beeinträchtigen",
            "30" to "aus Fleischstücken zusammengefügt",
            "31" to "aus Fischstücken zusammengefügt",
            "GeR" to "enthält Rindergelatine",
            "Ges" to "enthält Schweinegelatine",
        )
    }
}

/** One day's menu at one location. [closedMessage] is set (and [meals] empty) when the site has
 *  nothing for that day — weekends, holidays, or days not planned yet. */
data class MensaDay(
    val meals: List<MensaMeal>,
    val closedMessage: String?,
)
