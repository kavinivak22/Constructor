package com.example.constructorapp.tier2_boundary

import org.junit.Assert.*
import org.junit.Test

class LocationAndPriorityBoundariesTest {

    data class ParsedCoordinates(val latitude: Double, val longitude: Double)

    private fun parseCoordinates(location: String?): ParsedCoordinates? {
        if (location.isNullOrBlank()) return null
        val parts = location.split(",")
        if (parts.size != 2) return null

        val lat = parts[0].trim().toDoubleOrNull() ?: return null
        val lng = parts[1].trim().toDoubleOrNull() ?: return null

        if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return null
        return ParsedCoordinates(lat, lng)
    }

    private fun normalizePriority(priority: String?): String {
        return when (priority?.trim()?.lowercase()) {
            "high" -> "High"
            "low" -> "Low"
            else -> "Medium" // Default fallback
        }
    }

    @Test
    fun testCoordinates_validBoundaries() {
        val equator = parseCoordinates("0.0, 0.0")
        assertNotNull(equator)
        assertEquals(0.0, equator!!.latitude, 0.0001)
        assertEquals(0.0, equator.longitude, 0.0001)

        val northPole = parseCoordinates("90.0, 0.0")
        assertNotNull(northPole)
        assertEquals(90.0, northPole!!.latitude, 0.0001)

        val southPole = parseCoordinates("-90.0, 0.0")
        assertNotNull(southPole)
        assertEquals(-90.0, southPole!!.latitude, 0.0001)

        val dateLineEast = parseCoordinates("0.0, 180.0")
        assertNotNull(dateLineEast)
        assertEquals(180.0, dateLineEast!!.longitude, 0.0001)

        val dateLineWest = parseCoordinates("0.0, -180.0")
        assertNotNull(dateLineWest)
        assertEquals(-180.0, dateLineWest!!.longitude, 0.0001)
    }

    @Test
    fun testCoordinates_invalidLatitudeAndLongitude() {
        assertNull("Latitude > 90 is invalid", parseCoordinates("90.0001, 50.0"))
        assertNull("Latitude < -90 is invalid", parseCoordinates("-90.0001, 50.0"))
        assertNull("Longitude > 180 is invalid", parseCoordinates("10.0, 180.0001"))
        assertNull("Longitude < -180 is invalid", parseCoordinates("10.0, -180.0001"))
        assertNull("Non-numeric latitude", parseCoordinates("abc, 50.0"))
        assertNull("Non-numeric longitude", parseCoordinates("10.0, xyz"))
        assertNull("Three dimensions", parseCoordinates("10.0, 20.0, 30.0"))
        assertNull("Single number", parseCoordinates("10.0"))
    }

    @Test
    fun testCoordinates_whitespaceTolerance() {
        val parsed = parseCoordinates("   13.0827   ,   80.2707   ")
        assertNotNull(parsed)
        assertEquals(13.0827, parsed!!.latitude, 0.0001)
        assertEquals(80.2707, parsed.longitude, 0.0001)
    }

    @Test
    fun testNamedLocations_notCoordinates() {
        assertNull("Plain text name is not coordinate", parseCoordinates("Grocery Store"))
        assertNull("Office name is not coordinate", parseCoordinates("Home"))
        assertNull("City and state text is not coordinate", parseCoordinates("Chennai, Tamil Nadu"))
        assertNull("Empty string is not coordinate", parseCoordinates(""))
        assertNull("Null is not coordinate", parseCoordinates(null))
    }

    @Test
    fun testPriority_normalizationAndFallback() {
        assertEquals("High", normalizePriority("High"))
        assertEquals("High", normalizePriority("high"))
        assertEquals("High", normalizePriority("HIGH"))
        assertEquals("High", normalizePriority("  high  "))

        assertEquals("Low", normalizePriority("Low"))
        assertEquals("Low", normalizePriority("low"))
        assertEquals("Low", normalizePriority("LOW"))

        assertEquals("Medium", normalizePriority("Medium"))
        assertEquals("Medium", normalizePriority("medium"))
        assertEquals("Medium", normalizePriority("MEDIUM"))

        // Invalid fallbacks to Medium
        assertEquals("Medium", normalizePriority("Urgent"))
        assertEquals("Medium", normalizePriority("None"))
        assertEquals("Medium", normalizePriority(""))
        assertEquals("Medium", normalizePriority(null))
        assertEquals("Medium", normalizePriority("Critical"))
    }
}
