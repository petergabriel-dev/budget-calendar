package com.petergabriel.budgetcalendar.features.accounts.domain.usecase

import com.petergabriel.budgetcalendar.features.accounts.domain.repository.IAccountRepository

class DeleteAccountUseCase(
    private val accountRepository: IAccountRepository,
) {
    suspend operator fun invoke(id: Long): Result<Unit> {
        if (accountRepository.hasTransactionsForAccount(id)) {
            return Result.failure(IllegalStateException("Cannot delete account with existing transactions"))
        }

        accountRepository.deleteAccount(id)
        return Result.success(Unit)
    }
}
