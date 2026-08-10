package com.petergabriel.budgetcalendar.features.sandbox.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.budget.domain.usecase.CalculateSafeToSpendUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.AddSandboxTransactionRequest
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
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
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
                    isLoadingSnapshots = true,
                    snapshotError = null,
                    mutationError = null,
                )
            }
            viewModelScope.launch {
                checkAndExpireSandboxesUseCase()
                    .onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                isLoadingSnapshots = false,
                                snapshotError = throwable.message,
                            )
                        }
                        return@launch
                    }
                loadAvailableSnapshots(autoSelect = true)
            }
            return
        }

        clearSandboxState()
    }

    fun loadAvailableSnapshots(autoSelect: Boolean = false) {
        loadAvailableSnapshots(autoSelect = autoSelect, preferredSnapshotId = null)
    }

    fun selectSnapshot(id: Long) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingProjection = true,
                    isLoadingTransactions = true,
                    snapshotError = null,
                    projectionError = null,
                    transactionError = null,
                    mutationError = null,
                )
            }

            val snapshot = runCatching { getSandboxByIdUseCase(id) }
                .getOrElse { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoadingProjection = false,
                            isLoadingTransactions = false,
                            snapshotError = throwable.message,
                        )
                    }
                    return@launch
                }
                ?: run {
                    _uiState.update {
                        it.copy(
                            isLoadingProjection = false,
                            isLoadingTransactions = false,
                            snapshotError = "Sandbox snapshot with id=$id was not found",
                        )
                    }
                    return@launch
                }

            updateSnapshotLastAccessedUseCase(id)
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoadingProjection = false,
                            isLoadingTransactions = false,
                            snapshotError = throwable.message,
                        )
                    }
                    return@launch
                }

            val snapshots = runCatching { getSandboxesUseCase().first() }
                .getOrElse { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoadingProjection = false,
                            isLoadingTransactions = false,
                            snapshotError = throwable.message,
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
                    currentDailyRate = dailyRate(resolved.initialSafeToSpend),
                    comparison = null,
                    isSnapshotSheetVisible = false,
                    snapshotError = null,
                    projectionError = null,
                    transactionError = null,
                    mutationError = null,
                )
            }

            observeSelectedSnapshot(id)
        }
    }

    fun retrySelectedSnapshot() {
        uiState.value.activeSnapshot?.id?.let(::observeSelectedSnapshot)
    }

    fun showSnapshotSheet() {
        _uiState.update { it.copy(isSnapshotSheetVisible = true, mutationError = null) }
    }

    fun hideSnapshotSheet() {
        _uiState.update { it.copy(isSnapshotSheetVisible = false) }
    }

    fun showAddTransactionSheet() {
        if (uiState.value.activeSnapshot == null) {
            _uiState.update { it.copy(mutationError = "Please select a sandbox snapshot") }
            return
        }
        _uiState.update {
            it.copy(
                isAddTransactionSheetVisible = true,
                mutationError = null,
            )
        }
    }

    fun hideAddTransactionSheet() {
        if (!_uiState.value.isAddingTransaction) {
            _uiState.update { it.copy(isAddTransactionSheetVisible = false) }
        }
    }

    fun addTransaction(request: AddSandboxTransactionRequest) {
        val snapshot = uiState.value.activeSnapshot
            ?: run {
                _uiState.update { it.copy(mutationError = "Please select a sandbox snapshot") }
                return
            }
        if (uiState.value.isAddingTransaction) {
            return
        }

        val normalizedCategory = request.category.trim()
        if (request.type != TransactionType.TRANSFER && normalizedCategory.isBlank()) {
            _uiState.update { it.copy(mutationError = "Name is required") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAddingTransaction = true,
                    mutationError = null,
                )
            }

            addSimulationTransactionUseCase(
                request.copy(
                    snapshotId = snapshot.id,
                    category = normalizedCategory,
                    description = request.description?.trim()?.ifBlank { null },
                ),
            ).onSuccess {
                _uiState.update {
                    it.copy(
                        isAddingTransaction = false,
                        isAddTransactionSheetVisible = false,
                        mutationError = null,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isAddingTransaction = false,
                        mutationError = throwable.message,
                    )
                }
            }
        }
    }

    fun removeTransaction(transactionId: Long) {
        val snapshotId = uiState.value.activeSnapshot?.id
            ?: run {
                _uiState.update { it.copy(mutationError = "Please select a sandbox snapshot") }
                return
            }
        if (transactionId in uiState.value.removingTransactionIds) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    removingTransactionIds = it.removingTransactionIds + transactionId,
                    mutationError = null,
                )
            }

            removeSimulationTransactionUseCase(snapshotId, transactionId)
                .onSuccess {
                    _uiState.update {
                        it.copy(removingTransactionIds = it.removingTransactionIds - transactionId)
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            removingTransactionIds = it.removingTransactionIds - transactionId,
                            mutationError = throwable.message,
                        )
                    }
                }
        }
    }

    fun promoteTransaction(transactionId: Long) {
        val transaction = uiState.value.sandboxTransactions.firstOrNull { item -> item.id == transactionId }
            ?: run {
                _uiState.update { it.copy(mutationError = "Sandbox transaction with id=$transactionId was not found") }
                return
            }
        if (transactionId in uiState.value.promotingTransactionIds) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    promotingTransactionIds = it.promotingTransactionIds + transactionId,
                    mutationError = null,
                )
            }
            promoteTransactionUseCase(transaction)
                .onSuccess {
                    _uiState.update {
                        it.copy(promotingTransactionIds = it.promotingTransactionIds - transactionId)
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            promotingTransactionIds = it.promotingTransactionIds - transactionId,
                            mutationError = throwable.message,
                        )
                    }
                }
        }
    }

    fun createSandbox(name: String) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank() || uiState.value.isCreatingSnapshot) {
            if (trimmedName.isBlank()) {
                _uiState.update { it.copy(mutationError = "Snapshot name is required") }
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCreatingSnapshot = true,
                    isSnapshotMutationBusy = true,
                    mutationError = null,
                )
            }

            val currentSafeToSpend = fetchCurrentSafeToSpend()
                .getOrElse { throwable ->
                    _uiState.update {
                        it.copy(
                            isCreatingSnapshot = false,
                            isSnapshotMutationBusy = false,
                            mutationError = throwable.message,
                        )
                    }
                    return@launch
                }

            createSandboxUseCase(
                name = trimmedName,
                description = null,
                currentSafeToSpend = currentSafeToSpend,
            ).onSuccess { snapshot ->
                _uiState.update { it.copy(isCreatingSnapshot = false, isSnapshotMutationBusy = false) }
                loadAvailableSnapshots(autoSelect = true, preferredSnapshotId = snapshot.id)
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isCreatingSnapshot = false,
                        isSnapshotMutationBusy = false,
                        mutationError = throwable.message,
                    )
                }
            }
        }
    }

    fun deleteSandbox(id: Long) {
        if (uiState.value.isSnapshotMutationBusy) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSnapshotMutationBusy = true,
                    mutationError = null,
                )
            }

            deleteSandboxUseCase(id)
                .onSuccess {
                    _uiState.update { it.copy(isSnapshotMutationBusy = false) }
                    loadAvailableSnapshots(autoSelect = true)
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isSnapshotMutationBusy = false,
                            mutationError = throwable.message,
                        )
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(mutationError = null) }
    }

    override fun onCleared() {
        selectedSnapshotJob?.cancel()
        selectedSnapshotJob = null
        super.onCleared()
    }

    private fun clearSandboxState() {
        selectedSnapshotJob?.cancel()
        selectedSnapshotJob = null
        _uiState.update {
            SandboxHomeUiState(isSandboxMode = false)
        }
    }

    private fun observeSelectedSnapshot(snapshotId: Long) {
        selectedSnapshotJob?.cancel()
        _uiState.update {
            it.copy(
                isLoadingProjection = true,
                isLoadingTransactions = true,
                projectionError = null,
                transactionError = null,
            )
        }
        selectedSnapshotJob = viewModelScope.launch {
            launch {
                getSandboxTransactionsUseCase(snapshotId)
                    .catch { throwable ->
                        _uiState.update {
                            it.copy(
                                isLoadingTransactions = false,
                                transactionError = throwable.message,
                            )
                        }
                    }
                    .collect { transactions ->
                        if (isCurrentSnapshot(snapshotId)) {
                            _uiState.update {
                                it.copy(
                                    sandboxTransactions = transactions,
                                    isLoadingTransactions = false,
                                    transactionError = null,
                                )
                            }
                        }
                    }
            }
            launch {
                getSandboxSafeToSpendUseCase(snapshotId)
                    .catch { throwable ->
                        _uiState.update {
                            it.copy(
                                isLoadingProjection = false,
                                projectionError = throwable.message,
                            )
                        }
                    }
                    .collect { projectedSafeToSpend ->
                        if (isCurrentSnapshot(snapshotId)) {
                            _uiState.update {
                                it.copy(
                                    projectedSafeToSpend = projectedSafeToSpend,
                                    currentDailyRate = dailyRate(projectedSafeToSpend),
                                    isLoadingProjection = false,
                                    projectionError = null,
                                )
                            }
                        }
                    }
            }
            launch {
                compareSandboxWithRealityUseCase(snapshotId)
                    .catch {
                        if (isCurrentSnapshot(snapshotId)) {
                            _uiState.update { state -> state.copy(comparison = null) }
                        }
                    }
                    .collect { comparison ->
                        if (isCurrentSnapshot(snapshotId)) {
                            _uiState.update { state -> state.copy(comparison = comparison) }
                        }
                    }
            }
        }
    }

    private fun loadAvailableSnapshots(
        autoSelect: Boolean,
        preferredSnapshotId: Long? = null,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSnapshots = true, snapshotError = null) }
            val snapshots = runCatching { getSandboxesUseCase().first() }
                .getOrElse { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoadingSnapshots = false,
                            snapshotError = throwable.message,
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
                    currentDailyRate = activeSnapshot?.let { dailyRate(it.initialSafeToSpend) } ?: 0L,
                    comparison = if (activeSnapshot == null) null else state.comparison,
                    isLoadingSnapshots = false,
                    snapshotError = null,
                )
            }

            if (!autoSelect) {
                return@launch
            }

            val idToSelect = preferredSnapshotId ?: snapshots.firstOrNull()?.id
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

    private fun isCurrentSnapshot(snapshotId: Long): Boolean {
        return uiState.value.isSandboxMode && uiState.value.activeSnapshot?.id == snapshotId
    }

    private suspend fun fetchCurrentSafeToSpend(): Result<Long> = runCatching {
        calculateSafeToSpendUseCase().first().availableToSpend
    }

    private fun dailyRate(projectedSafeToSpend: Long): Long {
        return projectedSafeToSpend / daysRemainingInMonth().coerceAtLeast(1)
    }

    private fun daysRemainingInMonth(nowMillis: Long = DateUtils.nowMillis()): Int {
        val timeZone = TimeZone.currentSystemDefault()
        val today = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(timeZone).date
        return DateUtils.daysRemainingInMonth(today = today, tz = timeZone)
    }
}
