package com.petergabriel.budgetcalendar.features.recurring.domain.usecase

import com.petergabriel.budgetcalendar.features.recurring.domain.model.CreateRecurringTransactionRequest
import com.petergabriel.budgetcalendar.features.recurring.domain.model.RecurringTransaction
import com.petergabriel.budgetcalendar.features.recurring.domain.repository.IRecurringTransactionRepository

class CreateRecurringTransactionUseCase(
    private val recurringRepository: IRecurringTransactionRepository,
) {
    suspend operator fun invoke(request: CreateRecurringTransactionRequest): Result<RecurringTransaction> {
        val validationError = validateRecurringInput(
            accountId = request.accountId,
            destinationAccountId = request.destinationAccountId,
            amount = request.amount,
            dayOfMonth = request.dayOfMonth,
            type = request.type,
            description = request.description,
        )

        if (validationError != null) {
            return Result.failure(IllegalArgumentException(validationError))
        }

        val created = recurringRepository.insert(
            request.copy(
                description = request.description?.trim().takeUnless { value -> value.isNullOrEmpty() },
            ),
        )
        return Result.success(created)
    }
}
