package com.petergabriel.budgetcalendar.features.calendar.domain.model

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minus
import kotlinx.datetime.onDay
import kotlinx.datetime.plus
import kotlinx.datetime.yearMonth

fun LocalDate.toYearMonth(): YearMonth = yearMonth

fun YearMonth.firstDay(): LocalDate = onDay(1)

fun YearMonth.contains(date: LocalDate): Boolean =
    date.year == year && date.month == month

fun YearMonth.plusMonths(months: Int): YearMonth =
    firstDay().plus(DatePeriod(months = months)).yearMonth

fun YearMonth.lengthInDays(): Int =
    firstDay().plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1)).day
