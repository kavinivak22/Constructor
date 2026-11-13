package com.example.constructorapp.util

/**
 * Pure Kotlin Multiplatform Civil Date conversion utilities.
 * Completely free of java.text.SimpleDateFormat and java.util.Calendar,
 * ensuring seamless execution across iOS, Android, and Desktop.
 */
object DateTimeUtil {

    /**
     * Converts epoch milliseconds to "YYYY-MM-DD" format.
     */
    fun formatEpochToDateString(epochMillis: Long): String {
        val days = (epochMillis / 86400000L).toInt()
        val z = days + 719468
        val era = (if (z >= 0) z else z - 146096) / 146097
        val doe = z - era * 146097
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val m = mp + (if (mp < 10) 3 else -9)
        val year = y + (if (m <= 2) 1 else 0)
        val mm = if (m < 10) "0$m" else "$m"
        val dd = if (d < 10) "0$d" else "$d"
        return "$year-$mm-$dd"
    }

    /**
     * Parses "YYYY-MM-DD" into epoch milliseconds.
     */
    fun parseDateStringToEpoch(dateStr: String): Long? {
        val parts = dateStr.trim().split("-")
        if (parts.size != 3) return null
        val y = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        val d = parts[2].toIntOrNull() ?: return null

        val adjustedYear = y - (if (m <= 2) 1 else 0)
        val era = (if (adjustedYear >= 0) adjustedYear else adjustedYear - 399) / 400
        val yoe = adjustedYear - era * 400
        val doy = (153 * (m + (if (m > 2) -3 else 9)) + 2) / 5 + d - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        val days = era * 146097 + doe - 719468
        return days * 86400000L
    }

    /**
     * Generates standard RFC 4122 v4 UUID string across all platforms.
     */
    fun randomUuid(): String {
        val hex = "0123456789abcdef"
        val sb = StringBuilder()
        for (i in 0 until 36) {
            when (i) {
                8, 13, 18, 23 -> sb.append('-')
                14 -> sb.append('4')
                19 -> sb.append(hex[kotlin.random.Random.nextInt(8, 12)])
                else -> sb.append(hex[kotlin.random.Random.nextInt(16)])
            }
        }
        return sb.toString()
    }
}
