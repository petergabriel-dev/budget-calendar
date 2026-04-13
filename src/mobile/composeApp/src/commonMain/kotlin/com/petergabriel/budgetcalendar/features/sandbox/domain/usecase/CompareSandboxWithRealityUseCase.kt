package com.petergabriel.budgetcalendar.features.sandbox.domain.usecase

import com.petergabriel.budgetcalendar.features.budget.domain.usecase.CalculateSafeToSpendUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxComparison
import com.petergabriel.budgetcalendar.features.sandbox.domain.repository.ISandboxRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class CompareSandboxWithRealityUseCase(
    private val calculateSafeToSpendUseCase: CalculateSafeToSpendUseCase,
    private val getSandboxSafeToSpendUseCase: GetSandboxSafeToSpendUseCase,
    private val sandboxRepository: ISandboxRepository,
) {
    operator fun invoke(snapshotId: Long): Flow<SandboxComparison> {
        val realSafeToSpendFlow = calculateSafeToSpendUseCase().map { summary -> summary.availableToSpend }
        val sandboxSafeToSpendFlow = getSandboxSafeToSpendUseCase(snapshotId)
        val sandboxTransactionsFlow = sandboxRepository.getTransactionsBySnapshot(snapshotId)

        return combine(
            sandboxSafeToSpendFlow,
            realSafeToSpendFlow,
            sandboxTransactionsFlow,
        ) { sandboxSafeToSpend, realSafeToSpend, sandboxTransactions ->
            SandboxComparison(
                realSafeToSpend = realSafeToSpend,
                sandboxSafeToSpend = sandboxSafeToSpend,
                difference = sandboxSafeToSpend - realSafeToSpend,
                addedTransactions = sandboxTransactions.count { transaction -> transaction.originalTransactionId == null },
                promotedCount = sandboxTransactions.count { transaction -> transaction.originalTransactionId != null },
            )
        }
    }
}
