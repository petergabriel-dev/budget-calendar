package com.petergabriel.budgetcalendar.features.sandbox.presentation

import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxComparison
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxSnapshot
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxTransaction

data class SandboxUiState(
    val snapshots: List<SandboxSnapshot> = emptyList(),
    val activeSnapshot: SandboxSnapshot? = null,
    val sandboxTransactions: List<SandboxTransaction> = emptyList(),
    val sandboxSafeToSpend: Long = 0L,
    val comparison: SandboxComparison? = null,
    val isLoading: Boolean = false,
    val isComparing: Boolean = false,
    val error: String? = null,
)

