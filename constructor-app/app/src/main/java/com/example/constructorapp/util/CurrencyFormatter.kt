package com.example.constructorapp.util

import java.util.Locale

/**
 * Deterministic Indian Currency Formatter for INR.
 * Produces exact Indian numbering system representations:
 * - Full comma grouping: ₹1,25,000 | ₹2,50,000 | ₹1,50,00,000
 * - Compact abbreviations: ₹1.5Cr | ₹25L | ₹50k | ₹750
 */
object CurrencyFormatter {

    /**
     * Formats an amount into compact Indian notation (Cr, L, k).
     * Examples:
     * - 15,000,000 -> "₹1.5Cr"
     * - 20,000,000 -> "₹2Cr"
     * - 2,500,000  -> "₹25L"
     * - 150,000    -> "₹1.5L"
     * - 50,000     -> "₹50k"
     * - 5,500      -> "₹5.5k"
     * - 750        -> "₹750"
     */
    fun formatCompact(amount: Double): String {
        val isNegative = amount < 0
        val absAmount = kotlin.math.abs(amount)

        val formatted = when {
            absAmount >= 10_000_000.0 -> {
                val cr = absAmount / 10_000_000.0
                if (cr % 1.0 == 0.0) "₹%.0fCr".format(Locale.US, cr) else "₹%.1fCr".format(Locale.US, cr)
            }
            absAmount >= 100_000.0 -> {
                val l = absAmount / 100_000.0
                if (l % 1.0 == 0.0) "₹%.0fL".format(Locale.US, l) else "₹%.1fL".format(Locale.US, l)
            }
            absAmount >= 1_000.0 -> {
                val k = absAmount / 1_000.0
                if (k % 1.0 == 0.0) "₹%.0fk".format(Locale.US, k) else "₹%.1fk".format(Locale.US, k)
            }
            else -> "₹%.0f".format(Locale.US, absAmount)
        }

        return if (isNegative) "-$formatted" else formatted
    }

    /**
     * Formats an amount using Indian comma grouping (last 3 digits, then pairs of 2 digits).
     * Examples:
     * - 250000.0   -> "₹2,50,000"
     * - 125000.0   -> "₹1,25,000"
     * - 15000000.0 -> "₹1,50,00,000"
     * - 1234.567   -> "₹1,234.57" (with includeDecimals = true)
     */
    fun formatFull(amount: Double, includeDecimals: Boolean = false): String {
        val isNegative = amount < 0
        val absAmount = kotlin.math.abs(amount)
        val wholePart = absAmount.toLong()

        val groupedWhole = formatIndianGrouping(wholePart)

        val result = if (includeDecimals) {
            val fractionPart = kotlin.math.round((absAmount - wholePart) * 100).toInt()
            if (fractionPart > 0) {
                "₹$groupedWhole.%02d".format(Locale.US, fractionPart)
            } else {
                "₹$groupedWhole"
            }
        } else {
            "₹$groupedWhole"
        }

        return if (isNegative) "-$result" else result
    }

    /**
     * Core grouping algorithm for Indian numbering system:
     * 125000 -> 1,25,000 | 15000000 -> 1,50,00,000
     */
    private fun formatIndianGrouping(number: Long): String {
        val str = number.toString()
        if (str.length <= 3) return str
        val lastThree = str.substring(str.length - 3)
        val remaining = str.substring(0, str.length - 3)
        val sb = StringBuilder()
        var count = 0
        for (i in remaining.length - 1 downTo 0) {
            sb.append(remaining[i])
            count++
            if (count % 2 == 0 && i > 0) {
                sb.append(',')
            }
        }
        return sb.reverse().toString() + "," + lastThree
    }

    /**
     * Parses an INR formatted string back into Double.
     * Supports: "₹1.5Cr", "₹25L", "₹50k", "₹1,25,000", "250000"
     */
    fun parseInr(text: String): Double? {
        val clean = text.replace("₹", "").replace(",", "").trim()
        val lower = clean.lowercase(Locale.US)
        return try {
            when {
                lower.endsWith("cr") -> lower.removeSuffix("cr").toDouble() * 10_000_000.0
                lower.endsWith("l") -> lower.removeSuffix("l").toDouble() * 100_000.0
                lower.endsWith("k") -> lower.removeSuffix("k").toDouble() * 1_000.0
                else -> clean.toDouble()
            }
        } catch (e: Exception) {
            null
        }
    }
}

// Convenient Extension Functions
fun Double.toInrCompact(): String = CurrencyFormatter.formatCompact(this)
fun Double.toInrFull(includeDecimals: Boolean = false): String = CurrencyFormatter.formatFull(this, includeDecimals)
fun Long.toInrCompact(): String = CurrencyFormatter.formatCompact(this.toDouble())
fun Long.toInrFull(): String = CurrencyFormatter.formatFull(this.toDouble(), false)
