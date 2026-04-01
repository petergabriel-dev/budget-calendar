package com.petergabriel.budgetcalendar.features.recurring.domain.usecase

import com.petergabriel.budgetcalendar.features.recurring.domain.model.RecurringTransaction
import com.petergabriel.budgetcalendar.features.recurring.domain.model.RecurrenceType
import com.petergabriel.budgetcalendar.features.recurring.domain.model.UpdateRecurringTransactionRequest
import com.petergabriel.budgetcalendar.features.recurring.domain.repository.IRecurringTransactionRepository

class UpdateRecurringTransactionUseCase(
    private val recurringRepository: IRecurringTransactionRepository,
) {
    suspend operator fun invoke(id: Long, request: UpdateRecurringTransactionRequest): Result<RecurringTransaction> {
        val existing = recurringRepository.getById(id)
            ?: return Result.failure(NoSuchElementException("Recurring transaction with id=$id was not found"))

        val resolvedType = request.type ?: existing.type
        val resolvedAccountId = request.accountId ?: existing.accountId
        val resolvedAmount = request.amount ?: existing.amount
        val resolvedDayOfMonth = request.dayOfMonth ?: existing.dayOfMonth
        val resolvedDestinationAccountId = resolveDestinationAccountId(existing, request, resolvedType)
        val resolvedDescription = resolveDescription(existing.description, request.description)
        val resolvedIsActive = request.isActive ?: existing.isActive

        val validationError = validateRecurringInput(
            accountId = resolvedAccountId,
            destinationAccountId = resolvedDestinationAccountId,
            amount = resolvedAmount,
            dayOfMonth = resolvedDayOfMonth,
            type = resolvedType,
            description = resolvedDescription,
        )

        if (validationError != null) {
            return Result.failure(IllegalArgumentException(validationError))
        }

        val updated = recurringRepository.update(
            id = id,
            request = UpdateRecurringTransactionRequest(
                accountId = resolvedAccountId,
                destinationAccountId = resolvedDestinationAccountId,
                amount = resolvedAmount,
                dayOfMonth = resolvedDayOfMonth,
                type = resolvedType,
                description = resolvedDescription,
                isActive = resolvedIsActive,
            ),
        ) ?: return Result.failure(NoSuchElementException("Recurring transaction with id=$id was not found after update"))

        return Result.success(updated)
    }

    private fun resolveDestinationAccountId(
        existing: RecurringTransaction,
        request: UpdateRecurringTransactionRequest,
        resolvedType: RecurrenceType,
    ): Long? {
        return if (resolvedType == RecurrenceType.TRANSFER) {
            request.destinationAccountId ?: existing.destinationAccountId
        } else {
            null
        }
    }

    private fun resolveDescription(existingDescription: String?, requestedDescription: String?): String? {
        return if (requestedDescription == null) {
            existingDescription
        } else {
            requestedDescription.trim().takeUnless { value -> value.isEmpty() }
        }
    }
}
