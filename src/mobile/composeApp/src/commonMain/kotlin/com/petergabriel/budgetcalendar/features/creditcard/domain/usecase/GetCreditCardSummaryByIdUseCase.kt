package com.petergabriel.budgetcalendar.features.creditcard.domain.usecase

import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType
import com.petergabriel.budgetcalendar.features.accounts.domain.repository.IAccountRepository
import com.petergabriel.budgetcalendar.features.creditcard.domain.model.CreditCardSummary
import com.petergabriel.budgetcalendar.features.creditcard.domain.repository.ICreditCardRepository
import kotlin.math.abs
import kotlin.math.max

class GetCreditCardSummaryByIdUseCase(
    private val getCreditCardSettingsUseCase: GetCreditCardSettingsUseCase,
    private val accountRepository: IAccountRepository,
    private val creditCardRepository: ICreditCardRepository,
) {
    suspend operator fun invoke(accountId: Long): Result<CreditCardSummary> {
        val settings = getCreditCardSettingsUseCase(accountId).getOrElse { throwable ->
            return Result.failure(throwable)
        }

        val account = accountRepository.getAccountById(accountId)
            ?: return Result.failure(NoSuchElementException("Account with id=$accountId was not found"))
        if (account.type != AccountType.CREDIT_CARD) {
            return Result.failure(IllegalArgumentException("Account with id=$accountId is not a credit card"))
        }

        val projected = creditCardRepository.getProjectedBalance(accountId).getOrElse { throwable ->
            return Result.failure(throwable)
        }

        return Result.success(
            CreditCardSummary(
                accountId = account.id,
                accountName = account.name,
                currentBalance = projected,
                reservedAmount = max(0L, -projected),
                statementBalance = settings.statementBalance,
                creditLimit = settings.creditLimit,
                availableCredit = settings.creditLimit?.minus(abs(projected)),
                dueDate = settings.dueDate,
            ),
        )
    }
}
