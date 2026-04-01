package com.petergabriel.budgetcalendar.features.sandbox.data.local

data class SandboxTransactionsEntity(
    val id: Long,
    val snapshotId: Long,
    val accountId: Long,
    val amount: Long,
    val date: Long,
    val type: String,
    val status: String,
    val description: String?,
    val category: String?,
    val originalTransactionId: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)

