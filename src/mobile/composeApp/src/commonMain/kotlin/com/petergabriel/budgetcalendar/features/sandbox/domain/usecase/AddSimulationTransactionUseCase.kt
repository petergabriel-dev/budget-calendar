package com.petergabriel.budgetcalendar.features.sandbox.domain.usecase

import com.petergabriel.budgetcalendar.features.accounts.domain.repository.IAccountRepository
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxTransaction
import com.petergabriel.budgetcalendar.features.sandbox.domain.repository.ISandboxRepository
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType

class AddSimulationTransactionUseCase(
    private val sandboxRepository: ISandboxRepository,
    private val accountRepository: IAccountRepository,
) {
    suspend operator fun invoke(
        snapshotId: Long,
        accountId: Long,
        amount: Long,
        date: Long,
        type: TransactionType,
        description: String?,
        category: String?,
        originalTransactionId: Long? = null,
    ): Result<SandboxTransaction> {
        if (amount <= 0L) {
            return Result.failure(IllegalArgumentException("Transaction amount must be greater than 0"))
        }

        val account = accountRepository.getAccountById(accountId)
            ?: return Result.failure(NoSuchElementException("Account with id=$accountId was not found"))

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
