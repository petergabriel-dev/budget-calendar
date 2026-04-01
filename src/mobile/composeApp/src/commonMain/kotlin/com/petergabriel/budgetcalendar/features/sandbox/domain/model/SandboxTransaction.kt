package com.petergabriel.budgetcalendar.features.sandbox.domain.model

import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType

data class SandboxTransaction(
    val id: Long,
    val snapshotId: Long,
    val accountId: Long,
    val amount: Long,
    val date: Long,
    val type: TransactionType,
    val status: TransactionStatus,
    val description: String?,
    val category: String?,
    val originalTransactionId: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)
