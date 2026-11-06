package com.example.constructorapp.tier1_coverage

import com.example.constructorapp.util.CurrencyFormatter
import com.example.constructorapp.util.toInrCompact
import com.example.constructorapp.util.toInrFull
import org.junit.Assert.*
import org.junit.Test

class CurrencyFormatterEmpiricalTest {

    @Test
    fun testBoundary_zero() {
        assertEquals("₹0", CurrencyFormatter.formatCompact(0.0))
        assertEquals("₹0", CurrencyFormatter.formatFull(0.0))
        assertEquals("₹0", 0.0.toInrCompact())
        assertEquals("₹0", 0L.toInrFull())
    }

    @Test
    fun testBoundary_negativeValues() {
        assertEquals("-₹50k", CurrencyFormatter.formatCompact(-50_000.0))
        assertEquals("-₹750", CurrencyFormatter.formatCompact(-750.0))
        assertEquals("-₹1.5Cr", CurrencyFormatter.formatCompact(-15_000_000.0))
        assertEquals("-₹2,50,000", CurrencyFormatter.formatFull(-250_000.0))
        assertEquals("-₹1,00,00,00,000", CurrencyFormatter.formatFull(-1_000_000_000.0))
    }

    @Test
    fun testBoundary_hundredCrore() {
        // 100 Crore = 1,000,000,000
        val hundredCr = 1_000_000_000.0
        assertEquals("₹100Cr", CurrencyFormatter.formatCompact(hundredCr))
        assertEquals("₹1,00,00,00,000", CurrencyFormatter.formatFull(hundredCr))
    }

    @Test
    fun testBoundary_fivePointFiveLakh() {
        val fivePointFiveLakh = 550_000.0
        assertEquals("₹5.5L", CurrencyFormatter.formatCompact(fivePointFiveLakh))
        assertEquals("₹5,50,000", CurrencyFormatter.formatFull(fivePointFiveLakh))
    }

    @Test
    fun testBoundary_fiftyThousand() {
        val fiftyK = 50_000.0
        assertEquals("₹50k", CurrencyFormatter.formatCompact(fiftyK))
        assertEquals("₹50,000", CurrencyFormatter.formatFull(fiftyK))
    }

    @Test
    fun testBoundary_sevenHundredFifty() {
        val sevenFifty = 750.0
        assertEquals("₹750", CurrencyFormatter.formatCompact(sevenFifty))
        assertEquals("₹750", CurrencyFormatter.formatFull(sevenFifty))
    }

    @Test
    fun testIndianCommaGrouping_allScales() {
        assertEquals("₹5", CurrencyFormatter.formatFull(5.0))
        assertEquals("₹50", CurrencyFormatter.formatFull(50.0))
        assertEquals("₹500", CurrencyFormatter.formatFull(500.0))
        assertEquals("₹5,000", CurrencyFormatter.formatFull(5_000.0))
        assertEquals("₹50,000", CurrencyFormatter.formatFull(50_000.0))
        assertEquals("₹1,25,000", CurrencyFormatter.formatFull(125_000.0))
        assertEquals("₹2,50,000", CurrencyFormatter.formatFull(250_000.0))
        assertEquals("₹15,00,000", CurrencyFormatter.formatFull(1_500_000.0))
        assertEquals("₹1,50,00,000", CurrencyFormatter.formatFull(15_000_000.0))
        assertEquals("₹10,00,00,000", CurrencyFormatter.formatFull(100_000_000.0))
        assertEquals("₹1,00,00,00,000", CurrencyFormatter.formatFull(1_000_000_000.0))
    }

    @Test
    fun testDecimalRounding() {
        assertEquals("₹1,234.57", CurrencyFormatter.formatFull(1234.567, includeDecimals = true))
        assertEquals("₹1,234", CurrencyFormatter.formatFull(1234.567, includeDecimals = false))
        assertEquals("₹1,234", CurrencyFormatter.formatFull(1234.0, includeDecimals = true))
    }

    @Test
    fun testParseInr() {
        assertEquals(1_000_000_000.0, CurrencyFormatter.parseInr("₹100Cr")!!, 0.001)
        assertEquals(550_000.0, CurrencyFormatter.parseInr("₹5.5L")!!, 0.001)
        assertEquals(50_000.0, CurrencyFormatter.parseInr("₹50k")!!, 0.001)
        assertEquals(750.0, CurrencyFormatter.parseInr("₹750")!!, 0.001)
        assertEquals(250_000.0, CurrencyFormatter.parseInr("₹2,50,000")!!, 0.001)
        assertEquals(-50_000.0, CurrencyFormatter.parseInr("-₹50k")!!, 0.001)
        assertNull(CurrencyFormatter.parseInr("not-a-number"))
    }
}
