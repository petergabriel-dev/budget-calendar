package com.petergabriel.budgetcalendar.features.calendar.presentation.components

import kotlinx.datetime.Month
import kotlinx.datetime.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals

class MonthNavigationRowTest {
    @Test
    fun toDisplayLabel_abbreviatesAllMonths() {
        val expectedLabels = listOf(
            "Jan 2026", "Feb 2026", "Mar 2026", "Apr 2026",
            "May 2026", "Jun 2026", "Jul 2026", "Aug 2026",
            "Sep 2026", "Oct 2026", "Nov 2026", "Dec 2026",
        )

        Month.entries.forEachIndexed { index, month ->
            assertEquals(expectedLabels[index], YearMonth(2026, month).toDisplayLabel())
        }
    }
}
