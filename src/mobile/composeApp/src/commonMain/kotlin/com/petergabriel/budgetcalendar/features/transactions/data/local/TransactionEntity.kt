package com.petergabriel.budgetcalendar.features.transactions.data.local

data class TransactionEntity(
    val id: Long,
    val accountId: Long,
    val destinationAccountId: Long?,
    val amount: Long,
    val signedAmount: Long,
    val date: Long,
    val type: String,
    val status: String,
    val description: String?,
    val category: String?,
    val linkedTransactionId: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)
