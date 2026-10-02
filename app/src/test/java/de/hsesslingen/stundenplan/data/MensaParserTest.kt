package de.hsesslingen.stundenplan.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MensaParserTest {

    private fun resource(name: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(name)) { "missing resource $name" }
            .bufferedReader(Charsets.UTF_8)
            .readText()

    @Test
    fun `parses a day's meals with categories, prices, labels and photos`() {
        val day = MensaParser.parseDay(resource("mensa_day_sample.html"))

        assertNull(day.closedMessage)
        assertEquals(10, day.meals.size)
        assertEquals(
            listOf("Vorspeise", "Veganer Renner", "Hauptgericht", "Buffet", "Beilage", "Dessert"),
            day.meals.map { it.category }.distinct(),
        )

        val curry = day.meals.first { it.name == "Kichererbsen-Curry mit Duftreis" }
        assertEquals("Veganer Renner", curry.category)
        assertEquals(listOf("2,99", "5,99", "6,49"), curry.prices)
        assertEquals(listOf("VG", "VR"), curry.labels.map { it.code })
        assertEquals(listOf("vegan", "Veganer Renner"), curry.labels.map { it.title })
        assertEquals(
            "https://sws2.maxmanager.xyz/assets/fotos/musikhochschule/Speisefotos/8-9/n45rnr654_000915.jpg?v=1",
            curry.imageUrl,
        )
        assertEquals(listOf("Sl"), curry.additiveCodes)
        assertEquals(listOf("Sellerie"), curry.allergens)

        val gulasch = day.meals.first { it.name == "Schweinegulasch mit Paprika und Spätzle" }
        assertEquals(listOf("Ei", "GlW", "GlD"), gulasch.additiveCodes)
        assertEquals(listOf("Ei", "Weizen", "Dinkel"), gulasch.allergens)
        assertEquals("100 g", gulasch.nutritionBasis)
        assertEquals(NutritionValue("Brennwert", "517.4 kj / 124.0 kcal", isSubItem = false), gulasch.nutrition.first())
        assertTrue(gulasch.nutrition.any { it.label == "davon ges. FS" && it.isSubItem })
        assertTrue(gulasch.nutrition.any { it.label == "Salz" && !it.isSubItem })

        val dessert = day.meals.first { it.category == "Dessert" }
        assertEquals(listOf("La", "GeR"), dessert.additiveCodes)
        assertEquals(listOf("enthält Rindergelatine"), dessert.additives)
    }

    @Test
    fun `meals without photo or allergen info still parse`() {
        val day = MensaParser.parseDay(resource("mensa_day_sample.html"))
        val soup = day.meals.first { it.name == "Tagessuppe" }
        assertNull(soup.imageUrl)
        assertEquals(listOf("0,99", "1,39", "1,79"), soup.prices)
        assertTrue(soup.additiveCodes.isEmpty())
        assertTrue(soup.labels.isEmpty())
        assertTrue(soup.nutrition.isEmpty())
        assertNull(soup.nutritionBasis)
    }

    @Test
    fun `subcategories attach to the meals below them`() {
        val day = MensaParser.parseDay(resource("mensa_subgroups_sample.html"))
        val wok = day.meals.first { it.name == "Gemüsewok Curry mit Glasnudeln" }
        assertEquals("Mensa Special", wok.category)
        assertEquals("Wok", wok.subcategory)
        val schnitzel = day.meals.first { it.name.startsWith("Paniertes Schweineschnitzel") }
        assertEquals("All Time Classics", schnitzel.subcategory)
        // A new top-level category resets the subcategory.
        assertTrue(day.meals.filter { it.category == "Dessert" }.all { it.subcategory == null })
    }

    @Test
    fun `closed day yields the site's message and no meals`() {
        val day = MensaParser.parseDay(resource("mensa_closed_sample.html"))
        assertTrue(day.meals.isEmpty())
        assertEquals("Die Mensa hat an diesem Tag geschlossen.", day.closedMessage)
    }
}
