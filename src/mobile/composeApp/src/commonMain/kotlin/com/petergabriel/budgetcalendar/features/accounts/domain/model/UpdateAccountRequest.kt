package com.petergabriel.budgetcalendar.features.accounts.domain.model

data class UpdateAccountRequest(
    val name: String? = null,
    val type: AccountType? = null,
    val isInSpendingPool: Boolean? = null,
    val description: String? = null,
)
