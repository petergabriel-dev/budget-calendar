package com.petergabriel.budgetcalendar.features.accounts.domain.usecase

import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account
import com.petergabriel.budgetcalendar.features.accounts.domain.repository.IAccountRepository

class GetAccountByIdUseCase(
    private val accountRepository: IAccountRepository,
) {
    suspend operator fun invoke(id: Long): Result<Account> {
        val account = accountRepository.getAccountById(id)
            ?: return Result.failure(NoSuchElementException("Account with id=$id was not found"))
        return Result.success(account)
    }
}
