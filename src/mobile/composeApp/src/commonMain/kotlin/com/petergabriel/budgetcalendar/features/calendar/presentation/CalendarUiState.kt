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
    val selectedDate: LocalDate = nowLocalDate(),
    val calendarMonth: CalendarMonth? = null,
    val selectedDayTransactions: List<Transaction> = emptyList(),
    val endOfMonthProjection: Long = 0L,
    val accountNamesById: Map<Long, String> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

private fun nowLocalDate(timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDate {
    return Clock.System.now().toLocalDateTime(timeZone).date
}
