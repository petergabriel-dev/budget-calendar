package com.petergabriel.budgetcalendar.features.creditcard.data.local

data class CreditCardSettingsEntity(
    val id: Long,
    val accountId: Long,
    val creditLimit: Long?,
    val statementBalance: Long?,
    val dueDate: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)

data class CreditCardReservedAmountRow(
    val accountId: Long,
    val reservedAmount: Long,
)
