package com.petergabriel.budgetcalendar.features.calendar.presentation.components

import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlin.test.Test
import kotlin.test.assertEquals

class DayTransactionSectionHeaderTest {
    @Test
    fun formatDayTransactionHeaderLabel_returnsTodayPrefixWhenSelectedDateIsToday() {
        val today = LocalDate(year = 2026, month = Month.FEBRUARY, day = 3)

        val label = formatDayTransactionHeaderLabel(
            selectedDate = today,
            today = today,
        )

        assertEquals("Today · Feb 3", label)
    }

    @Test
    fun formatDayTransactionHeaderLabel_returnsWeekdayPrefixWhenSelectedDateDiffersFromToday() {
        val selectedDate = LocalDate(year = 2026, month = Month.FEBRUARY, day = 2)
        val today = LocalDate(year = 2026, month = Month.FEBRUARY, day = 3)

        val label = formatDayTransactionHeaderLabel(
            selectedDate = selectedDate,
            today = today,
        )

        assertEquals("Monday · Feb 2", label)
    }
}
