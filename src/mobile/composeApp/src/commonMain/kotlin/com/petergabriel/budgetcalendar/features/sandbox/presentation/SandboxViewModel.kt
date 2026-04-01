package com.petergabriel.budgetcalendar.features.sandbox.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.budget.domain.usecase.CalculateSafeToSpendUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SimulationInput
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.CreateSandboxUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.DeleteSandboxUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.GetSandboxByIdUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.GetSandboxesUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.RunSimulationUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.UpdateSnapshotLastAccessedUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class SandboxViewModel(
    private val createSandboxUseCase: CreateSandboxUseCase,
    private val getSandboxesUseCase: GetSandboxesUseCase,
    private val getSandboxByIdUseCase: GetSandboxByIdUseCase,
    private val deleteSandboxUseCase: DeleteSandboxUseCase,
    private val runSimulationUseCase: RunSimulationUseCase,
    private val updateSnapshotLastAccessedUseCase: UpdateSnapshotLastAccessedUseCase,
    private val calculateSafeToSpendUseCase: CalculateSafeToSpendUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SandboxHomeUiState())
    val uiState = _uiState.asStateFlow()

    fun setSandboxMode(enabled: Boolean) {
        if (enabled) {
            _uiState.update { it.copy(isSandboxMode = true, error = null) }
            refreshCurrentDailyRate()
            loadAvailableSnapshots()
            return
        }

        clearSimulation()
        _uiState.update {
            it.copy(
                isSandboxMode = false,
                isSnapshotSheetVisible = false,
                error = null,
            )
        }
    }

    fun loadAvailableSnapshots() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            runCatching {
                getSandboxesUseCase().first()
            }.onSuccess { snapshots ->
                var shouldResetSimulation = false
                _uiState.update { state ->
                    val keptActive = state.activeSnapshot?.id?.let { activeId ->
                        snapshots.firstOrNull { snapshot -> snapshot.id == activeId }
                    }
                    val resolvedActive = keptActive ?: snapshots.firstOrNull()
                    shouldResetSimulation = state.activeSnapshot?.id != resolvedActive?.id
                    state.copy(
                        availableSnapshots = snapshots,
                        activeSnapshot = resolvedActive,
                        projectedSafeToSpend = resolvedActive?.initialSafeToSpend ?: 0L,
                        isLoading = false,
                    )
                }
                if (shouldResetSimulation) {
                    clearSimulation()
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = throwable.message,
                    )
                }
            }
        }
    }

    fun selectSnapshot(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val snapshot = runCatching {
                getSandboxByIdUseCase(id)
            }.getOrElse { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = throwable.message,
                    )
                }
                return@launch
            } ?: run {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Sandbox snapshot with id=$id was not found",
                    )
                }
                return@launch
            }

            updateSnapshotLastAccessedUseCase(id)
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message,
                        )
                    }
                    return@launch
                }

            val snapshots = runCatching {
                getSandboxesUseCase().first()
            }.getOrElse { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = throwable.message,
                    )
                }
                return@launch
            }

            val resolved = snapshots.firstOrNull { item -> item.id == id } ?: snapshot
            _uiState.update {
                it.copy(
                    activeSnapshot = resolved,
                    availableSnapshots = snapshots,
                    projectedSafeToSpend = resolved.initialSafeToSpend,
                    isSnapshotSheetVisible = false,
                    isLoading = false,
                )
            }
            clearSimulation()
        }
    }

    fun showSnapshotSheet() {
        _uiState.update { it.copy(isSnapshotSheetVisible = true) }
    }

    fun hideSnapshotSheet() {
        _uiState.update { it.copy(isSnapshotSheetVisible = false) }
    }

    fun updateSimulationInput(input: SimulationInput) {
        _uiState.update {
            it.copy(
                simulationInput = input,
                consequencesResult = null,
                error = null,
            )
        }
    }

    fun runSimulation() {
        val state = uiState.value
        val snapshot = state.activeSnapshot
            ?: run {
                _uiState.update { it.copy(error = "Please select a sandbox snapshot") }
                return
            }

        if (state.simulationInput.purchaseName.isBlank()) {
            _uiState.update { it.copy(error = "Purchase name is required") }
            return
        }

        if (state.simulationInput.amount <= 0L) {
            _uiState.update { it.copy(error = "Amount must be greater than 0") }
            return
        }

        val result = runSimulationUseCase(
            simulationInput = state.simulationInput,
            activeSnapshot = snapshot,
            currentDailyRate = state.currentDailyRate,
            daysRemainingInMonth = daysRemainingInMonth(),
        )

        _uiState.update {
            it.copy(
                consequencesResult = result,
                error = null,
            )
        }
    }

    fun clearSimulation() {
        _uiState.update {
            it.copy(
                simulationInput = SimulationInput(
                    purchaseName = "",
                    amount = 0L,
                ),
                consequencesResult = null,
            )
        }
    }

    fun createSandbox(name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val currentSafeToSpend = fetchCurrentSafeToSpend()
                .getOrElse { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message,
                        )
                    }
                    return@launch
                }

            createSandboxUseCase(
                name = name,
                description = null,
                currentSafeToSpend = currentSafeToSpend,
            ).onSuccess { snapshot ->
                val snapshots = runCatching {
                    getSandboxesUseCase().first()
                }.getOrDefault(emptyList())

                val resolved = snapshots.firstOrNull { item -> item.id == snapshot.id } ?: snapshot
                _uiState.update {
                    it.copy(
                        availableSnapshots = snapshots,
                        activeSnapshot = resolved,
                        projectedSafeToSpend = resolved.initialSafeToSpend,
                        isSnapshotSheetVisible = false,
                        isLoading = false,
                        error = null,
                    )
                }
                clearSimulation()
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = throwable.message,
                    )
                }
            }
        }
    }

    fun deleteSandbox(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            deleteSandboxUseCase(id)
                .onSuccess {
                    val snapshots = runCatching {
                        getSandboxesUseCase().first()
                    }.getOrDefault(emptyList())

                    _uiState.update { state ->
                        val resolvedActive = state.activeSnapshot?.takeIf { snapshot -> snapshot.id != id }
                            ?: snapshots.firstOrNull()
                        state.copy(
                            availableSnapshots = snapshots,
                            activeSnapshot = resolvedActive,
                            projectedSafeToSpend = resolvedActive?.initialSafeToSpend ?: 0L,
                            isLoading = false,
                            error = null,
                        )
                    }
                    if (uiState.value.activeSnapshot?.id != id) {
                        clearSimulation()
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message,
                        )
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun refreshCurrentDailyRate() {
        viewModelScope.launch {
            val currentSafeToSpend = fetchCurrentSafeToSpend().getOrElse { throwable ->
                _uiState.update { it.copy(error = throwable.message) }
                return@launch
            }
            val daysRemaining = daysRemainingInMonth()
            val dailyRate = if (daysRemaining > 0) {
                currentSafeToSpend / daysRemaining
            } else {
                currentSafeToSpend
            }

            _uiState.update { it.copy(currentDailyRate = dailyRate) }
        }
    }

    private suspend fun fetchCurrentSafeToSpend(): Result<Long> {
        return runCatching {
            calculateSafeToSpendUseCase().first().availableToSpend
        }
    }

    private fun daysRemainingInMonth(nowMillis: Long = DateUtils.nowMillis()): Int {
        val timeZone = TimeZone.currentSystemDefault()
        val today = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(timeZone).date
        val firstDayOfMonth = LocalDate(today.year, today.month.number, 1)
        val firstDayNextMonth = firstDayOfMonth.plus(DatePeriod(months = 1))
        val remaining = firstDayNextMonth.toEpochDays() - today.toEpochDays()
        return remaining.toInt().coerceAtLeast(1)
    }
}
