package com.petergabriel.budgetcalendar.features.transactions.domain.usecase

import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import com.petergabriel.budgetcalendar.features.transactions.domain.repository.ITransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class TransactionFilter(
    val accountId: Long? = null,
    val date: Long? = null,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val type: TransactionType? = null,
)

class GetTransactionsUseCase(
    private val transactionRepository: ITransactionRepository,
) {
    operator fun invoke(filter: TransactionFilter): Flow<List<Transaction>> {
        val flow = when {
            filter.accountId != null -> transactionRepository.getTransactionsByAccount(filter.accountId)
            filter.date != null -> transactionRepository.getTransactionsByDate(filter.date)
            filter.startDate != null && filter.endDate != null -> transactionRepository.getTransactionsByDateRange(
                filter.startDate,
                filter.endDate,
                filter.type,
            )

            else -> transactionRepository.getTransactionsByDateRange(
                startDate = Long.MIN_VALUE / 4,
                endDate = Long.MAX_VALUE / 4,
                typeFilter = filter.type,
            )
        }

        return if (filter.type == null || (filter.startDate != null && filter.endDate != null)) {
            flow
        } else {
            flow.map { transactions -> transactions.filter { transaction -> transaction.type == filter.type } }
        }
    }
}
