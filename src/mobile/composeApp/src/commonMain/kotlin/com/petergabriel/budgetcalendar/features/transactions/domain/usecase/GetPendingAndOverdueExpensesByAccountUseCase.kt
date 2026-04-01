package com.petergabriel.budgetcalendar.features.transactions.domain.usecase

import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import com.petergabriel.budgetcalendar.features.transactions.domain.repository.ITransactionRepository
import kotlinx.coroutines.flow.Flow

class GetPendingAndOverdueExpensesByAccountUseCase(
    private val transactionRepository: ITransactionRepository,
) {
    operator fun invoke(accountId: Long): Flow<List<Transaction>> {
        return transactionRepository.getPendingAndOverdueExpensesByAccount(accountId)
    }
}
