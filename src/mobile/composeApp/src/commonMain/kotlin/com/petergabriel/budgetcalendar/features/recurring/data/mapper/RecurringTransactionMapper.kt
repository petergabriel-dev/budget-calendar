package com.petergabriel.budgetcalendar.features.recurring.data.mapper

import com.petergabriel.budgetcalendar.features.recurring.data.local.RecurringTransactionEntity
import com.petergabriel.budgetcalendar.features.recurring.domain.model.RecurringTransaction
import com.petergabriel.budgetcalendar.features.recurring.domain.model.RecurrenceType

class RecurringTransactionMapper {
    fun toDomain(entity: RecurringTransactionEntity): RecurringTransaction {
        return RecurringTransaction(
            id = entity.id,
            accountId = entity.accountId,
            destinationAccountId = entity.destinationAccountId,
            amount = entity.amount,
            dayOfMonth = entity.dayOfMonth,
            type = RecurrenceType.fromDbValue(entity.type),
            description = entity.description,
            isActive = entity.isActive,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }
}
