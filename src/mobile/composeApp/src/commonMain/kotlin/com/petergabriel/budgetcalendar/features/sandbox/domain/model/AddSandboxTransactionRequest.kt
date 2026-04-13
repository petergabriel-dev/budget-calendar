package com.petergabriel.budgetcalendar.features.sandbox.domain.model

import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType

data class AddSandboxTransactionRequest(
    val snapshotId: Long,
    val accountId: Long,
    val amount: Long,
    val date: Long,
    val type: TransactionType,
    val category: String,
    val description: String? = null,
)
