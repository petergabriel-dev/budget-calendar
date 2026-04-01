package com.petergabriel.budgetcalendar.features.sandbox.presentation

import com.petergabriel.budgetcalendar.features.sandbox.domain.model.ConsequencesResult
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxSnapshot
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SimulationInput

data class SandboxHomeUiState(
    val isSandboxMode: Boolean = false,
    val activeSnapshot: SandboxSnapshot? = null,
    val availableSnapshots: List<SandboxSnapshot> = emptyList(),
    val isSnapshotSheetVisible: Boolean = false,
    val projectedSafeToSpend: Long = 0L,
    val currentDailyRate: Long = 0L,
    val simulationInput: SimulationInput = SimulationInput(
        purchaseName = "",
        amount = 0L,
    ),
    val consequencesResult: ConsequencesResult? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)
