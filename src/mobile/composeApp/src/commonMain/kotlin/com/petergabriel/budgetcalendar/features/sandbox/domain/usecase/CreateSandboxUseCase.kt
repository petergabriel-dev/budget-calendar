package com.petergabriel.budgetcalendar.features.sandbox.domain.usecase

import com.petergabriel.budgetcalendar.features.budget.domain.usecase.CalculateSafeToSpendUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxSnapshot
import com.petergabriel.budgetcalendar.features.sandbox.domain.repository.ISandboxRepository
import kotlinx.coroutines.flow.first

class CreateSandboxUseCase(
    private val sandboxRepository: ISandboxRepository,
    private val calculateSafeToSpendUseCase: CalculateSafeToSpendUseCase? = null,
) {
    suspend operator fun invoke(
        name: String,
        description: String?,
        currentSafeToSpend: Long,
    ): Result<SandboxSnapshot> = runCatching {
        val trimmedName = name.trim()
        if (trimmedName.length !in 1..50) {
            throw IllegalArgumentException("Sandbox name must be 1-50 characters")
        }

        val trimmedDescription = description?.trim()?.ifBlank { null }

        sandboxRepository.createSnapshot(
            name = trimmedName,
            description = trimmedDescription,
            initialSafeToSpend = currentSafeToSpend,
        )
    }

    suspend operator fun invoke(name: String, description: String?): Result<SandboxSnapshot> {
        val calculateSafeToSpend = calculateSafeToSpendUseCase
            ?: return Result.failure(IllegalStateException("CalculateSafeToSpendUseCase is not configured"))

        val currentSafeToSpend = runCatching {
            calculateSafeToSpend().first().availableToSpend
        }.getOrElse { throwable ->
            return Result.failure(throwable)
        }

        return invoke(
            name = name,
            description = description,
            currentSafeToSpend = currentSafeToSpend,
        )
    }
}
