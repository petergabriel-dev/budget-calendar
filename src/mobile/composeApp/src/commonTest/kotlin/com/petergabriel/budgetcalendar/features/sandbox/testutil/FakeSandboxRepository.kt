package com.petergabriel.budgetcalendar.features.sandbox.testutil

import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxSnapshot
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxTransaction
import com.petergabriel.budgetcalendar.features.sandbox.domain.repository.ISandboxRepository
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeSandboxRepository : ISandboxRepository {
    private val snapshots = mutableListOf<SandboxSnapshot>()
    private val transactions = mutableListOf<SandboxTransaction>()
    private val snapshotsFlow = MutableStateFlow<List<SandboxSnapshot>>(emptyList())
    private val transactionsFlow = MutableStateFlow<List<SandboxTransaction>>(emptyList())
    private var nextSnapshotId = 1L
    private var nextTransactionId = 1L

    override fun getAllSnapshots(): Flow<List<SandboxSnapshot>> = snapshotsFlow

    override suspend fun getSnapshotById(id: Long): SandboxSnapshot? {
        return snapshots.firstOrNull { item -> item.id == id }
    }

    override suspend fun createSnapshot(name: String, description: String?, initialSafeToSpend: Long): SandboxSnapshot {
        val now = DateUtils.nowMillis()
        val snapshot = SandboxSnapshot(
            id = nextSnapshotId++,
            name = name,
            description = description,
            createdAt = now,
            lastAccessedAt = now,
            initialSafeToSpend = initialSafeToSpend,
        )
        snapshots += snapshot
        emitSnapshots()
        return snapshot
    }

    override suspend fun updateLastAccessed(id: Long) {
        val index = snapshots.indexOfFirst { item -> item.id == id }
        if (index < 0) {
            throw NoSuchElementException("Sandbox snapshot with id=$id was not found")
        }
        snapshots[index] = snapshots[index].copy(lastAccessedAt = DateUtils.nowMillis())
        emitSnapshots()
    }

    override suspend fun deleteSnapshot(id: Long) {
        snapshots.removeAll { item -> item.id == id }
        transactions.removeAll { item -> item.snapshotId == id }
        emitSnapshots()
        emitTransactions()
    }

    override fun getTransactionsBySnapshot(snapshotId: Long): Flow<List<SandboxTransaction>> {
        return transactionsFlow.map { all ->
            all.filter { item -> item.snapshotId == snapshotId }
                .sortedWith(compareBy<SandboxTransaction> { item -> item.date }
                    .thenByDescending { item -> item.createdAt })
        }
    }

    override suspend fun insertSandboxTransaction(
        snapshotId: Long,
        accountId: Long,
        amount: Long,
        date: Long,
        type: TransactionType,
        status: TransactionStatus,
        description: String?,
        category: String?,
        originalTransactionId: Long?,
    ): Result<SandboxTransaction> {
        if (snapshots.none { snapshot -> snapshot.id == snapshotId }) {
            return Result.failure(NoSuchElementException("Sandbox snapshot with id=$snapshotId was not found"))
        }

        val now = DateUtils.nowMillis()
        val transaction = SandboxTransaction(
            id = nextTransactionId++,
            snapshotId = snapshotId,
            accountId = accountId,
            amount = amount,
            date = date,
            type = type,
            status = status,
            description = description,
            category = category,
            originalTransactionId = originalTransactionId,
            createdAt = now,
            updatedAt = now,
        )
        transactions += transaction
        emitTransactions()
        return Result.success(transaction)
    }

    override suspend fun deleteSandboxTransaction(id: Long): Result<Unit> {
        transactions.removeAll { item -> item.id == id }
        emitTransactions()
        return Result.success(Unit)
    }

    override suspend fun getSandboxBalanceDelta(snapshotId: Long): Result<Long> {
        val delta = transactions
            .filter { item -> item.snapshotId == snapshotId }
            .sumOf { item ->
                when (item.type) {
                    TransactionType.INCOME -> item.amount
                    TransactionType.EXPENSE -> -item.amount
                    TransactionType.TRANSFER -> 0L
                }
            }
        return Result.success(delta)
    }

    override suspend fun deleteExpiredSnapshots(cutoffMs: Long): Result<Int> {
        val expiredIds = snapshots
            .filter { snapshot -> snapshot.lastAccessedAt < cutoffMs }
            .map { snapshot -> snapshot.id }
        snapshots.removeAll { snapshot -> snapshot.id in expiredIds }
        transactions.removeAll { transaction -> transaction.snapshotId in expiredIds }
        emitSnapshots()
        emitTransactions()
        return Result.success(expiredIds.size)
    }

    fun seedSnapshots(vararg seeded: SandboxSnapshot) {
        snapshots.clear()
        snapshots.addAll(seeded)
        nextSnapshotId = (seeded.maxOfOrNull { item -> item.id } ?: 0L) + 1L
        emitSnapshots()
    }

    fun seedTransactions(vararg seeded: SandboxTransaction) {
        transactions.clear()
        transactions.addAll(seeded)
        nextTransactionId = (seeded.maxOfOrNull { item -> item.id } ?: 0L) + 1L
        emitTransactions()
    }

    fun allSnapshots(): List<SandboxSnapshot> = snapshots.toList()
    fun allTransactions(): List<SandboxTransaction> = transactions.toList()

    private fun emitSnapshots() {
        snapshotsFlow.value = snapshots.sortedByDescending { snapshot -> snapshot.lastAccessedAt }
    }

    private fun emitTransactions() {
        transactionsFlow.value = transactions.toList()
    }
}
