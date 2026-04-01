package com.petergabriel.budgetcalendar.features.calendar.domain.model

data class DaySummary(
    val totalIncome: Long,
    val totalExpenses: Long,
    val netAmount: Long,
    val transactionCount: Int,
    val hasPending: Boolean,
    val hasOverdue: Boolean,
    val hasConfirmed: Boolean,
)
