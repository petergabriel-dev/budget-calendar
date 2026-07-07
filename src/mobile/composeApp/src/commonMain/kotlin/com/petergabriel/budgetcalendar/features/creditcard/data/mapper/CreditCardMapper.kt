package com.petergabriel.budgetcalendar.features.creditcard.data.mapper

import com.petergabriel.budgetcalendar.features.creditcard.data.local.CreditCardProjectedBalanceRow
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
            statementClosingDay = entity.statementClosingDay?.toInt(),
            paymentDueDay = entity.paymentDueDay?.toInt(),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }

    fun toProjectedBalanceRow(accountId: Long, projectedBalance: Long): CreditCardProjectedBalanceRow {
        return CreditCardProjectedBalanceRow(
            accountId = accountId,
            projectedBalance = projectedBalance,
        )
    }
}
