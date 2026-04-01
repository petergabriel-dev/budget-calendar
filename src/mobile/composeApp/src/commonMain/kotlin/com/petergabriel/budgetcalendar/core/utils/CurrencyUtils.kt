package com.petergabriel.budgetcalendar.core.utils

import kotlin.math.abs

object CurrencyUtils {
    fun formatCents(amountInCents: Long, currencySymbol: String = "₱", includePlusSign: Boolean = false): String {
        val absolute = abs(amountInCents)
        val major = absolute / 100
        val minor = absolute % 100
        val sign = when {
            amountInCents < 0 -> "-"
            includePlusSign && amountInCents > 0 -> "+"
            else -> ""
        }

        return "$sign$currencySymbol${major.withThousandsSeparator()}.${minor.toString().padStart(2, '0')}"
    }

    fun dollarsToCents(amount: Double): Long = (amount * 100.0).toLong()

    private fun Long.withThousandsSeparator(): String {
        val str = this.toString()
        return buildString {
            str.forEachIndexed { index, char ->
                if (index > 0 && (str.length - index) % 3 == 0) append(',')
                append(char)
            }
        }
    }
}
