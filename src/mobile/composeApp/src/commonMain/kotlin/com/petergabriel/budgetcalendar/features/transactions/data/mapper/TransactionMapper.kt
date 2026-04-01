package com.petergabriel.budgetcalendar.features.transactions.data.mapper

import com.petergabriel.budgetcalendar.features.transactions.data.local.TransactionEntity
import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType

class TransactionMapper {
    fun toDomain(entity: TransactionEntity): Transaction {
        return Transaction(
            id = entity.id,
            accountId = entity.accountId,
            destinationAccountId = entity.destinationAccountId,
            amount = entity.amount,
            date = entity.date,
            type = TransactionType.fromDbValue(entity.type),
            status = TransactionStatus.fromDbValue(entity.status),
            description = entity.description,
            category = entity.category,
            linkedTransactionId = entity.linkedTransactionId,
            isSandbox = entity.isSandbox,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }
}
