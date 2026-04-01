package com.petergabriel.budgetcalendar.features.accounts.domain.usecase

import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account
import com.petergabriel.budgetcalendar.features.accounts.domain.repository.IAccountRepository
import kotlinx.coroutines.flow.Flow

class GetSpendingPoolAccountsUseCase(
    private val accountRepository: IAccountRepository,
) {
    operator fun invoke(): Flow<List<Account>> = accountRepository.getSpendingPoolAccounts()
}
