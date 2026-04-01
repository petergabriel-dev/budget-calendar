package com.petergabriel.budgetcalendar.features.calendar.domain.model

import kotlinx.datetime.LocalDate

data class CalendarDay(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
    val dailySummary: DaySummary?,
)
