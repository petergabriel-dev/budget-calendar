package com.petergabriel.budgetcalendar.features.sandbox.domain.usecase

import com.petergabriel.budgetcalendar.features.sandbox.domain.repository.ISandboxRepository

class DeleteSandboxUseCase(
    private val sandboxRepository: ISandboxRepository,
) {
    suspend operator fun invoke(snapshotId: Long): Result<Unit> {
        return runCatching {
            sandboxRepository.deleteSnapshot(snapshotId)
        }
    }
}
