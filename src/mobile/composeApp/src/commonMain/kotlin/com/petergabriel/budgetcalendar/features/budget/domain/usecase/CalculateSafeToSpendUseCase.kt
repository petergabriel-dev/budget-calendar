package com.petergabriel.budgetcalendar.features.budget.domain.usecase

import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.budget.domain.model.BudgetSummary
import com.petergabriel.budgetcalendar.features.budget.domain.repository.IBudgetRepository
import com.petergabriel.budgetcalendar.features.budget.domain.repository.IMonthlyRolloverRepository
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import com.petergabriel.budgetcalendar.features.transactions.domain.repository.ITransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class CalculateSafeToSpendUseCase(
    private val budgetRepository: IBudgetRepository,
    private val monthlyRolloverRepository: IMonthlyRolloverRepository,
    private val transactionRepository: ITransactionRepository,
) {
    operator fun invoke(): Flow<BudgetSummary> {
        val (monthStart, monthEnd) = DateUtils.currentMonthBounds()
        
        // Combine budget-related flows first
        val budgetDataFlow = combine(
            budgetRepository.getTotalSpendingPoolBalance(),
            budgetRepository.getPendingReservations(),
            budgetRepository.getOverdueReservations(),
            budgetRepository.getCreditCardReservedAmount(),
            monthlyRolloverRepository.getAllRollovers(),
        ) { totalBalance, pendingReservations, overdueReservations, creditCardReserved, rollovers ->
            BudgetData(
                totalBalance = totalBalance,
                pendingReservations = pendingReservations,
                overdueReservations = overdueReservations,
                creditCardReserved = creditCardReserved,
                rollovers = rollovers,
            )
        }
        
        // Combine with confirmed transactions flow
        val confirmedTransactionsFlow = transactionRepository.getConfirmedTransactionsByDateRange(monthStart, monthEnd)
        
        return combine(budgetDataFlow, confirmedTransactionsFlow) { budgetData, confirmedTransactions ->
            val confirmedSpending = confirmedTransactions
                .filter { transaction -> transaction.type == TransactionType.EXPENSE }
                .sumOf { transaction -> transaction.amount }
            
            // Bugfix (2026-04-01): Subtract confirmedSpending from STS to account for 
            // confirmed transactions that may not yet be reflected in totalBalance.
            // This ensures STS correctly decreases when expenses are confirmed.
            // Note: If adjustBalance() works correctly (updates totalBalance when confirming),
            // this could lead to double-deduction. The confirmedSpending subtraction is
            // a safeguard for the case where adjustBalance hasn't propagated.
            val availableToSpend = (
                budgetData.totalBalance -
                    budgetData.pendingReservations -
                    budgetData.overdueReservations -
                    budgetData.creditCardReserved -
                    confirmedSpending
                ).coerceAtLeast(0L)
            
            val latestRollover = budgetData.rollovers.firstOrNull()?.rolloverAmount ?: 0L

            BudgetSummary(
                totalLiquidAssets = budgetData.totalBalance,
                pendingReservations = budgetData.pendingReservations,
                overdueReservations = budgetData.overdueReservations,
                confirmedSpending = confirmedSpending,
                availableToSpend = availableToSpend,
                rolloverAmount = latestRollover,
                creditCardReserved = budgetData.creditCardReserved,
                lastCalculatedAt = DateUtils.nowMillis(),
            )
        }
    }
    
    private data class BudgetData(
        val totalBalance: Long,
        val pendingReservations: Long,
        val overdueReservations: Long,
        val creditCardReserved: Long,
        val rollovers: List<com.petergabriel.budgetcalendar.features.budget.domain.model.MonthlyRollover>,
    )
}
