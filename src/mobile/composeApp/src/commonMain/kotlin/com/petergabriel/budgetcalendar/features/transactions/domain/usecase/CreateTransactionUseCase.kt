package com.petergabriel.budgetcalendar.features.transactions.domain.usecase

import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.transactions.domain.model.CreateTransactionRequest
import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import com.petergabriel.budgetcalendar.features.transactions.domain.repository.ITransactionRepository

class CreateTransactionUseCase(
    private val transactionRepository: ITransactionRepository,
) {
    suspend operator fun invoke(request: CreateTransactionRequest): Result<Transaction> {
        if (request.amount <= 0) {
            return Result.failure(IllegalArgumentException("Transaction amount must be greater than 0"))
        }

        when (request.type) {
            TransactionType.TRANSFER -> {
                val destinationId = request.destinationAccountId
                    ?: return Result.failure(IllegalArgumentException("Transfer destination account is required"))
                if (request.accountId == destinationId) {
                    return Result.failure(IllegalArgumentException("Cannot transfer to the same account"))
                }
            }

            TransactionType.INCOME -> {
                if (DateUtils.isDateInPast(request.date)) {
                    return Result.failure(IllegalArgumentException("Cannot record income in the past"))
                }
            }

            TransactionType.EXPENSE -> {
                if (DateUtils.isMoreThanDaysInFuture(request.date, days = 30)) {
                    return Result.failure(IllegalArgumentException("Cannot schedule expenses more than 30 days ahead"))
                }
            }
        }

        return if (request.type == TransactionType.TRANSFER) {
            createTransferPair(request)
        } else {
            createSingleTransaction(request)
        }
    }

    private suspend fun createSingleTransaction(request: CreateTransactionRequest): Result<Transaction> {
        val created = transactionRepository.createTransaction(
            request.copy(
                signedAmount = when (request.type) {
                    TransactionType.INCOME -> request.amount
                    TransactionType.EXPENSE -> -request.amount
                    TransactionType.TRANSFER -> request.signedAmount
                },
            ),
        )
        return Result.success(created)
    }

    private suspend fun createTransferPair(request: CreateTransactionRequest): Result<Transaction> {
        val destinationId = checkNotNull(request.destinationAccountId)

        val source = transactionRepository.createTransaction(
            request.copy(
                linkedTransactionId = null,
                destinationAccountId = destinationId,
                signedAmount = -request.amount,
            ),
        )

        val destination = transactionRepository.createTransaction(
            request.copy(
                accountId = destinationId,
                destinationAccountId = request.accountId,
                linkedTransactionId = source.id,
                signedAmount = request.amount,
            ),
        )

        transactionRepository.updateLinkedTransactionId(source.id, destination.id)

        return Result.success(source.copy(linkedTransactionId = destination.id))
    }
}
