package com.petergabriel.budgetcalendar.features.accounts.data.mapper

import com.petergabriel.budgetcalendar.features.accounts.data.local.AccountEntity
import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account
import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType

class AccountMapper {
    fun toDomain(entity: AccountEntity): Account {
        return Account(
            id = entity.id,
            name = entity.name,
            type = AccountType.fromDbValue(entity.type),
            balance = entity.balance,
            isInSpendingPool = entity.isInSpendingPool,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            description = entity.description,
        )
    }
}
