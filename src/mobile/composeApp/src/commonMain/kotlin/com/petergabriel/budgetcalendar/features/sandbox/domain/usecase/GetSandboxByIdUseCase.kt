package com.petergabriel.budgetcalendar.features.sandbox.domain.usecase

import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxSnapshot
import com.petergabriel.budgetcalendar.features.sandbox.domain.repository.ISandboxRepository

class GetSandboxByIdUseCase(
    private val sandboxRepository: ISandboxRepository,
) {
    suspend operator fun invoke(id: Long): SandboxSnapshot? {
        return sandboxRepository.getSnapshotById(id)
    }
}
