package com.petergabriel.budgetcalendar.features.sandbox.domain.usecase

import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxSnapshot
import com.petergabriel.budgetcalendar.features.sandbox.domain.repository.ISandboxRepository
import kotlinx.coroutines.flow.Flow

class GetSandboxesUseCase(
    private val sandboxRepository: ISandboxRepository,
) {
    operator fun invoke(): Flow<List<SandboxSnapshot>> {
        return sandboxRepository.getAllSnapshots()
    }
}
