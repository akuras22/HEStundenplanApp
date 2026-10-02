package de.hsesslingen.stundenplan.data

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

/**
 * Parses one day's menu as returned by the Studierendenwerk's Speiseplan backend (the HTML
 * fragment its page injects into `#speiseplan`). The fragment is a flat run of Bootstrap rows in
 * display order: a `gruppenkopf` row per category, optional `untergruppenkopf` rows below it, then
 * one `splMeal` row per dish — or a single `nodata` row on closed days.
 */
object MensaParser {

    const val BASE_URL = "https://sws2.maxmanager.xyz/"
    const val DEFAULT_CLOSED_MESSAGE = "Für diesen Tag gibt es keinen Speiseplan."

    private val PRICE_RE = Regex("""€\s*(\d+,\d{2}(?:\s*/\s*\d+,\d{2})*)""")
    private val ICON_CODE_RE = Regex("""/([^/?]+)\.png""")
    // "MSC-zertifizierter Fisch (MSC-C-51632)" — the certificate number means nothing on a label.
    private val TRAILING_PARENS_RE = Regex("""\s*\([^)]*\)\s*$""")

    fun parseDay(html: String): MensaDay {
        val doc = Jsoup.parse(html, BASE_URL)
        val meals = mutableListOf<MensaMeal>()
        var category: String? = null
        var subcategory: String? = null
        var noDataText: String? = null
        for (row in doc.select("div.row")) {
            when {
                row.hasClass("gruppenkopf") -> {
                    category = row.selectFirst(".gruppenname")?.text()?.let(::titleCase)
                    subcategory = null
                }
                row.hasClass("untergruppenkopf") ->
                    subcategory = row.selectFirst(".gruppenname")?.text()?.let(::titleCase)
                row.hasClass("splMeal") ->
                    parseMeal(row, category.orEmpty(), subcategory)?.let(meals::add)
                row.hasClass("nodata") ->
                    noDataText = row.text().trim().ifBlank { null }
            }
        }
        return MensaDay(
            meals = meals,
            closedMessage = if (meals.isEmpty()) noDataText ?: DEFAULT_CLOSED_MESSAGE else null,
        )
    }

    private fun parseMeal(row: Element, category: String, subcategory: String?): MensaMeal? {
        // Each meal is rendered twice (phone and desktop layout); the first span is the phone
        // layout's title, the cleanest copy of the name.
        val name = row.selectFirst("span")?.text()?.trim().orEmpty()
        if (name.isEmpty()) return null

        val imageUrl = row.selectFirst("img.smallFoto")?.absUrl("src")?.ifBlank { null }

        val prices = PRICE_RE.find(row.text().replace(' ', ' '))
            ?.groupValues?.get(1)
            ?.split("/")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()

        // iconLarge is the desktop layout's copy; the phone layout repeats them as plain "icon".
        val labels = row.select("img.iconLarge").mapNotNull { img ->
            val code = ICON_CODE_RE.find(img.attr("src"))?.groupValues?.get(1) ?: return@mapNotNull null
            val title = img.attr("title").replace(TRAILING_PARENS_RE, "").trim().ifBlank { code }
            MensaLabel(code = code, title = title)
        }

        val azn = row.selectFirst("div.azn")
        val additiveCodes = azn?.ownText()
            ?.trim()
            ?.removePrefix("(")?.removeSuffix(")")
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()

        val nutritionLines = azn?.select("div")
            ?.firstOrNull { it.ownText().trim().startsWith("Nährwerte") }
            ?.let(::textLines)
            .orEmpty()
        val nutritionBasis = nutritionLines.firstOrNull()?.removePrefix("Nährwerte pro")?.trim()?.ifBlank { null }
        val nutrition = nutritionLines.drop(1).mapNotNull { line ->
            val sep = line.indexOf(':')
            if (sep <= 0) return@mapNotNull null
            val rawLabel = line.substring(0, sep).trim()
            val value = line.substring(sep + 1).trim()
            if (value.isEmpty()) return@mapNotNull null
            NutritionValue(
                label = rawLabel.removePrefix("-").trim(),
                value = value,
                isSubItem = rawLabel.startsWith("-"),
            )
        }

        return MensaMeal(
            category = category,
            subcategory = subcategory,
            name = name,
            imageUrl = imageUrl,
            prices = prices,
            labels = labels,
            additiveCodes = additiveCodes,
            nutritionBasis = nutritionBasis,
            nutrition = nutrition,
        )
    }

    /** Text of [element] split at its `<br>`s ("Nährwerte pro 100 g", "Brennwert: …", …). */
    private fun textLines(element: Element): List<String> {
        val lines = mutableListOf(StringBuilder())
        for (node in element.childNodes()) {
            when {
                node is TextNode -> lines.last().append(node.text())
                node is Element && node.normalName() == "br" -> lines += StringBuilder()
                node is Element -> lines.last().append(' ').append(node.text())
            }
        }
        return lines.map { it.toString().replace(' ', ' ').replace(Regex("""\s+"""), " ").trim() }
            .filter { it.isNotEmpty() }
    }

    /** "VEGANER RENNER" -> "Veganer Renner" — the site prints every category in caps. */
    private fun titleCase(raw: String): String =
        raw.trim().lowercase().split(" ").filter { it.isNotEmpty() }.joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercase() }
        }
}
