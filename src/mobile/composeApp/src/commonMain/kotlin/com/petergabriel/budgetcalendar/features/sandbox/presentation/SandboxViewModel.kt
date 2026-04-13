package com.petergabriel.budgetcalendar.features.sandbox.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.budget.domain.usecase.CalculateSafeToSpendUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.AddSandboxTransactionRequest
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxSnapshot
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxTransaction
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.AddSimulationTransactionUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.CheckAndExpireSandboxesUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.CompareSandboxWithRealityUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.CreateSandboxUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.DeleteSandboxUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.GetSandboxByIdUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.GetSandboxSafeToSpendUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.GetSandboxTransactionsUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.GetSandboxesUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.PromoteTransactionUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.RemoveSimulationTransactionUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.UpdateSnapshotLastAccessedUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class SandboxViewModel(
    private val createSandboxUseCase: CreateSandboxUseCase,
    private val getSandboxesUseCase: GetSandboxesUseCase,
    private val getSandboxByIdUseCase: GetSandboxByIdUseCase,
    private val deleteSandboxUseCase: DeleteSandboxUseCase,
    private val addSimulationTransactionUseCase: AddSimulationTransactionUseCase,
    private val removeSimulationTransactionUseCase: RemoveSimulationTransactionUseCase,
    private val getSandboxTransactionsUseCase: GetSandboxTransactionsUseCase,
    private val getSandboxSafeToSpendUseCase: GetSandboxSafeToSpendUseCase,
    private val compareSandboxWithRealityUseCase: CompareSandboxWithRealityUseCase,
    private val promoteTransactionUseCase: PromoteTransactionUseCase,
    private val updateSnapshotLastAccessedUseCase: UpdateSnapshotLastAccessedUseCase,
    private val checkAndExpireSandboxesUseCase: CheckAndExpireSandboxesUseCase,
    private val calculateSafeToSpendUseCase: CalculateSafeToSpendUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SandboxHomeUiState())
    val uiState = _uiState.asStateFlow()
    private var selectedSnapshotJob: Job? = null

    fun setSandboxMode(enabled: Boolean) {
        if (enabled) {
            _uiState.update {
                it.copy(
                    isSandboxMode = true,
                    isLoading = true,
                    error = null,
                )
            }
            viewModelScope.launch {
                checkAndExpireSandboxesUseCase()
                    .onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = throwable.message,
                            )
                        }
                        return@launch
                    }
                loadAvailableSnapshots(autoSelect = true)
            }
            return
        }

        selectedSnapshotJob?.cancel()
        selectedSnapshotJob = null
        _uiState.update {
            it.copy(
                isSandboxMode = false,
                isSnapshotSheetVisible = false,
                isAddTransactionSheetVisible = false,
                activeSnapshot = null,
                sandboxTransactions = emptyList(),
                projectedSafeToSpend = 0L,
                currentDailyRate = 0L,
                comparison = null,
                isLoading = false,
                error = null,
            )
        }
    }

    fun loadAvailableSnapshots(autoSelect: Boolean = false) {
        loadAvailableSnapshots(
            autoSelect = autoSelect,
            preferredSnapshotId = null,
        )
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
                    sandboxTransactions = emptyList(),
                    projectedSafeToSpend = resolved.initialSafeToSpend,
                    comparison = null,
                    isSnapshotSheetVisible = false,
                    isLoading = false,
                    error = null,
                )
            }

            observeSelectedSnapshot(id)
        }
    }

    fun showSnapshotSheet() {
        _uiState.update { it.copy(isSnapshotSheetVisible = true) }
    }

    fun hideSnapshotSheet() {
        _uiState.update { it.copy(isSnapshotSheetVisible = false) }
    }

    fun showAddTransactionSheet() {
        _uiState.update {
            it.copy(
                isAddTransactionSheetVisible = true,
                error = null,
            )
        }
    }

    fun hideAddTransactionSheet() {
        _uiState.update { it.copy(isAddTransactionSheetVisible = false) }
    }

    fun addTransaction(request: AddSandboxTransactionRequest) {
        val snapshot = uiState.value.activeSnapshot
            ?: run {
                _uiState.update { it.copy(error = "Please select a sandbox snapshot") }
                return
            }

        val normalizedCategory = request.category.trim()
        if (normalizedCategory.isBlank()) {
            _uiState.update { it.copy(error = "Name is required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            addSimulationTransactionUseCase(
                request.copy(
                    snapshotId = snapshot.id,
                    category = normalizedCategory,
                    description = request.description?.trim()?.ifBlank { null },
                ),
            ).onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAddTransactionSheetVisible = false,
                        error = null,
                    )
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

    fun removeTransaction(transactionId: Long) {
        val snapshotId = uiState.value.activeSnapshot?.id
            ?: run {
                _uiState.update { it.copy(error = "Please select a sandbox snapshot") }
                return
            }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            removeSimulationTransactionUseCase(snapshotId, transactionId)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = null,
                        )
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

    fun promoteTransaction(transactionId: Long) {
        val transaction = uiState.value.sandboxTransactions.firstOrNull { item -> item.id == transactionId }
            ?: run {
                _uiState.update { it.copy(error = "Sandbox transaction with id=$transactionId was not found") }
                return
            }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            promoteTransactionUseCase(transaction)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = null,
                        )
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
                loadAvailableSnapshots(
                    autoSelect = true,
                    preferredSnapshotId = snapshot.id,
                )
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
                    loadAvailableSnapshots(autoSelect = true)
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

    override fun onCleared() {
        selectedSnapshotJob?.cancel()
        selectedSnapshotJob = null
        super.onCleared()
    }

    private fun observeSelectedSnapshot(snapshotId: Long) {
        selectedSnapshotJob?.cancel()
        selectedSnapshotJob = viewModelScope.launch {
            combine(
                getSandboxTransactionsUseCase(snapshotId),
                getSandboxSafeToSpendUseCase(snapshotId),
                compareSandboxWithRealityUseCase(snapshotId),
            ) { transactions, projectedSafeToSpend, comparison ->
                Triple(transactions, projectedSafeToSpend, comparison)
            }
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message,
                        )
                    }
                }
                .collect { (transactions, projectedSafeToSpend, comparison) ->
                    if (!uiState.value.isSandboxMode || uiState.value.activeSnapshot?.id != snapshotId) {
                        return@collect
                    }

                    val dailyRate = projectedSafeToSpend / daysRemainingInMonth()
                    _uiState.update {
                        it.copy(
                            sandboxTransactions = transactions,
                            projectedSafeToSpend = projectedSafeToSpend,
                            comparison = comparison,
                            currentDailyRate = dailyRate,
                            isLoading = false,
                            error = null,
                        )
                    }
                }
        }
    }

    private fun loadAvailableSnapshots(
        autoSelect: Boolean,
        preferredSnapshotId: Long?,
    ) {
        viewModelScope.launch {
            val snapshots = runCatching { getSandboxesUseCase().first() }
                .getOrElse { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message,
                        )
                    }
                    return@launch
                }

            _uiState.update { state ->
                val activeSnapshot = preferredSnapshotId?.let { id ->
                    snapshots.firstOrNull { snapshot -> snapshot.id == id }
                } ?: state.activeSnapshot?.id?.let { activeId ->
                    snapshots.firstOrNull { snapshot -> snapshot.id == activeId }
                }

                state.copy(
                    availableSnapshots = snapshots,
                    activeSnapshot = activeSnapshot,
                    sandboxTransactions = if (activeSnapshot == null) emptyList() else state.sandboxTransactions,
                    projectedSafeToSpend = activeSnapshot?.initialSafeToSpend ?: 0L,
                    comparison = if (activeSnapshot == null) null else state.comparison,
                    isLoading = false,
                    error = null,
                )
            }

            if (!autoSelect) {
                return@launch
            }

            val idToSelect = preferredSnapshotId
                ?: snapshots.firstOrNull()?.id

            if (idToSelect == null) {
                selectedSnapshotJob?.cancel()
                selectedSnapshotJob = null
                _uiState.update {
                    it.copy(
                        activeSnapshot = null,
                        sandboxTransactions = emptyList(),
                        projectedSafeToSpend = 0L,
                        currentDailyRate = 0L,
                        comparison = null,
                        isSnapshotSheetVisible = false,
                        isAddTransactionSheetVisible = false,
                    )
                }
                return@launch
            }

            selectSnapshot(idToSelect)
        }
    }

    private suspend fun fetchCurrentSafeToSpend(): Result<Long> = runCatching {
        calculateSafeToSpendUseCase().first().availableToSpend
    }

    private fun daysRemainingInMonth(nowMillis: Long = DateUtils.nowMillis()): Int {
        val timeZone = TimeZone.currentSystemDefault()
        val today = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(timeZone).date
        return DateUtils.daysRemainingInMonth(today = today, tz = timeZone)
    }
}
