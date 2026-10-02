package com.petergabriel.budgetcalendar.features.budget.domain.model

data class BudgetSummary(
    val totalLiquidAssets: Long,
    val pendingReservations: Long,
    val overdueReservations: Long,
    val availableToSpend: Long,
    val creditCardReserved: Long,
    val lastCalculatedAt: Long,
)
