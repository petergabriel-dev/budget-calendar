package com.petergabriel.budgetcalendar.features.budget.presentation

import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account
import com.petergabriel.budgetcalendar.features.budget.domain.model.BudgetSummary
import com.petergabriel.budgetcalendar.features.budget.domain.model.CreditCardReservation

data class BudgetUiState(
    val budgetSummary: BudgetSummary = emptyBudgetSummary(),
    val spendingPoolAccounts: List<Account> = emptyList(),
    val creditCardReservations: List<CreditCardReservation> = emptyList(),
    val isLoading: Boolean = false,
    val isCalculating: Boolean = false,
    val error: String? = null,
)

private fun emptyBudgetSummary(): BudgetSummary {
    return BudgetSummary(
        totalLiquidAssets = 0L,
        pendingReservations = 0L,
        overdueReservations = 0L,
        confirmedSpending = 0L,
        availableToSpend = 0L,
        rolloverAmount = 0L,
        creditCardReserved = 0L,
        lastCalculatedAt = DateUtils.nowMillis(),
    )
}
