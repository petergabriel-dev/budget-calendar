package com.petergabriel.budgetcalendar.features.budget.domain.model

import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account

data class SpendingPool(
    val accounts: List<Account>,
    val totalBalance: Long,
    val pendingReservations: Long,
    val overdueReservations: Long,
    val confirmedThisMonth: Long,
    val availableToSpend: Long,
)
