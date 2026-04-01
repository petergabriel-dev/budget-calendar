package com.petergabriel.budgetcalendar.features.sandbox.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.petergabriel.budgetcalendar.core.database.BudgetCalendarDatabase
import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.sandbox.data.local.SandboxSnapshotsEntity
import com.petergabriel.budgetcalendar.features.sandbox.data.local.SandboxTransactionsEntity
import com.petergabriel.budgetcalendar.features.sandbox.data.mapper.SandboxMapper
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxSnapshot
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxTransaction
import com.petergabriel.budgetcalendar.features.sandbox.domain.repository.ISandboxRepository
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SandboxRepositoryImpl(
    private val database: BudgetCalendarDatabase,
    private val sandboxMapper: SandboxMapper,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ISandboxRepository {

    override fun getAllSnapshots(): Flow<List<SandboxSnapshot>> {
        return database.sandboxQueries
            .getAllSnapshots(::toSnapshotEntity)
            .asFlow()
            .mapToList(dispatcher)
            .map { entities -> entities.map(sandboxMapper::toDomain) }
    }

    override suspend fun getSnapshotById(id: Long): SandboxSnapshot? {
        return database.sandboxQueries
            .getSnapshotById(id, ::toSnapshotEntity)
            .executeAsOneOrNull()
            ?.let(sandboxMapper::toDomain)
    }

    override suspend fun createSnapshot(
        name: String,
        description: String?,
        initialSafeToSpend: Long,
    ): SandboxSnapshot {
        val now = DateUtils.nowMillis()
        database.sandboxQueries.insertSnapshot(
            name,
            description,
            now,
            now,
            initialSafeToSpend,
        )
        val insertedId = database.sandboxQueries.getLastInsertedSnapshotId().executeAsOne()
        return requireNotNull(getSnapshotById(insertedId)) {
            "Sandbox snapshot with id=$insertedId was not found"
        }
    }

    override suspend fun updateLastAccessed(id: Long) {
        database.sandboxQueries.updateSnapshotLastAccessed(
            DateUtils.nowMillis(),
            id,
        )
    }

    override suspend fun deleteSnapshot(id: Long) {
        database.sandboxQueries.deleteSnapshot(id)
    }

    override fun getTransactionsBySnapshot(snapshotId: Long): Flow<List<SandboxTransaction>> {
        return database.sandbox_transactionsQueries
            .getSandboxTransactionsBySnapshot(snapshotId, ::toTransactionEntity)
            .asFlow()
            .mapToList(dispatcher)
            .map { entities -> entities.map(sandboxMapper::toDomain) }
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
    ): Result<SandboxTransaction> = runCatching {
        val now = DateUtils.nowMillis()
        database.sandbox_transactionsQueries.insertSandboxTransaction(
            snapshotId,
            accountId,
            amount,
            date,
            type.dbValue,
            status.dbValue,
            description,
            category,
            originalTransactionId,
            now,
            now,
        )

        val insertedId = database.sandbox_transactionsQueries.getLastInsertedSandboxTransactionId().executeAsOne()
        database.sandbox_transactionsQueries
            .getSandboxTransactionById(insertedId, ::toTransactionEntity)
            .executeAsOneOrNull()
            ?.let(sandboxMapper::toDomain)
            ?: throw NoSuchElementException("Sandbox transaction with id=$insertedId was not found")
    }

    override suspend fun deleteSandboxTransaction(id: Long): Result<Unit> = runCatching {
        database.sandbox_transactionsQueries.deleteSandboxTransaction(id)
    }

    override suspend fun getSandboxBalanceDelta(snapshotId: Long): Result<Long> = runCatching {
        database.sandbox_transactionsQueries.getSandboxBalanceDeltaBySnapshot(snapshotId).executeAsOne()
    }

    override suspend fun deleteExpiredSnapshots(cutoffMs: Long): Result<Int> = runCatching {
        val expired = database.sandboxQueries
            .getExpiredSnapshots(cutoffMs, ::toSnapshotEntity)
            .executeAsList()

        expired.forEach { snapshot ->
            database.sandboxQueries.deleteSnapshot(snapshot.id)
        }

        expired.size
    }

    private fun toSnapshotEntity(
        id: Long,
        name: String,
        description: String?,
        createdAt: Long,
        lastAccessedAt: Long,
        initialSafeToSpend: Long,
    ): SandboxSnapshotsEntity {
        return SandboxSnapshotsEntity(
            id = id,
            name = name,
            description = description,
            createdAt = createdAt,
            lastAccessedAt = lastAccessedAt,
            initialSafeToSpend = initialSafeToSpend,
        )
    }

    private fun toTransactionEntity(
        id: Long,
        snapshotId: Long,
        accountId: Long,
        amount: Long,
        date: Long,
        type: String,
        status: String,
        description: String?,
        category: String?,
        originalTransactionId: Long?,
        createdAt: Long,
        updatedAt: Long,
    ): SandboxTransactionsEntity {
        return SandboxTransactionsEntity(
            id = id,
            snapshotId = snapshotId,
            accountId = accountId,
            amount = amount,
            date = date,
            type = type,
            status = status,
            description = description,
            category = category,
            originalTransactionId = originalTransactionId,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }
}
