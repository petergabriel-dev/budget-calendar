package com.petergabriel.budgetcalendar.features.recurring.domain.usecase

import com.petergabriel.budgetcalendar.features.recurring.domain.repository.IRecurringTransactionRepository

class DeleteRecurringTransactionUseCase(
    private val recurringRepository: IRecurringTransactionRepository,
) {
    suspend operator fun invoke(id: Long): Result<Unit> {
        recurringRepository.delete(id)
        return Result.success(Unit)
    }
}
