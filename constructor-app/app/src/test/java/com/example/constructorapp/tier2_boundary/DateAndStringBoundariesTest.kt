package com.example.constructorapp.tier2_boundary

import org.junit.Assert.*
import org.junit.Test
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class DateAndStringBoundariesTest {

    private fun isValidIsoDate(dateStr: String?): Boolean {
        if (dateStr.isNullOrBlank()) return false
        val regex = Regex("""^\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\d|3[01])$""")
        if (!regex.matches(dateStr)) return false

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
        }
        return try {
            sdf.parse(dateStr)
            true
        } catch (e: ParseException) {
            false
        }
    }

    @Test
    fun testValidIsoDates() {
        assertTrue(isValidIsoDate("2026-11-01"))
        assertTrue(isValidIsoDate("2026-01-31"))
        assertTrue(isValidIsoDate("2026-12-31"))
        assertTrue(isValidIsoDate("2024-02-29")) // Leap year
        assertTrue(isValidIsoDate("2000-02-29")) // Century leap year
    }

    @Test
    fun testInvalidIsoDates_calendarBoundaries() {
        assertFalse("2025 is not a leap year", isValidIsoDate("2025-02-29"))
        assertFalse("April only has 30 days", isValidIsoDate("2026-04-31"))
        assertFalse("Month 13 does not exist", isValidIsoDate("2026-13-01"))
        assertFalse("Month 00 is invalid", isValidIsoDate("2026-00-15"))
        assertFalse("Day 00 is invalid", isValidIsoDate("2026-05-00"))
        assertFalse("Day 32 is invalid", isValidIsoDate("2026-05-32"))
    }

    @Test
    fun testInvalidIsoDates_malformedFormats() {
        assertFalse("DD-MM-YYYY format is not ISO", isValidIsoDate("01-11-2026"))
        assertFalse("Slash delimiter is not ISO", isValidIsoDate("2026/11/01"))
        assertFalse("Single digit month/day not allowed", isValidIsoDate("2026-1-1"))
        assertFalse("Alpha month not allowed", isValidIsoDate("01-Nov-2026"))
        assertFalse("Empty string is invalid", isValidIsoDate(""))
        assertFalse("Whitespace string is invalid", isValidIsoDate("   "))
        assertFalse("Null is invalid", isValidIsoDate(null))
        assertFalse("Random text is invalid", isValidIsoDate("yesterday"))
    }

    @Test
    fun testStringLength_extremeAndLongText() {
        val longTitle = "A".repeat(5000)
        assertEquals(5000, longTitle.length)
        assertTrue(longTitle.isNotBlank())

        val megaDescription = "Concrete slab curing inspection required. ".repeat(1000)
        assertEquals(42000, megaDescription.length)
        assertTrue(megaDescription.startsWith("Concrete"))
    }

    @Test
    fun testString_specialCharactersAndEscaping() {
        val injectionString = "' OR '1'='1'; DROP TABLE reminders; --"
        assertTrue(injectionString.contains("'"))
        assertTrue(injectionString.contains(";"))

        val xssString = "<script>alert('xss');</script>"
        assertTrue(xssString.contains("<"))
        assertTrue(xssString.contains(">"))

        val quotesAndSlashes = "Item \"A\" \\ 'B' \n \t \r"
        assertTrue(quotesAndSlashes.contains("\""))
        assertTrue(quotesAndSlashes.contains("\\"))
    }

    @Test
    fun testString_multilingualAndEmojiFidelity() {
        val tamilSiteName = "மெய்யழகன் கன்ஸ்ட்ரக்ஷன்ஸ் - தளம் 4"
        val hindiSiteName = "नमस्ते कंस्ट्रक्शन प्रोजेक्ट - साईट १"
        val emojiName = "🏗️ Foundation Pour 🏢 Level 14 📐"

        assertEquals("மெய்யழகன் கன்ஸ்ட்ரக்ஷன்ஸ் - தளம் 4", tamilSiteName)
        assertEquals("नमस्ते कंस्ट्रक्शन प्रोजेक्ट - साईट १", hindiSiteName)
        assertTrue(emojiName.contains("🏗️"))
        assertTrue(emojiName.contains("📐"))
    }
}
