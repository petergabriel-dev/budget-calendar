package com.petergabriel.budgetcalendar.features.accounts.domain.usecase

import com.petergabriel.budgetcalendar.features.accounts.domain.repository.IAccountRepository

class CalculateNetWorthUseCase(
    private val accountRepository: IAccountRepository,
) {
    suspend operator fun invoke(): Result<Long> {
        return Result.success(accountRepository.calculateNetWorth())
    }
}
