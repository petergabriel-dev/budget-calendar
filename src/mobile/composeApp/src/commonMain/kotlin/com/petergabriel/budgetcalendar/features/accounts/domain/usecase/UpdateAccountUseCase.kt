package com.petergabriel.budgetcalendar.features.accounts.domain.usecase

import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account
import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType
import com.petergabriel.budgetcalendar.features.accounts.domain.model.UpdateAccountRequest
import com.petergabriel.budgetcalendar.features.accounts.domain.repository.IAccountRepository

class UpdateAccountUseCase(
    private val accountRepository: IAccountRepository,
) {
    suspend operator fun invoke(id: Long, request: UpdateAccountRequest): Result<Account> {
        val existing = accountRepository.getAccountById(id)
            ?: return Result.failure(NoSuchElementException("Account with id=$id was not found"))

        val trimmedName = request.name?.trim()
        if (trimmedName != null && (trimmedName.isBlank() || trimmedName.length > 50)) {
            return Result.failure(IllegalArgumentException("Account name must be 1-50 characters"))
        }

        val resolvedType = request.type ?: existing.type
        val resolvedBalance = request.balance ?: existing.balance

        if (resolvedType != AccountType.CREDIT_CARD && resolvedBalance < 0) {
            return Result.failure(IllegalArgumentException("Asset accounts cannot have negative balance"))
        }

        val updated = accountRepository.updateAccount(
            id = id,
            request = request.copy(
                name = trimmedName,
                isInSpendingPool = if (resolvedType == AccountType.CREDIT_CARD) {
                    false
                } else {
                    request.isInSpendingPool
                },
            ),
        ) ?: return Result.failure(NoSuchElementException("Account with id=$id was not found after update"))

        return Result.success(updated)
    }
}
