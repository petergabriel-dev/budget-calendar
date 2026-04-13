package com.petergabriel.budgetcalendar.core.utils

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class DateUtilsTest {

    @Test
    fun daysRemainingInMonth_janFirst_returnsThirtyOne() {
        assertDaysRemaining(
            date = LocalDate(2026, 1, 1),
            expected = 31,
        )
    }

    @Test
    fun daysRemainingInMonth_janLast_returnsOne() {
        assertDaysRemaining(
            date = LocalDate(2026, 1, 31),
            expected = 1,
        )
    }

    @Test
    fun daysRemainingInMonth_febNonLeapLastDay_returnsOne() {
        assertDaysRemaining(
            date = LocalDate(2025, 2, 28),
            expected = 1,
        )
    }

    @Test
    fun daysRemainingInMonth_febLeapDayMinusOne_returnsTwo() {
        assertDaysRemaining(
            date = LocalDate(2024, 2, 28),
            expected = 2,
        )
    }

    @Test
    fun daysRemainingInMonth_febLeapLastDay_returnsOne() {
        assertDaysRemaining(
            date = LocalDate(2024, 2, 29),
            expected = 1,
        )
    }

    @Test
    fun daysRemainingInMonth_marchMidMonth_returnsSeventeen() {
        assertDaysRemaining(
            date = LocalDate(2026, 3, 15),
            expected = 17,
        )
    }

    @Test
    fun daysRemainingInMonth_aprilMidMonth_returnsSixteen() {
        assertDaysRemaining(
            date = LocalDate(2026, 4, 15),
            expected = 16,
        )
    }

    @Test
    fun daysRemainingInMonth_decLastDay_returnsOne() {
        assertDaysRemaining(
            date = LocalDate(2026, 12, 31),
            expected = 1,
        )
    }

    private fun assertDaysRemaining(
        date: LocalDate,
        expected: Int,
    ) {
        val actual = DateUtils.daysRemainingInMonth(
            today = date,
            tz = TimeZone.currentSystemDefault(),
        )

        assertEquals(expected, actual)
    }
}
