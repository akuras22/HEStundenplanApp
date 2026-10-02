package de.hsesslingen.stundenplan.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CampusLocationsTest {

    @Test
    fun `locates QIS rooms by campus, building and floor`() {
        val f = CampusLocations.locateRoom("Gebäude 01 - F 01.406")!!
        assertEquals("F 01.406", f.room)
        assertEquals("Campus Esslingen Flandernstraße", f.campusName)
        assertEquals("Gebäude F 01", f.buildingName)
        assertEquals("4. Obergeschoss", f.floorLabel)
        assertEquals(48.745379, f.latitude, 1e-6)

        assertEquals("Erdgeschoss", CampusLocations.locateRoom("Gebäude 01 - F 01.016")!!.floorLabel)
        assertEquals("Untergeschoss", CampusLocations.locateRoom("Gebäude 01 - S 01.-122")!!.floorLabel)
        assertEquals("Campus Göppingen", CampusLocations.locateRoom("Gebäude 02 - G 02.110")!!.campusName)
        assertEquals("S 03.001a", CampusLocations.locateRoom("Gebäude 03 - S 03.001a")!!.room)
    }

    @Test
    fun `buildings only known by street are marked approximate`() {
        val container = CampusLocations.locateRoom("Gebäude 13A - S 13A.012")
        assertNotNull(container)
        assertTrue(container!!.isApproximate)
        assertEquals("Gebäude S 13A", container.buildingName)
    }

    @Test
    fun `virtual and unknown rooms have no location`() {
        assertNull(CampusLocations.locateRoom("Gebäude 01 - SV1"))
        assertNull(CampusLocations.locateRoom("Gebäude 01 - GV2"))
        assertNull(CampusLocations.locateRoom(null))
        assertNull(CampusLocations.locateRoom("Online"))
        assertNull(CampusLocations.locateRoom("Gebäude 99 - S 99.101"))
    }

    @Test
    fun `tile math matches OpenStreetMap's`() {
        // Building F 01 lies in tile 17/68930/45153 (standard slippy-map formula, computed separately).
        val position = tilePosition(48.745379, 9.321960, 17)
        assertEquals(68930, tileIndex(position.x))
        assertEquals(45153, tileIndex(position.y))
    }
}
