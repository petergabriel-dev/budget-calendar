package com.petergabriel.budgetcalendar.features.budget.domain.usecase

import com.petergabriel.budgetcalendar.features.budget.domain.repository.IBudgetRepository
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import com.petergabriel.budgetcalendar.features.transactions.domain.repository.ITransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

class CalculateMonthEndRolloverUseCase(
    private val budgetRepository: IBudgetRepository,
    private val transactionRepository: ITransactionRepository,
) {
    suspend operator fun invoke(year: Int, month: Int): Long {
        val monthStart = LocalDate(year, month, 1)
            .atStartOfDayIn(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()

        val nextMonthStart = if (month == 12) {
            LocalDate(year + 1, 1, 1)
        } else {
            LocalDate(year, month + 1, 1)
        }
            .atStartOfDayIn(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()

        val monthEnd = nextMonthStart - 1L

        val spendingPoolBalance = budgetRepository.getTotalSpendingPoolBalance().first()
        val confirmedExpenses = transactionRepository
            .getConfirmedTransactionsByDateRange(monthStart, monthEnd)
            .first()
            .filter { transaction -> transaction.type == TransactionType.EXPENSE }
            .sumOf { transaction -> transaction.amount }

        return (spendingPoolBalance - confirmedExpenses).coerceAtLeast(0L)
    }
}
