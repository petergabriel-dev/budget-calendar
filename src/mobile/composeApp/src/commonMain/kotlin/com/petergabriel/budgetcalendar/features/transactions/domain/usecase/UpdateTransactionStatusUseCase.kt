package com.petergabriel.budgetcalendar.features.transactions.domain.usecase

import com.petergabriel.budgetcalendar.features.accounts.domain.repository.IAccountRepository
import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import com.petergabriel.budgetcalendar.features.transactions.domain.model.UpdateTransactionStatusRequest
import com.petergabriel.budgetcalendar.features.transactions.domain.repository.ITransactionRepository

class UpdateTransactionStatusUseCase(
    private val transactionRepository: ITransactionRepository,
    private val accountRepository: IAccountRepository,
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

        // Adjust account balance on CONFIRMED transition
        if (request.status == TransactionStatus.CONFIRMED) {
            when (current.type) {
                TransactionType.EXPENSE -> accountRepository.adjustBalance(current.accountId, -current.amount)
                TransactionType.INCOME -> accountRepository.adjustBalance(current.accountId, current.amount)
                TransactionType.TRANSFER -> {
                    // For transfer: subtract from source account, add to destination account
                    // current is the source transaction (money leaves current.accountId)
                    accountRepository.adjustBalance(current.accountId, -current.amount)
                    // linked transaction is the destination (money arrives at linked.accountId)
                    current.linkedTransactionId?.let { linkedId ->
                        val linkedTx = transactionRepository.getTransactionById(linkedId)
                        linkedTx?.let { accountRepository.adjustBalance(it.accountId, current.amount) }
                    }
                }
            }
        }

        // CANCELLED: No balance adjustment needed. Pending amounts were reserved via STS
        // but were never applied to the account balance.

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
