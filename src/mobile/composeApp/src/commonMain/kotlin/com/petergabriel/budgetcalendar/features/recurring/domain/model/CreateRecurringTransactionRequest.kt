package com.petergabriel.budgetcalendar.features.recurring.domain.model

data class CreateRecurringTransactionRequest(
    val accountId: Long,
    val destinationAccountId: Long? = null,
    val amount: Long,
    val dayOfMonth: Int,
    val type: RecurrenceType,
    val description: String? = null,
    val isActive: Boolean = true,
)
