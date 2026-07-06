package com.petergabriel.budgetcalendar.features.creditcard.domain.usecase

import com.petergabriel.budgetcalendar.features.creditcard.domain.repository.ICreditCardRepository
import kotlin.math.max

class CalculateReservedAmountUseCase(
    private val creditCardRepository: ICreditCardRepository,
) {
    suspend operator fun invoke(accountId: Long): Result<Long> {
        return creditCardRepository.getProjectedBalance(accountId).map { projected -> max(0L, -projected) }
    }
}
