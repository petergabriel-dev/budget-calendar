package com.petergabriel.budgetcalendar.features.transactions.domain.usecase

import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.transactions.domain.model.UpdateTransactionStatusRequest
import com.petergabriel.budgetcalendar.features.transactions.domain.repository.ITransactionRepository

class UpdateTransactionStatusUseCase(
    private val transactionRepository: ITransactionRepository,
) {
    suspend operator fun invoke(id: Long, request: UpdateTransactionStatusRequest): Result<Transaction> {
        val current = transactionRepository.getTransactionById(id)
            ?: return Result.failure(NoSuchElementException("Transaction with id=$id was not found"))

        if (!isValidTransition(current.status, request.status)) {
            return Result.failure(
                IllegalStateException("Invalid transaction status transition: ${current.status} -> ${request.status}"),
            )
        }

        val updated = transactionRepository.updateTransactionStatus(id, request.status)
            ?: return Result.failure(NoSuchElementException("Transaction with id=$id was not found after update"))

        current.linkedTransactionId?.let { linkedId ->
            transactionRepository.updateTransactionStatus(linkedId, request.status)
        }

        return Result.success(updated)
    }

    private fun isValidTransition(from: TransactionStatus, to: TransactionStatus): Boolean {
        if (from == to) return true

        return when (from) {
            TransactionStatus.PENDING -> to == TransactionStatus.OVERDUE || to == TransactionStatus.CONFIRMED || to == TransactionStatus.CANCELLED
            TransactionStatus.OVERDUE -> to == TransactionStatus.CONFIRMED || to == TransactionStatus.CANCELLED
            TransactionStatus.CONFIRMED -> false
            TransactionStatus.CANCELLED -> false
        }
    }
}
