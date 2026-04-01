package com.petergabriel.budgetcalendar.features.sandbox.domain.repository

import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxSnapshot
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxTransaction
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface ISandboxRepository {
    fun getAllSnapshots(): Flow<List<SandboxSnapshot>>
    suspend fun getSnapshotById(id: Long): SandboxSnapshot?
    suspend fun createSnapshot(name: String, description: String?, initialSafeToSpend: Long): SandboxSnapshot
    suspend fun updateLastAccessed(id: Long)
    suspend fun deleteSnapshot(id: Long)

    fun getTransactionsBySnapshot(snapshotId: Long): Flow<List<SandboxTransaction>>
    suspend fun insertSandboxTransaction(
        snapshotId: Long,
        accountId: Long,
        amount: Long,
        date: Long,
        type: TransactionType,
        status: TransactionStatus,
        description: String?,
        category: String?,
        originalTransactionId: Long?,
    ): Result<SandboxTransaction>

    suspend fun deleteSandboxTransaction(id: Long): Result<Unit>
    suspend fun getSandboxBalanceDelta(snapshotId: Long): Result<Long>
    suspend fun deleteExpiredSnapshots(cutoffMs: Long): Result<Int>
}
