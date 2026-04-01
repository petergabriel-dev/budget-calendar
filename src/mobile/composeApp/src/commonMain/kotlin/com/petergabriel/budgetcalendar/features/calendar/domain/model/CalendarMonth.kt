package com.petergabriel.budgetcalendar.features.calendar.domain.model

import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

data class CalendarMonth(
    val yearMonth: YearMonth,
    val days: List<CalendarDay>,
    val transactionsByDate: Map<LocalDate, List<Transaction>>,
)
