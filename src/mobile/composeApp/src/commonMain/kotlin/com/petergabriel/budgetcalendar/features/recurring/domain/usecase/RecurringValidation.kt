package com.petergabriel.budgetcalendar.features.recurring.domain.usecase

import com.petergabriel.budgetcalendar.features.recurring.domain.model.RecurrenceType

internal fun validateRecurringInput(
    accountId: Long,
    destinationAccountId: Long?,
    amount: Long,
    dayOfMonth: Int,
    type: RecurrenceType,
    description: String?,
): String? {
    if (accountId <= 0L) {
        return "Account is required"
    }

    if (amount <= 0L) {
        return "Amount must be greater than zero"
    }

    if (dayOfMonth !in 1..31) {
        return "Day of month must be between 1 and 31"
    }

    if ((description?.trim()?.length ?: 0) > 200) {
        return "Description cannot exceed 200 characters"
    }

    if (type == RecurrenceType.TRANSFER) {
        if (destinationAccountId == null) {
            return "Destination account is required for transfers"
        }

        if (destinationAccountId == accountId) {
            return "Cannot transfer to the same account"
        }
    }

    return null
}
