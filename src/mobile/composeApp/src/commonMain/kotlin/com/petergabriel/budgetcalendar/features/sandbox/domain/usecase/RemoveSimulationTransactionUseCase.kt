package com.petergabriel.budgetcalendar.features.sandbox.domain.usecase

import com.petergabriel.budgetcalendar.features.sandbox.domain.repository.ISandboxRepository

class RemoveSimulationTransactionUseCase(
    private val sandboxRepository: ISandboxRepository,
) {
    suspend operator fun invoke(snapshotId: Long, transactionId: Long): Result<Unit> {
        sandboxRepository.deleteSandboxTransaction(transactionId)
            .onFailure { throwable -> return Result.failure(throwable) }

        runCatching {
            sandboxRepository.updateLastAccessed(snapshotId)
        }.onFailure { throwable ->
            return Result.failure(throwable)
        }

        return Result.success(Unit)
    }
}
