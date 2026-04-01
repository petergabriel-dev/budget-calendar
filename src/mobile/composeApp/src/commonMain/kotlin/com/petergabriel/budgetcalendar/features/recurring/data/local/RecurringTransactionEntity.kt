package com.petergabriel.budgetcalendar.features.recurring.data.local

data class RecurringTransactionEntity(
    val id: Long,
    val accountId: Long,
    val destinationAccountId: Long?,
    val amount: Long,
    val dayOfMonth: Int,
    val type: String,
    val description: String?,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
