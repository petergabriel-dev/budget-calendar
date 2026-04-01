package com.petergabriel.budgetcalendar.features.transactions.domain.usecase

import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.transactions.domain.repository.ITransactionRepository

class MarkOverdueTransactionsUseCase(
    private val transactionRepository: ITransactionRepository,
) {
    suspend operator fun invoke(nowMillis: Long = DateUtils.nowMillis()): Int {
        return transactionRepository.markOverdueTransactions(nowMillis)
    }
}
