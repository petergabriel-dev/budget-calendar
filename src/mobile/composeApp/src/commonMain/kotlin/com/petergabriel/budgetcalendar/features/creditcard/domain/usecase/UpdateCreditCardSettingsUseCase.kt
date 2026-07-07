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
        statementClosingDay: Int? = null,
        paymentDueDay: Int? = null,
    ): Result<CreditCardSettings> {
        val dayValidationError = validateCycleDays(statementClosingDay, paymentDueDay)
        if (dayValidationError != null) return Result.failure(dayValidationError)

        if (creditLimit != null && creditLimit < 0L) {
            return Result.failure(IllegalArgumentException("Credit limit must be >= 0"))
        }

        return creditCardRepository.update(
            accountId = accountId,
            creditLimit = creditLimit,
            statementBalance = statementBalance,
            dueDate = dueDate,
            statementClosingDay = statementClosingDay,
            paymentDueDay = paymentDueDay,
        )
    }

    private fun validateCycleDays(statementClosingDay: Int?, paymentDueDay: Int?): IllegalArgumentException? {
        val invalidDay = listOfNotNull(statementClosingDay, paymentDueDay).firstOrNull { day -> day !in 1..31 }
        return if (invalidDay == null) null else IllegalArgumentException("Statement cycle days must be 1-31")
    }
}
