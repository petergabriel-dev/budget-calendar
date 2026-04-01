package com.petergabriel.budgetcalendar.features.creditcard.domain.usecase

import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType
import com.petergabriel.budgetcalendar.features.accounts.domain.repository.IAccountRepository
import com.petergabriel.budgetcalendar.features.creditcard.domain.model.CreditCardSettings
import com.petergabriel.budgetcalendar.features.creditcard.domain.repository.ICreditCardRepository

class CreateCreditCardSettingsUseCase(
    private val accountRepository: IAccountRepository,
    private val creditCardRepository: ICreditCardRepository,
) {
    suspend operator fun invoke(
        accountId: Long,
        creditLimit: Long? = null,
        statementBalance: Long? = null,
        dueDate: Long? = null,
    ): Result<CreditCardSettings> {
        if (creditLimit != null && creditLimit < 0L) {
            return Result.failure(IllegalArgumentException("Credit limit must be >= 0"))
        }

        val account = accountRepository.getAccountById(accountId)
            ?: return Result.failure(NoSuchElementException("Account with id=$accountId was not found"))
        if (account.type != AccountType.CREDIT_CARD) {
            return Result.failure(IllegalArgumentException("Credit card settings can only be created for credit card accounts"))
        }

        return creditCardRepository.insert(
            accountId = accountId,
            creditLimit = creditLimit,
            statementBalance = statementBalance,
            dueDate = dueDate,
        )
    }
}
