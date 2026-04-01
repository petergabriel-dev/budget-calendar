package com.petergabriel.budgetcalendar.features.transactions.domain.usecase

import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import com.petergabriel.budgetcalendar.features.transactions.domain.repository.ITransactionRepository

class DeleteTransactionUseCase(
    private val transactionRepository: ITransactionRepository,
) {
    suspend operator fun invoke(id: Long): Result<Unit> {
        val transaction = transactionRepository.getTransactionById(id)
            ?: return Result.failure(NoSuchElementException("Transaction with id=$id was not found"))

        if (transaction.type == TransactionType.TRANSFER) {
            val linkedId = transaction.linkedTransactionId
                ?: transactionRepository.getTransactionByLinkedId(transaction.id)?.id

            linkedId?.takeIf { it != transaction.id }?.let { transactionRepository.deleteTransaction(it) }
        }

        transactionRepository.deleteTransaction(id)
        return Result.success(Unit)
    }
}
