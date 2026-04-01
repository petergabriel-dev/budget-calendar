package com.petergabriel.budgetcalendar.features.creditcard.domain.usecase

import com.petergabriel.budgetcalendar.features.creditcard.domain.repository.ICreditCardRepository

class CalculateReservedAmountUseCase(
    private val creditCardRepository: ICreditCardRepository,
) {
    suspend operator fun invoke(accountId: Long): Result<Long> {
        return creditCardRepository.getReservedAmount(accountId)
    }
}
