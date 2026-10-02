package com.petergabriel.budgetcalendar.features.budget.data.mapper

import com.petergabriel.budgetcalendar.features.budget.domain.model.CreditCardReservation

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

}

data class PendingAndOverdueAmountRow(
    val status: String,
    val amount: Long,
)
