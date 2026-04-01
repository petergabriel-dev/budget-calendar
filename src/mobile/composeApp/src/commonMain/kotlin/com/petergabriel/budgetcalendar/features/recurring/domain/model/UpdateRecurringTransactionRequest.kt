package com.petergabriel.budgetcalendar.features.recurring.domain.model

data class UpdateRecurringTransactionRequest(
    val accountId: Long? = null,
    val destinationAccountId: Long? = null,
    val amount: Long? = null,
    val dayOfMonth: Int? = null,
    val type: RecurrenceType? = null,
    val description: String? = null,
    val isActive: Boolean? = null,
)
