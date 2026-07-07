package com.petergabriel.budgetcalendar.core.utils

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

object DateUtils {
    const val MILLIS_IN_DAY: Long = 86_400_000L

    fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

    fun startOfDayMillis(dateMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): Long {
        val instant = Instant.fromEpochMilliseconds(dateMillis)
        val localDate = instant.toLocalDateTime(timeZone).date
        return localDate.atStartOfDayIn(timeZone).toEpochMilliseconds()
    }

    fun endOfDayMillis(dateMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): Long {
        return startOfDayMillis(dateMillis, timeZone) + MILLIS_IN_DAY - 1
    }

    fun isDateInPast(dateMillis: Long, nowMillis: Long = nowMillis()): Boolean {
        return startOfDayMillis(dateMillis) < startOfDayMillis(nowMillis)
    }

    fun isMoreThanDaysInFuture(dateMillis: Long, days: Int, nowMillis: Long = nowMillis()): Boolean {
        val lastAllowedMillis = startOfDayMillis(nowMillis) + (days * MILLIS_IN_DAY)
        return startOfDayMillis(dateMillis) > lastAllowedMillis
    }

    fun currentYearMonth(nowMillis: Long = nowMillis(), timeZone: TimeZone = TimeZone.currentSystemDefault()): Pair<Int, Int> {
        val localDate = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(timeZone).date
        return localDate.year to localDate.month.number
    }

    fun daysRemainingInMonth(
        today: LocalDate,
        tz: TimeZone = TimeZone.currentSystemDefault(),
    ): Int {
        val normalizedToday = today.atStartOfDayIn(tz).toLocalDateTime(tz).date
        val firstDayOfMonth = LocalDate(normalizedToday.year, normalizedToday.month.number, 1)
        val lastDayOfMonth = firstDayOfMonth
            .plus(DatePeriod(months = 1))
            .minus(DatePeriod(days = 1))
            .day

        return (lastDayOfMonth - normalizedToday.day + 1).coerceAtLeast(1)
    }

    fun previousMonth(year: Int, month: Int): Pair<Int, Int> {
        return if (month == 1) {
            year - 1 to 12
        } else {
            year to month - 1
        }
    }

    fun currentMonthBounds(nowMillis: Long = nowMillis(), timeZone: TimeZone = TimeZone.currentSystemDefault()): Pair<Long, Long> {
        val localDate = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(timeZone).date
        val firstDay = LocalDate(localDate.year, localDate.month.number, 1)
        val start = firstDay.atStartOfDayIn(timeZone)
        val nextMonthStart = start.plus(DatePeriod(months = 1), timeZone)
        return start.toEpochMilliseconds() to (nextMonthStart.toEpochMilliseconds() - 1)
    }

    fun millisUntilNextDay(timeZone: TimeZone = TimeZone.currentSystemDefault()): Long {
        val now = Clock.System.now()
        val tomorrow = LocalDate.fromEpochDays(now.toLocalDateTime(timeZone).date.toEpochDays() + 1)
        val tomorrowStart = tomorrow.atStartOfDayIn(timeZone)
        return (tomorrowStart - now).inWholeMilliseconds
    }

    fun toUtcDayStartMillis(localDayStartMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): Long {
        val localDate = Instant.fromEpochMilliseconds(localDayStartMillis).toLocalDateTime(timeZone).date
        return LocalDate(localDate.year, localDate.monthNumber, localDate.dayOfMonth)
            .atStartOfDayIn(TimeZone.UTC)
            .toEpochMilliseconds()
    }

    fun toLocalDayStartMillis(utcDayStartMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): Long {
        val utcDate = Instant.fromEpochMilliseconds(utcDayStartMillis).toLocalDateTime(TimeZone.UTC).date
        return utcDate.atStartOfDayIn(timeZone).toEpochMilliseconds()
    }

    fun statementClosingOnOrAfter(day: Int, fromMillis: Long, tz: TimeZone = TimeZone.currentSystemDefault()): Long {
        require(day in 1..31) { "Statement closing day must be 1-31" }
        val fromDate = Instant.fromEpochMilliseconds(fromMillis).toLocalDateTime(tz).date
        val thisMonthClosing = clampedDate(fromDate.year, fromDate.month.number, day)
        val closingDate = if (thisMonthClosing >= fromDate) {
            thisMonthClosing
        } else {
            val nextMonth = LocalDate(fromDate.year, fromDate.month.number, 1).plus(DatePeriod(months = 1))
            clampedDate(nextMonth.year, nextMonth.month.number, day)
        }
        return closingDate.atStartOfDayIn(tz).toEpochMilliseconds()
    }

    fun paymentDueAfter(day: Int, closingMillis: Long, tz: TimeZone = TimeZone.currentSystemDefault()): Long {
        require(day in 1..31) { "Payment due day must be 1-31" }
        val closingDate = Instant.fromEpochMilliseconds(closingMillis).toLocalDateTime(tz).date
        val thisMonthDue = clampedDate(closingDate.year, closingDate.month.number, day)
        val dueDate = if (thisMonthDue > closingDate) {
            thisMonthDue
        } else {
            val nextMonth = LocalDate(closingDate.year, closingDate.month.number, 1).plus(DatePeriod(months = 1))
            clampedDate(nextMonth.year, nextMonth.month.number, day)
        }
        return dueDate.atStartOfDayIn(tz).toEpochMilliseconds()
    }

    fun formatMonthDay(millis: Long, tz: TimeZone = TimeZone.currentSystemDefault()): String {
        val date = Instant.fromEpochMilliseconds(millis).toLocalDateTime(tz).date
        val month = when (date.month.number) {
            1 -> "Jan"
            2 -> "Feb"
            3 -> "Mar"
            4 -> "Apr"
            5 -> "May"
            6 -> "Jun"
            7 -> "Jul"
            8 -> "Aug"
            9 -> "Sep"
            10 -> "Oct"
            11 -> "Nov"
            else -> "Dec"
        }
        return "$month ${date.day}"
    }

    private fun clampedDate(year: Int, month: Int, day: Int): LocalDate {
        val firstDay = LocalDate(year, month, 1)
        val lastDay = firstDay.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1)).day
        return LocalDate(year, month, day.coerceAtMost(lastDay))
    }
}
