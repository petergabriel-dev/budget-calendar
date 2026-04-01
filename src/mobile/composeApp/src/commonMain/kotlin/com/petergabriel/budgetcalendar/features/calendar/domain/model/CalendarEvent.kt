package com.petergabriel.budgetcalendar.features.calendar.domain.model

import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import kotlinx.datetime.LocalDate

enum class CalendarEventType {
    PENDING_INCOME,
    PENDING_EXPENSE,
    CONFIRMED_INCOME,
    CONFIRMED_EXPENSE,
    OVERDUE,
}

data class CalendarEvent(
    val date: LocalDate,
    val transaction: Transaction,
    val eventType: CalendarEventType,
)
