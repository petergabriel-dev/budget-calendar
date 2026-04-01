package com.petergabriel.budgetcalendar.features.budget.domain.model

import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction

data class CreditCardReservation(
    val accountId: Long,
    val accountName: String,
    val reservedAmount: Long,
    val pendingTransactions: List<Transaction>,
)
