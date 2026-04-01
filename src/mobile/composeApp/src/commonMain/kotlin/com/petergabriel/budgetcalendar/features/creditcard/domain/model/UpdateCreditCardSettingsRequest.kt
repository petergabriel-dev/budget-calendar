package com.petergabriel.budgetcalendar.features.creditcard.domain.model

data class UpdateCreditCardSettingsRequest(
    val creditLimit: Long?,
    val statementBalance: Long?,
    val dueDate: Long?,
)
