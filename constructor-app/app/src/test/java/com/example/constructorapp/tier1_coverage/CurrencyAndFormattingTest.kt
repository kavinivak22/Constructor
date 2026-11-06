package com.example.constructorapp.tier1_coverage

import org.junit.Assert.*
import org.junit.Test
import java.text.NumberFormat
import java.util.Locale

class CurrencyAndFormattingTest {

    private fun formatIndianCurrency(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        formatter.maximumFractionDigits = 2
        formatter.minimumFractionDigits = 0
        return formatter.format(amount)
    }

    private fun formatAbbreviatedInr(amount: Double): String {
        return when {
            amount >= 10_000_000 -> {
                val cr = amount / 10_000_000
                if (cr % 1.0 == 0.0) "₹%.0fCr".format(Locale.US, cr) else "₹%.1fCr".format(Locale.US, cr)
            }
            amount >= 100_000 -> {
                val l = amount / 100_000
                if (l % 1.0 == 0.0) "₹%.0fL".format(Locale.US, l) else "₹%.1fL".format(Locale.US, l)
            }
            amount >= 1_000 -> {
                val k = amount / 1_000
                if (k % 1.0 == 0.0) "₹%.0fk".format(Locale.US, k) else "₹%.1fk".format(Locale.US, k)
            }
            else -> "₹%.0f".format(Locale.US, amount)
        }
    }

    @Test
    fun testIndianCurrencyFormatting_standardValues() {
        val formatted = formatIndianCurrency(250000.0)
        assertTrue("Formatted currency should contain currency symbol", formatted.contains("₹") || formatted.contains("Rs") || formatted.contains("INR"))
        assertTrue("Formatted currency should contain 250,000 or 2,50,000", formatted.contains("2,50,000") || formatted.contains("250,000"))
    }

    @Test
    fun testIndianCurrencyFormatting_zeroValue() {
        val formatted = formatIndianCurrency(0.0)
        assertTrue("Zero formatted currency should contain 0", formatted.contains("0"))
    }

    @Test
    fun testIndianCurrencyFormatting_decimalRounding() {
        val formatted = formatIndianCurrency(1234.567)
        assertTrue("Decimal should round to 2 places", formatted.contains("1,234.57"))
    }

    @Test
    fun testAbbreviatedInr_croreNotation() {
        assertEquals("₹1.5Cr", formatAbbreviatedInr(15_000_000.0))
        assertEquals("₹2Cr", formatAbbreviatedInr(20_000_000.0))
    }

    @Test
    fun testAbbreviatedInr_lakhNotation() {
        assertEquals("₹25L", formatAbbreviatedInr(2_500_000.0))
        assertEquals("₹1.5L", formatAbbreviatedInr(150_000.0))
    }

    @Test
    fun testAbbreviatedInr_thousandNotation() {
        assertEquals("₹50k", formatAbbreviatedInr(50_000.0))
        assertEquals("₹5.5k", formatAbbreviatedInr(5_500.0))
    }

    @Test
    fun testStatusResolution_activeAndCompleted() {
        fun resolveStatusCategory(status: String?): String {
            return when (status?.lowercase()) {
                "completed", "active" -> "SUCCESS"
                "planning", "draft" -> "WARNING"
                "fulfilled", "approved" -> "INFO"
                "cancelled" -> "DANGER"
                else -> "DEFAULT"
            }
        }

        assertEquals("SUCCESS", resolveStatusCategory("Active"))
        assertEquals("SUCCESS", resolveStatusCategory("completed"))
        assertEquals("WARNING", resolveStatusCategory("Planning"))
        assertEquals("WARNING", resolveStatusCategory("draft"))
        assertEquals("INFO", resolveStatusCategory("Approved"))
        assertEquals("INFO", resolveStatusCategory("Fulfilled"))
        assertEquals("DANGER", resolveStatusCategory("cancelled"))
        assertEquals("DEFAULT", resolveStatusCategory(null))
        assertEquals("DEFAULT", resolveStatusCategory("UnknownStatus"))
    }
}
