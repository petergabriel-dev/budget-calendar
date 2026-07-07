package com.petergabriel.budgetcalendar.features.accounts.domain.usecase

import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account
import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType
import com.petergabriel.budgetcalendar.features.accounts.domain.model.CreateAccountRequest
import com.petergabriel.budgetcalendar.features.accounts.domain.repository.IAccountRepository
import com.petergabriel.budgetcalendar.features.creditcard.domain.usecase.CreateCreditCardSettingsUseCase

class CreateAccountUseCase(
    private val accountRepository: IAccountRepository,
    private val createCreditCardSettingsUseCase: CreateCreditCardSettingsUseCase? = null,
) {
    suspend operator fun invoke(request: CreateAccountRequest): Result<Account> {
        val trimmedName = request.name.trim()
        if (trimmedName.isEmpty() || trimmedName.length > 50) {
            return Result.failure(IllegalArgumentException("Account name must be 1-50 characters"))
        }

        if (request.type != AccountType.CREDIT_CARD && request.initialBalance < 0) {
            return Result.failure(IllegalArgumentException("Asset accounts cannot have negative initial balance"))
        }

        val cardSettingsUseCase = if (request.type == AccountType.CREDIT_CARD) {
            createCreditCardSettingsUseCase
                ?: return Result.failure(IllegalStateException("Credit card settings use case is required"))
        } else {
            null
        }

        if (request.type == AccountType.CREDIT_CARD) {
            if (request.creditLimit == null || request.creditLimit <= 0L) {
                return Result.failure(IllegalArgumentException("Credit card limit must be greater than 0"))
            }
            if (request.statementClosingDay !in 1..31 || request.paymentDueDay !in 1..31) {
                return Result.failure(IllegalArgumentException("Credit card cycle days must be 1-31"))
            }
        }

        val createdAccount = accountRepository.createAccount(
            request.copy(
                name = trimmedName,
                isInSpendingPool = if (request.type == AccountType.CREDIT_CARD) {
                    false
                } else {
                    request.isInSpendingPool
                },
            ),
        )

        if (createdAccount.type == AccountType.CREDIT_CARD) {
            val settingsUseCase = cardSettingsUseCase
                ?: error("Credit card settings use case was validated before account creation")
            settingsUseCase(
                accountId = createdAccount.id,
                creditLimit = request.creditLimit,
                statementClosingDay = request.statementClosingDay,
                paymentDueDay = request.paymentDueDay,
            ).onFailure { throwable ->
                accountRepository.deleteAccount(createdAccount.id)
                return Result.failure(throwable)
            }
        }

        return Result.success(createdAccount)
    }
}
