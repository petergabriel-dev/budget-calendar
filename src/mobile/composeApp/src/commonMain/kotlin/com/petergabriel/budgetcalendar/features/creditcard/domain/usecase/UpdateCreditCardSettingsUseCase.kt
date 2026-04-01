package com.petergabriel.budgetcalendar.features.creditcard.domain.usecase

import com.petergabriel.budgetcalendar.features.creditcard.domain.model.CreditCardSettings
import com.petergabriel.budgetcalendar.features.creditcard.domain.repository.ICreditCardRepository

class UpdateCreditCardSettingsUseCase(
    private val creditCardRepository: ICreditCardRepository,
) {
    suspend operator fun invoke(
        accountId: Long,
        creditLimit: Long?,
        statementBalance: Long?,
        dueDate: Long?,
    ): Result<CreditCardSettings> {
        if (creditLimit != null && creditLimit < 0L) {
            return Result.failure(IllegalArgumentException("Credit limit must be >= 0"))
        }

        return creditCardRepository.update(
            accountId = accountId,
            creditLimit = creditLimit,
            statementBalance = statementBalance,
            dueDate = dueDate,
        )
    }
}
