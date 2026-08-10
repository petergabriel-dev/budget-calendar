package com.petergabriel.budgetcalendar.features.sandbox.presentation

import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxComparison
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxSnapshot
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxTransaction

data class SandboxHomeUiState(
    val isSandboxMode: Boolean = false,
    val activeSnapshot: SandboxSnapshot? = null,
    val availableSnapshots: List<SandboxSnapshot> = emptyList(),
    val isSnapshotSheetVisible: Boolean = false,
    val sandboxTransactions: List<SandboxTransaction> = emptyList(),
    val projectedSafeToSpend: Long = 0L,
    val currentDailyRate: Long = 0L,
    val comparison: SandboxComparison? = null,
    val isAddTransactionSheetVisible: Boolean = false,
    val isLoadingSnapshots: Boolean = false,
    val isLoadingProjection: Boolean = false,
    val isLoadingTransactions: Boolean = false,
    val snapshotError: String? = null,
    val projectionError: String? = null,
    val transactionError: String? = null,
    val mutationError: String? = null,
    val isAddingTransaction: Boolean = false,
    val isCreatingSnapshot: Boolean = false,
    val isSnapshotMutationBusy: Boolean = false,
    val removingTransactionIds: Set<Long> = emptySet(),
    val promotingTransactionIds: Set<Long> = emptySet(),
)
