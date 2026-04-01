package com.petergabriel.budgetcalendar.features.sandbox.domain.usecase

import com.petergabriel.budgetcalendar.features.sandbox.domain.repository.ISandboxRepository

class GetSandboxSafeToSpendUseCase(
    private val sandboxRepository: ISandboxRepository,
) {
    suspend operator fun invoke(snapshotId: Long): Result<Long> {
        val snapshot = sandboxRepository.getSnapshotById(snapshotId)
            ?: return Result.failure(NoSuchElementException("Sandbox snapshot with id=$snapshotId was not found"))

        val delta = sandboxRepository.getSandboxBalanceDelta(snapshotId)
            .getOrElse { throwable -> return Result.failure(throwable) }

        return Result.success((snapshot.initialSafeToSpend + delta).coerceAtLeast(0L))
    }
}
