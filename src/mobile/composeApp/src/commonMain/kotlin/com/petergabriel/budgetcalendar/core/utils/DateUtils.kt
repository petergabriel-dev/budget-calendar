package com.petergabriel.budgetcalendar.core.utils

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
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
}
