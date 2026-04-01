package com.petergabriel.budgetcalendar.features.creditcard.domain.model

data class CreditCardSettings(
    val id: Long,
    val accountId: Long,
    val creditLimit: Long?,
    val statementBalance: Long?,
    val dueDate: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)
