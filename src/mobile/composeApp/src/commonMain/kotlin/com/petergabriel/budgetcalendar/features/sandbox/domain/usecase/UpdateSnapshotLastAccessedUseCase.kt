package com.petergabriel.budgetcalendar.features.sandbox.domain.usecase

import com.petergabriel.budgetcalendar.features.sandbox.domain.repository.ISandboxRepository

class UpdateSnapshotLastAccessedUseCase(
    private val sandboxRepository: ISandboxRepository,
) {
    suspend operator fun invoke(id: Long): Result<Unit> {
        return runCatching {
            sandboxRepository.updateLastAccessed(id)
        }
    }
}
