package com.petergabriel.budgetcalendar.features.calendar.presentation

import com.petergabriel.budgetcalendar.features.calendar.domain.model.CalendarMonth
import com.petergabriel.budgetcalendar.features.calendar.domain.model.toYearMonth
import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

data class CalendarUiState(
    val currentMonth: YearMonth = nowLocalDate().toYearMonth(),
    val calendarMonth: CalendarMonth? = null,
    val endOfMonthProjection: Long = 0L,
    val accountNamesById: Map<Long, String> = emptyMap(),
    val isMonthLoading: Boolean = false,
    val monthError: String? = null,
    val isProjectionLoading: Boolean = false,
    val projectionError: String? = null,
) {
    fun transactionsOn(date: LocalDate): List<Transaction> =
        calendarMonth?.transactionsByDate?.get(date).orEmpty()
}

private fun nowLocalDate(timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDate {
    return Clock.System.now().toLocalDateTime(timeZone).date
}
