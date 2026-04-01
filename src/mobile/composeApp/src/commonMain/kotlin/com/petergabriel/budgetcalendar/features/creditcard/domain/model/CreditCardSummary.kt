package com.petergabriel.budgetcalendar.features.creditcard.domain.model

data class CreditCardSummary(
    val accountId: Long,
    val accountName: String,
    val currentBalance: Long,
    val reservedAmount: Long,
    val statementBalance: Long?,
    val creditLimit: Long?,
    val availableCredit: Long?,
    val dueDate: Long?,
)
