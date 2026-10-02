package com.petergabriel.budgetcalendar.features.budget.data.mapper

class BudgetMapper {
    fun toAmount(rawAmount: Long): Long = rawAmount

    fun toPendingAndOverdueRow(status: String, amount: Long): PendingAndOverdueAmountRow {
        return PendingAndOverdueAmountRow(
            status = status,
            amount = amount,
        )
    }

}

data class PendingAndOverdueAmountRow(
    val status: String,
    val amount: Long,
)
