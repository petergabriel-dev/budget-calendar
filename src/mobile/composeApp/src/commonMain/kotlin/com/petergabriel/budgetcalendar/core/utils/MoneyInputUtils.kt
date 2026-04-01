package com.petergabriel.budgetcalendar.core.utils

import kotlin.math.abs

object MoneyInputUtils {
    private val amountPattern = Regex("^-?\\d+(\\.\\d{0,2})?$")

    fun parseToCents(input: String): Long? {
        val normalized = input.trim().replace(",", "")
        if (normalized.isEmpty() || !amountPattern.matches(normalized)) {
            return null
        }

        val isNegative = normalized.startsWith('-')
        val unsigned = normalized.removePrefix("-")
        val parts = unsigned.split('.')

        val major = parts[0].toLongOrNull() ?: return null
        val minorRaw = parts.getOrNull(1).orEmpty()
        val minor = when (minorRaw.length) {
            0 -> 0L
            1 -> "${minorRaw}0".toLong()
            else -> minorRaw.toLong()
        }

        val cents = (major * 100L) + minor
        return if (isNegative) -cents else cents
    }

    fun centsToInput(amountInCents: Long): String {
        val sign = if (amountInCents < 0) "-" else ""
        val absolute = abs(amountInCents)
        val major = absolute / 100
        val minor = (absolute % 100).toInt()

        return if (minor == 0) {
            "$sign$major"
        } else {
            val minorText = minor.toString().padStart(2, '0').trimEnd('0')
            "$sign$major.$minorText"
        }
    }
}
