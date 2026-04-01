package com.petergabriel.budgetcalendar.features.transactions.domain.usecase

import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import com.petergabriel.budgetcalendar.features.transactions.domain.repository.ITransactionRepository
import kotlinx.coroutines.flow.Flow

class GetPendingTransactionsUseCase(
    private val transactionRepository: ITransactionRepository,
) {
    operator fun invoke(): Flow<List<Transaction>> = transactionRepository.getPendingTransactions()
}
