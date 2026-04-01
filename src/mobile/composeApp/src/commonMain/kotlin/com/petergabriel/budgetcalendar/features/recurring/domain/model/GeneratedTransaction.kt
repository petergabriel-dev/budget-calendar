package com.petergabriel.budgetcalendar.features.recurring.domain.model

data class GeneratedTransaction(
    val recurringId: Long,
    val date: Long,
    val amount: Long,
    val type: RecurrenceType,
    val accountId: Long,
    val willGenerate: Boolean,
)
