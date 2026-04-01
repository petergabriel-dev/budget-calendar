package com.petergabriel.budgetcalendar.features.budget.domain.usecase

import com.petergabriel.budgetcalendar.features.budget.domain.model.CreditCardReservation
import com.petergabriel.budgetcalendar.features.budget.domain.repository.IBudgetRepository
import kotlinx.coroutines.flow.Flow

class GetCreditCardReservationsUseCase(
    private val budgetRepository: IBudgetRepository,
) {
    operator fun invoke(): Flow<List<CreditCardReservation>> {
        return budgetRepository.getCreditCardReservations()
    }
}
