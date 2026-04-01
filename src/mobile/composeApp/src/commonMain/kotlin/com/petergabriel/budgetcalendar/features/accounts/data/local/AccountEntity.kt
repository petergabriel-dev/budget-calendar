package com.petergabriel.budgetcalendar.features.accounts.data.local

data class AccountEntity(
    val id: Long,
    val name: String,
    val type: String,
    val balance: Long,
    val isInSpendingPool: Boolean,
    val description: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
