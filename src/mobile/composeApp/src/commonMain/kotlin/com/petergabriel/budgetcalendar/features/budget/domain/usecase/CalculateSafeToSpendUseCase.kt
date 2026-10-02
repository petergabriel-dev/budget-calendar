package com.petergabriel.budgetcalendar.features.budget.domain.usecase

import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.budget.domain.model.BudgetSummary
import com.petergabriel.budgetcalendar.features.budget.domain.repository.IBudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class CalculateSafeToSpendUseCase(
    private val budgetRepository: IBudgetRepository,
) {
    operator fun invoke(): Flow<BudgetSummary> = combine(
        budgetRepository.getTotalSpendingPoolBalance(),
        budgetRepository.getPendingReservations(),
        budgetRepository.getOverdueReservations(),
        budgetRepository.getCreditCardReservedAmount(),
    ) { totalBalance, pendingReservations, overdueReservations, creditCardReserved ->
        val availableToSpend = (
            totalBalance - pendingReservations - overdueReservations - creditCardReserved
        ).coerceAtLeast(0L)

        BudgetSummary(
            totalLiquidAssets = totalBalance,
            pendingReservations = pendingReservations,
            overdueReservations = overdueReservations,
            availableToSpend = availableToSpend,
            creditCardReserved = creditCardReserved,
            lastCalculatedAt = DateUtils.nowMillis(),
        )
    }
}
