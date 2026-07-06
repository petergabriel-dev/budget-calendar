package com.petergabriel.budgetcalendar.features.sandbox.domain.usecase

import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxTransaction
import com.petergabriel.budgetcalendar.features.sandbox.domain.repository.ISandboxRepository
import com.petergabriel.budgetcalendar.features.transactions.domain.model.CreateTransactionRequest
import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import com.petergabriel.budgetcalendar.features.transactions.domain.repository.ITransactionRepository
import com.petergabriel.budgetcalendar.features.transactions.domain.usecase.CreateTransactionUseCase

class PromoteTransactionUseCase(
    private val transactionRepository: ITransactionRepository,
    private val sandboxRepository: ISandboxRepository,
    private val createTransactionUseCase: CreateTransactionUseCase,
) {
    suspend operator fun invoke(sandboxTransaction: SandboxTransaction): Result<Transaction> {
        val existingRealTransaction = sandboxTransaction.originalTransactionId
            ?.let { originalId -> transactionRepository.getTransactionById(originalId) }

        if (existingRealTransaction != null) {
            sandboxRepository.deleteSandboxTransaction(sandboxTransaction.id)
                .onFailure { throwable -> return Result.failure(throwable) }
            return Result.success(existingRealTransaction)
        }

        if (sandboxTransaction.type == TransactionType.TRANSFER) {
            val destinationAccountId = sandboxTransaction.destinationAccountId
                ?: return Result.failure(
                    IllegalArgumentException("Transfer requires a destination account"),
                )

            val request = CreateTransactionRequest(
                accountId = sandboxTransaction.accountId,
                destinationAccountId = destinationAccountId,
                amount = sandboxTransaction.amount,
                date = sandboxTransaction.date,
                type = TransactionType.TRANSFER,
                status = sandboxTransaction.status,
                description = sandboxTransaction.description,
                category = sandboxTransaction.category,
            )

            val result = createTransactionUseCase(request)
            if (result.isFailure) {
                return Result.failure(result.exceptionOrNull() ?: IllegalStateException("Transfer creation failed"))
            }

            sandboxRepository.deleteSandboxTransaction(sandboxTransaction.id)
                .onFailure { throwable -> return Result.failure(throwable) }

            return result
        }

        val promoted = runCatching {
            transactionRepository.createTransaction(
                CreateTransactionRequest(
                    accountId = sandboxTransaction.accountId,
                    amount = sandboxTransaction.amount,
                    date = sandboxTransaction.date,
                    type = sandboxTransaction.type,
                    status = sandboxTransaction.status,
                    description = sandboxTransaction.description,
                    category = sandboxTransaction.category,
                    signedAmount = sandboxTransaction.signedAmount ?: when (sandboxTransaction.type) {
                        TransactionType.INCOME -> sandboxTransaction.amount
                        TransactionType.EXPENSE -> -sandboxTransaction.amount
                        TransactionType.TRANSFER -> -sandboxTransaction.amount
                    },
                ),
            )
        }.getOrElse { throwable ->
            return Result.failure(throwable)
        }

        sandboxRepository.deleteSandboxTransaction(sandboxTransaction.id)
            .onFailure { throwable -> return Result.failure(throwable) }

        return Result.success(promoted)
    }
}