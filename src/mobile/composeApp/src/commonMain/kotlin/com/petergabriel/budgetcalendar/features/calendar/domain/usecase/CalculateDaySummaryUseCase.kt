package com.petergabriel.budgetcalendar.features.calendar.domain.usecase

import com.petergabriel.budgetcalendar.features.calendar.domain.model.DaySummary
import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class CalculateDaySummaryUseCase {
    operator fun invoke(
        transactions: List<Transaction>,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): Map<LocalDate, DaySummary> {
        return transactions
            .asSequence()
            .filterNot { transaction -> transaction.status == TransactionStatus.CANCELLED }
            .groupBy { transaction ->
                Instant
                    .fromEpochMilliseconds(transaction.date)
                    .toLocalDateTime(timeZone)
                    .date
            }
            .mapValues { (_, dayTransactions) ->
                val totalIncome = dayTransactions
                    .filter { transaction -> transaction.type == TransactionType.INCOME }
                    .sumOf { transaction -> transaction.amount }
                val totalExpenses = dayTransactions
                    .filter { transaction -> transaction.type == TransactionType.EXPENSE }
                    .sumOf { transaction -> transaction.amount }

                DaySummary(
                    totalIncome = totalIncome,
                    totalExpenses = totalExpenses,
                    netAmount = totalIncome - totalExpenses,
                    transactionCount = dayTransactions.size,
                    hasPending = dayTransactions.any { transaction -> transaction.status == TransactionStatus.PENDING },
                    hasOverdue = dayTransactions.any { transaction -> transaction.status == TransactionStatus.OVERDUE },
                    hasConfirmed = dayTransactions.any { transaction -> transaction.status == TransactionStatus.CONFIRMED },
                )
            }
    }
}
