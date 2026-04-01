package com.petergabriel.budgetcalendar.features.sandbox.domain.usecase

import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxTransaction
import com.petergabriel.budgetcalendar.features.sandbox.domain.repository.ISandboxRepository
import kotlinx.coroutines.flow.Flow

class GetSandboxTransactionsUseCase(
    private val sandboxRepository: ISandboxRepository,
) {
    operator fun invoke(snapshotId: Long): Flow<List<SandboxTransaction>> {
        return sandboxRepository.getTransactionsBySnapshot(snapshotId)
    }
}
