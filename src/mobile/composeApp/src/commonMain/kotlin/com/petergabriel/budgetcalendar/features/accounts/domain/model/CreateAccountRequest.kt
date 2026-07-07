package com.petergabriel.budgetcalendar.features.accounts.domain.model

data class CreateAccountRequest(
    val name: String,
    val type: AccountType,
    val initialBalance: Long,
    val isInSpendingPool: Boolean,
    val description: String? = null,
    val creditLimit: Long? = null,
    val statementClosingDay: Int? = null,
    val paymentDueDay: Int? = null,
)
