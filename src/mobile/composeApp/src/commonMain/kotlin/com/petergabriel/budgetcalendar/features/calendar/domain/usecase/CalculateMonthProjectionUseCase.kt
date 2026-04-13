package com.petergabriel.budgetcalendar.features.calendar.domain.usecase

import com.petergabriel.budgetcalendar.features.accounts.domain.repository.IAccountRepository
import com.petergabriel.budgetcalendar.features.calendar.domain.model.firstDay
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import com.petergabriel.budgetcalendar.features.transactions.domain.repository.ITransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus

class CalculateMonthProjectionUseCase(
    private val transactionRepository: ITransactionRepository,
    private val accountRepository: IAccountRepository,
) {
    operator fun invoke(yearMonth: YearMonth): Flow<Long> {
        val timeZone = TimeZone.currentSystemDefault()
        val startOfMonthMillis = yearMonth
            .firstDay()
            .atStartOfDayIn(timeZone)
            .toEpochMilliseconds()
        val endOfMonthMillis = yearMonth
            .firstDay()
            .plus(DatePeriod(months = 1))
            .atStartOfDayIn(timeZone)
            .toEpochMilliseconds() - 1L

        return combine(
            accountRepository.getSpendingPoolAccounts(),
            transactionRepository.getMonthProjectionTransactions(startOfMonthMillis, endOfMonthMillis),
        ) { spendingPoolAccounts, monthTransactions ->
            if (spendingPoolAccounts.isEmpty()) {
                return@combine 0L
            }

            val poolBalance = spendingPoolAccounts.sumOf { account -> account.balance }
            val spendingPoolAccountIds = spendingPoolAccounts.map { it.id }.toSet()
            val signedTransactionSum = monthTransactions.sumOf { transaction ->
                when (transaction.type) {
                    TransactionType.INCOME -> transaction.amount
                    TransactionType.EXPENSE -> -transaction.amount
                    TransactionType.TRANSFER -> {
                        // Source account is in spending pool (query filters for this)
                        // If destination is NOT in spending pool, money is leaving the pool
                        val destinationIsInPool = transaction.destinationAccountId?.let { it in spendingPoolAccountIds } ?: false
                        if (destinationIsInPool) 0L else -transaction.amount
                    }
                }
            }

            poolBalance + signedTransactionSum
        }
    }
}
