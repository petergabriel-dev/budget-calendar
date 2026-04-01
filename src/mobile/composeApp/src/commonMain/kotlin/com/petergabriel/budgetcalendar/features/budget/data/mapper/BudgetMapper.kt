package com.petergabriel.budgetcalendar.features.budget.data.mapper

import com.petergabriel.budgetcalendar.features.budget.domain.model.CreditCardReservation
import com.petergabriel.budgetcalendar.features.budget.domain.model.MonthlyRollover

class BudgetMapper {
    fun toAmount(rawAmount: Long): Long = rawAmount

    fun toPendingAndOverdueRow(status: String, amount: Long): PendingAndOverdueAmountRow {
        return PendingAndOverdueAmountRow(
            status = status,
            amount = amount,
        )
    }

    fun toCreditCardReservation(accountId: Long, accountName: String, reservedAmount: Long): CreditCardReservation {
        return CreditCardReservation(
            accountId = accountId,
            accountName = accountName,
            reservedAmount = reservedAmount,
            pendingTransactions = emptyList(),
        )
    }

    fun toMonthlyRollover(
        id: Long,
        year: Long,
        month: Long,
        rolloverAmount: Long,
        createdAt: Long,
    ): MonthlyRollover {
        return MonthlyRollover(
            id = id,
            year = year.toInt(),
            month = month.toInt(),
            rolloverAmount = rolloverAmount,
            createdAt = createdAt,
        )
    }
}

data class PendingAndOverdueAmountRow(
    val status: String,
    val amount: Long,
)
