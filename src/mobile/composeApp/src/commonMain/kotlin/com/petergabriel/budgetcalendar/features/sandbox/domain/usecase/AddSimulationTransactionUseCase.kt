package com.petergabriel.budgetcalendar.features.sandbox.domain.usecase

import com.petergabriel.budgetcalendar.features.accounts.domain.repository.IAccountRepository
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.AddSandboxTransactionRequest
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxTransaction
import com.petergabriel.budgetcalendar.features.sandbox.domain.repository.ISandboxRepository
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType

class AddSimulationTransactionUseCase(
    private val sandboxRepository: ISandboxRepository,
    private val accountRepository: IAccountRepository,
) {
    suspend operator fun invoke(request: AddSandboxTransactionRequest): Result<SandboxTransaction> {
        return invoke(
            snapshotId = request.snapshotId,
            accountId = request.accountId,
            amount = request.amount,
            date = request.date,
            type = request.type,
            description = request.description,
            category = request.category,
            originalTransactionId = null,
            destinationAccountId = request.destinationAccountId,
        )
    }

    suspend operator fun invoke(
        snapshotId: Long,
        accountId: Long,
        amount: Long,
        date: Long,
        type: TransactionType,
        description: String?,
        category: String?,
        originalTransactionId: Long? = null,
        destinationAccountId: Long? = null,
    ): Result<SandboxTransaction> {
        if (amount <= 0L) {
            return Result.failure(IllegalArgumentException("Transaction amount must be greater than 0"))
        }

        val account = accountRepository.getAccountById(accountId)
            ?: return Result.failure(NoSuchElementException("Account with id=$accountId was not found"))

        if (type == TransactionType.TRANSFER) {
            val destinationId = destinationAccountId
                ?: return Result.failure(IllegalArgumentException("Destination account is required"))
            if (destinationId == accountId) {
                return Result.failure(IllegalArgumentException("Cannot transfer to the same account"))
            }
            accountRepository.getAccountById(destinationId)
                ?: return Result.failure(NoSuchElementException("Destination account with id=$destinationId was not found"))
        }

        val inserted = sandboxRepository.insertSandboxTransaction(
            snapshotId = snapshotId,
            accountId = account.id,
            amount = amount,
            date = date,
            type = type,
            status = TransactionStatus.PENDING,
            description = description?.trim()?.ifBlank { null },
            category = category?.trim()?.ifBlank { null },
            originalTransactionId = originalTransactionId,
            destinationAccountId = if (type == TransactionType.TRANSFER) destinationAccountId else null,
        )

        if (inserted.isFailure) {
            return inserted
        }

        runCatching {
            sandboxRepository.updateLastAccessed(snapshotId)
        }.onFailure { throwable ->
            return Result.failure(throwable)
        }

        return inserted
    }
}
