package com.petergabriel.budgetcalendar.features.calendar.domain.usecase

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BuildCalendarMonthUseCaseTest {
    private val useCase = BuildCalendarMonthUseCase(CalculateDaySummaryUseCase())
    private val timeZone = TimeZone.UTC

    @Test
    fun buildMonth_alwaysReturnsContiguousSixWeekGrid() {
        listOf(
            YearMonth(2026, 2),
            YearMonth(2028, 2),
            YearMonth(2026, 7),
            YearMonth(2026, 8),
        ).forEach { yearMonth ->
            val month = useCase(
                yearMonth = yearMonth,
                transactions = emptyList(),
                timeZone = timeZone,
                nowMillisProvider = { 0L },
            )

            val expectedFirstDate = yearMonth.firstDay.minus(
                DatePeriod(days = yearMonth.firstDay.dayOfWeek.daysSinceSunday()),
            )
            assertEquals(42, month.days.size, "$yearMonth cell count")
            assertEquals(expectedFirstDate, month.days.first().date, "$yearMonth first cell")
            assertEquals(DayOfWeek.SUNDAY, month.days.first().date.dayOfWeek, "$yearMonth first weekday")
            assertEquals(
                yearMonth.firstDay,
                month.days.first { it.isCurrentMonth }.date,
                "$yearMonth first current-month day",
            )
            assertEquals(
                yearMonth.numberOfDays,
                month.days.count { it.isCurrentMonth },
                "$yearMonth current-month day count",
            )
            assertTrue(
                month.days.zipWithNext().all { (previous, next) ->
                    next.date == previous.date.plus(DatePeriod(days = 1))
                },
                "$yearMonth dates must be contiguous",
            )
        }
    }

    private fun DayOfWeek.daysSinceSunday(): Int =
        if (this == DayOfWeek.SUNDAY) 0 else ordinal + 1
}
