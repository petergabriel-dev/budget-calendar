package com.petergabriel.budgetcalendar.features.budget.domain.model

data class MonthlyRollover(
    val id: Long,
    val year: Int,
    val month: Int,
    val rolloverAmount: Long,
    val createdAt: Long,
)
