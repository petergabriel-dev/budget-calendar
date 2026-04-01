package com.petergabriel.budgetcalendar.features.creditcard.domain.usecase

import com.petergabriel.budgetcalendar.features.creditcard.domain.model.CreditCardSettings
import com.petergabriel.budgetcalendar.features.creditcard.domain.repository.ICreditCardRepository

class GetCreditCardSettingsUseCase(
    private val creditCardRepository: ICreditCardRepository,
    private val createCreditCardSettingsUseCase: CreateCreditCardSettingsUseCase,
) {
    suspend operator fun invoke(accountId: Long): Result<CreditCardSettings> {
        return creditCardRepository.getSettingsByAccountId(accountId)
            .recoverCatching { throwable ->
                if (throwable is NoSuchElementException) {
                    createCreditCardSettingsUseCase(accountId = accountId).getOrThrow()
                } else {
                    throw throwable
                }
            }
    }
}
