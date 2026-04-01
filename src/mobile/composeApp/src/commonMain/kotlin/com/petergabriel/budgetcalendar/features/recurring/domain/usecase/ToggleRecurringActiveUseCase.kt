package com.petergabriel.budgetcalendar.features.recurring.domain.usecase

import com.petergabriel.budgetcalendar.features.recurring.domain.model.RecurringTransaction
import com.petergabriel.budgetcalendar.features.recurring.domain.repository.IRecurringTransactionRepository

class ToggleRecurringActiveUseCase(
    private val recurringRepository: IRecurringTransactionRepository,
    private val generateMonthlyTransactionsUseCase: GenerateMonthlyTransactionsUseCase,
) {
    suspend operator fun invoke(id: Long, isActive: Boolean): Result<RecurringTransaction> {
        val updated = recurringRepository.toggleActive(id = id, isActive = isActive)
            ?: return Result.failure(NoSuchElementException("Recurring transaction with id=$id was not found"))

        if (isActive) {
            val generationResult = generateMonthlyTransactionsUseCase(monthsAhead = 1)
            if (generationResult.isFailure) {
                val error = generationResult.exceptionOrNull()
                return Result.failure(error ?: IllegalStateException("Failed to generate transactions"))
            }
        }

        return Result.success(updated)
    }
}
