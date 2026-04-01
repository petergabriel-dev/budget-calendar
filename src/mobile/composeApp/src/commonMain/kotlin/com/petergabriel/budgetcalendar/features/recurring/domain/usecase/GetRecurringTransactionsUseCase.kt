package com.petergabriel.budgetcalendar.features.recurring.domain.usecase

import com.petergabriel.budgetcalendar.features.recurring.domain.model.RecurringTransaction
import com.petergabriel.budgetcalendar.features.recurring.domain.repository.IRecurringTransactionRepository
import kotlinx.coroutines.flow.Flow

class GetRecurringTransactionsUseCase(
    private val recurringRepository: IRecurringTransactionRepository,
) {
    operator fun invoke(): Flow<List<RecurringTransaction>> = recurringRepository.getAll()
}
