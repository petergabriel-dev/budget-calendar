package com.petergabriel.budgetcalendar.features.sandbox.domain.usecase

import com.petergabriel.budgetcalendar.features.budget.domain.usecase.CalculateSafeToSpendUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxComparison
import com.petergabriel.budgetcalendar.features.sandbox.domain.repository.ISandboxRepository
import kotlinx.coroutines.flow.first

class CompareSandboxWithRealityUseCase(
    private val calculateSafeToSpendUseCase: CalculateSafeToSpendUseCase,
    private val getSandboxSafeToSpendUseCase: GetSandboxSafeToSpendUseCase,
    private val sandboxRepository: ISandboxRepository,
) {
    suspend operator fun invoke(snapshotId: Long): Result<SandboxComparison> {
        val realSafeToSpend = runCatching {
            calculateSafeToSpendUseCase().first().availableToSpend
        }.getOrElse { throwable ->
            return Result.failure(throwable)
        }

        val sandboxSafeToSpend = getSandboxSafeToSpendUseCase(snapshotId)
            .getOrElse { throwable -> return Result.failure(throwable) }

        val sandboxTransactions = runCatching {
            sandboxRepository.getTransactionsBySnapshot(snapshotId).first()
        }.getOrElse { throwable ->
            return Result.failure(throwable)
        }

        return Result.success(
            SandboxComparison(
                realSafeToSpend = realSafeToSpend,
                sandboxSafeToSpend = sandboxSafeToSpend,
                difference = sandboxSafeToSpend - realSafeToSpend,
                addedTransactions = sandboxTransactions.count { transaction -> transaction.originalTransactionId == null },
                promotedCount = sandboxTransactions.count { transaction -> transaction.originalTransactionId != null },
            ),
        )
    }
}

