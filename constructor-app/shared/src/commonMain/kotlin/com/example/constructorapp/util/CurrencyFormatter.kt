package com.example.constructorapp.util

import kotlin.math.abs
import kotlin.math.round

/**
 * Deterministic Indian Currency Formatter for INR.
 * Produces exact Indian numbering system representations across Kotlin Multiplatform targets:
 * - Full comma grouping: ₹1,25,000 | ₹2,50,000 | ₹1,50,00,000
 * - Compact abbreviations: ₹1.5Cr | ₹25L | ₹50k | ₹750
 */
object CurrencyFormatter {

    /**
     * Formats an amount into compact Indian notation (Cr, L, k).
     */
    fun formatCompact(amount: Double): String {
        val isNegative = amount < 0
        val absAmount = abs(amount)

        val formatted = when {
            absAmount >= 10_000_000.0 -> {
                val cr = absAmount / 10_000_000.0
                if (cr % 1.0 == 0.0) "₹${cr.toLong()}Cr" else "₹${formatOneDecimal(cr)}Cr"
            }
            absAmount >= 100_000.0 -> {
                val l = absAmount / 100_000.0
                if (l % 1.0 == 0.0) "₹${l.toLong()}L" else "₹${formatOneDecimal(l)}L"
            }
            absAmount >= 1_000.0 -> {
                val k = absAmount / 1_000.0
                if (k % 1.0 == 0.0) "₹${k.toLong()}k" else "₹${formatOneDecimal(k)}k"
            }
            else -> "₹${absAmount.toLong()}"
        }

        return if (isNegative) "-$formatted" else formatted
    }

    private fun formatOneDecimal(v: Double): String {
        val rounded = round(v * 10.0) / 10.0
        val whole = rounded.toLong()
        val frac = round((abs(rounded) - abs(whole)) * 10.0).toInt()
        return if (frac == 0) "$whole" else "$whole.$frac"
    }

    /**
     * Formats an amount using Indian comma grouping (last 3 digits, then pairs of 2 digits).
     */
    fun formatFull(amount: Double, includeDecimals: Boolean = false): String {
        val isNegative = amount < 0
        val absAmount = abs(amount)
        val wholePart = absAmount.toLong()

        val groupedWhole = formatIndianGrouping(wholePart)

        val result = if (includeDecimals) {
            val fractionPart = round((absAmount - wholePart) * 100).toInt()
            if (fractionPart > 0) {
                val fracStr = if (fractionPart < 10) "0$fractionPart" else "$fractionPart"
                "₹$groupedWhole.$fracStr"
            } else {
                "₹$groupedWhole"
            }
        } else {
            "₹$groupedWhole"
        }

        return if (isNegative) "-$result" else result
    }

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
        val lower = clean.lowercase()
        return try {
            when {
                lower.endsWith("cr") -> lower.removeSuffix("cr").toDouble() * 10_000_000.0
                lower.endsWith("l") -> lower.removeSuffix("l").toDouble() * 100_000.0
                lower.endsWith("k") -> lower.removeSuffix("k").toDouble() * 1_000.0
                else -> clean.toDouble()
            }
        } catch (_: Exception) {
            null
        }
    }
}

// Extension Functions
fun Double.toInrCompact(): String = CurrencyFormatter.formatCompact(this)
fun Double.toInrFull(includeDecimals: Boolean = false): String = CurrencyFormatter.formatFull(this, includeDecimals)
fun Long.toInrCompact(): String = CurrencyFormatter.formatCompact(this.toDouble())
fun Long.toInrFull(): String = CurrencyFormatter.formatFull(this.toDouble(), false)
