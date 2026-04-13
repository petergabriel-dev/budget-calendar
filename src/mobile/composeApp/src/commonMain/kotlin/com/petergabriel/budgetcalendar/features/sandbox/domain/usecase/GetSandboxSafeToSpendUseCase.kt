package com.petergabriel.budgetcalendar.features.sandbox.domain.usecase

import com.petergabriel.budgetcalendar.features.sandbox.domain.repository.ISandboxRepository
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class GetSandboxSafeToSpendUseCase(
    private val sandboxRepository: ISandboxRepository,
) {
    operator fun invoke(snapshotId: Long): Flow<Long> = flow {
        val snapshot = sandboxRepository.getSnapshotById(snapshotId)
            ?: throw NoSuchElementException("Sandbox snapshot with id=$snapshotId was not found")
        val initialSafeToSpend = snapshot.initialSafeToSpend

        emitAll(
            sandboxRepository.getTransactionsBySnapshot(snapshotId)
                .map { transactions ->
                    val delta = transactions.sumOf { transaction ->
                        when (transaction.type) {
                            TransactionType.INCOME -> transaction.amount
                            TransactionType.EXPENSE -> -transaction.amount
                            TransactionType.TRANSFER -> 0L
                        }
                    }
                    initialSafeToSpend + delta
                },
        )
    }
}
