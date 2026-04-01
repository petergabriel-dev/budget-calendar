package com.petergabriel.budgetcalendar.features.creditcard.data.mapper

import com.petergabriel.budgetcalendar.features.creditcard.data.local.CreditCardReservedAmountRow
import com.petergabriel.budgetcalendar.features.creditcard.data.local.CreditCardSettingsEntity
import com.petergabriel.budgetcalendar.features.creditcard.domain.model.CreditCardSettings

class CreditCardMapper {
    fun toDomain(entity: CreditCardSettingsEntity): CreditCardSettings {
        return CreditCardSettings(
            id = entity.id,
            accountId = entity.accountId,
            creditLimit = entity.creditLimit,
            statementBalance = entity.statementBalance,
            dueDate = entity.dueDate,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }

    fun toReservedAmountRow(accountId: Long, reservedAmount: Long): CreditCardReservedAmountRow {
        return CreditCardReservedAmountRow(
            accountId = accountId,
            reservedAmount = reservedAmount,
        )
    }
}
