package com.petergabriel.budgetcalendar.features.creditcard.data.local

data class CreditCardSettingsEntity(
    val id: Long,
    val accountId: Long,
    val creditLimit: Long?,
    val statementBalance: Long?,
    val dueDate: Long?,
    val statementClosingDay: Long?,
    val paymentDueDay: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)

data class CreditCardProjectedBalanceRow(
    val accountId: Long,
    val projectedBalance: Long,
)
