package com.petergabriel.budgetcalendar.features.transactions.domain.model

data class CreateTransactionRequest(
    val accountId: Long,
    val destinationAccountId: Long? = null,
    val amount: Long,
    val date: Long,
    val type: TransactionType,
    val status: TransactionStatus = TransactionStatus.PENDING,
    val description: String? = null,
    val category: String? = null,
    val linkedTransactionId: Long? = null,
    val isSandbox: Boolean = false,
)
