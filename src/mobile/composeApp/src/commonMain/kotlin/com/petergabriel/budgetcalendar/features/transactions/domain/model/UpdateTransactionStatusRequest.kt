package com.petergabriel.budgetcalendar.features.transactions.domain.model

data class UpdateTransactionStatusRequest(
    val status: TransactionStatus,
)
