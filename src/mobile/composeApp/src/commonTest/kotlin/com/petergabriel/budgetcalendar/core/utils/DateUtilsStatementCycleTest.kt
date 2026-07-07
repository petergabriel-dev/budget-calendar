package com.petergabriel.budgetcalendar.core.utils

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class DateUtilsStatementCycleTest {
    private val tz = TimeZone.UTC

    @Test
    fun statementClosingOnOrAfter_day31InFebruary_clampsToLastDay() {
        val closing = DateUtils.statementClosingOnOrAfter(
            day = 31,
            fromMillis = millis(LocalDate(2025, 2, 1)),
            tz = tz,
        )

        assertEquals(LocalDate(2025, 2, 28), date(closing))
    }

    @Test
    fun statementClosingOnOrAfter_day31InLeapFebruary_clampsToLeapDay() {
        val closing = DateUtils.statementClosingOnOrAfter(
            day = 31,
            fromMillis = millis(LocalDate(2024, 2, 1)),
            tz = tz,
        )

        assertEquals(LocalDate(2024, 2, 29), date(closing))
    }

    @Test
    fun statementClosingOnOrAfter_day31InApril_clampsToThirty() {
        val closing = DateUtils.statementClosingOnOrAfter(
            day = 31,
            fromMillis = millis(LocalDate(2026, 4, 1)),
            tz = tz,
        )

        assertEquals(LocalDate(2026, 4, 30), date(closing))
    }

    @Test
    fun statementClosingOnOrAfter_boundaryDayEqualsClosing_returnsSameDay() {
        val closing = DateUtils.statementClosingOnOrAfter(
            day = 15,
            fromMillis = millis(LocalDate(2026, 3, 15)),
            tz = tz,
        )

        assertEquals(LocalDate(2026, 3, 15), date(closing))
    }

    @Test
    fun statementClosingOnOrAfter_afterCurrentMonthClosing_returnsNextMonthClosing() {
        val closing = DateUtils.statementClosingOnOrAfter(
            day = 10,
            fromMillis = millis(LocalDate(2026, 3, 11)),
            tz = tz,
        )

        assertEquals(LocalDate(2026, 4, 10), date(closing))
    }

    @Test
    fun paymentDueAfter_dueDayGreaterThanClosingDay_returnsSameMonthDue() {
        val due = DateUtils.paymentDueAfter(
            day = 25,
            closingMillis = millis(LocalDate(2026, 3, 15)),
            tz = tz,
        )

        assertEquals(LocalDate(2026, 3, 25), date(due))
    }

    @Test
    fun paymentDueAfter_dueDayLessThanClosingDay_returnsNextMonthDue() {
        val due = DateUtils.paymentDueAfter(
            day = 9,
            closingMillis = millis(LocalDate(2026, 3, 15)),
            tz = tz,
        )

        assertEquals(LocalDate(2026, 4, 9), date(due))
    }

    @Test
    fun paymentDueAfter_dueDay31InApril_clampsToThirty() {
        val due = DateUtils.paymentDueAfter(
            day = 31,
            closingMillis = millis(LocalDate(2026, 4, 15)),
            tz = tz,
        )

        assertEquals(LocalDate(2026, 4, 30), date(due))
    }

    @Test
    fun formatMonthDay_returnsShortMonthAndDay() {
        assertEquals("Mar 5", DateUtils.formatMonthDay(millis(LocalDate(2026, 3, 5)), tz))
    }

    private fun millis(date: LocalDate): Long = date.atStartOfDayIn(tz).toEpochMilliseconds()

    private fun date(millis: Long): LocalDate = Instant.fromEpochMilliseconds(millis).toLocalDateTime(tz).date
}
