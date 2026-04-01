package com.petergabriel.budgetcalendar.features.sandbox.data.mapper

import com.petergabriel.budgetcalendar.features.sandbox.data.local.SandboxSnapshotsEntity
import com.petergabriel.budgetcalendar.features.sandbox.data.local.SandboxTransactionsEntity
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxSnapshot
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxTransaction
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType

class SandboxMapper {
    fun toDomain(entity: SandboxSnapshotsEntity): SandboxSnapshot {
        return SandboxSnapshot(
            id = entity.id,
            name = entity.name,
            description = entity.description,
            createdAt = entity.createdAt,
            lastAccessedAt = entity.lastAccessedAt,
            initialSafeToSpend = entity.initialSafeToSpend,
        )
    }

    fun toDomain(entity: SandboxTransactionsEntity): SandboxTransaction {
        return SandboxTransaction(
            id = entity.id,
            snapshotId = entity.snapshotId,
            accountId = entity.accountId,
            amount = entity.amount,
            date = entity.date,
            type = TransactionType.fromDbValue(entity.type),
            status = TransactionStatus.fromDbValue(entity.status),
            description = entity.description,
            category = entity.category,
            originalTransactionId = entity.originalTransactionId,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }
}

