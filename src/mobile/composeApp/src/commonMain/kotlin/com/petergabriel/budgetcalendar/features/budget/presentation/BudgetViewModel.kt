package com.petergabriel.budgetcalendar.features.budget.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petergabriel.budgetcalendar.features.accounts.domain.usecase.GetSpendingPoolAccountsUseCase
import com.petergabriel.budgetcalendar.features.budget.domain.model.MonthlyRollover
import com.petergabriel.budgetcalendar.features.budget.domain.usecase.CalculateMonthEndRolloverUseCase
import com.petergabriel.budgetcalendar.features.budget.domain.usecase.CalculateSafeToSpendUseCase
import com.petergabriel.budgetcalendar.features.budget.domain.usecase.GetCreditCardReservationsUseCase
import com.petergabriel.budgetcalendar.features.budget.domain.usecase.GetRolloverHistoryUseCase
import com.petergabriel.budgetcalendar.features.budget.domain.usecase.SaveMonthlyRolloverUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BudgetViewModel(
    private val calculateSafeToSpendUseCase: CalculateSafeToSpendUseCase,
    private val getCreditCardReservationsUseCase: GetCreditCardReservationsUseCase,
    private val saveMonthlyRolloverUseCase: SaveMonthlyRolloverUseCase,
    private val getRolloverHistoryUseCase: GetRolloverHistoryUseCase,
    private val calculateMonthEndRolloverUseCase: CalculateMonthEndRolloverUseCase,
    private val getSpendingPoolAccountsUseCase: GetSpendingPoolAccountsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState(isLoading = true, isCalculating = true))
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    private val _rolloverHistory = MutableStateFlow<List<MonthlyRollover>>(emptyList())
    val rolloverHistory: StateFlow<List<MonthlyRollover>> = _rolloverHistory.asStateFlow()

    private var summaryJob: Job? = null
    private var spendingPoolJob: Job? = null
    private var reservationsJob: Job? = null
    private var rolloverHistoryJob: Job? = null

    init {
        observeBudgetSummary()
        observeSpendingPoolAccounts()
        observeCreditCardReservations()
        loadRolloverHistory()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCalculating = true, error = null) }
            runCatching { calculateSafeToSpendUseCase().first() }
                .onSuccess { summary ->
                    _uiState.update {
                        it.copy(
                            budgetSummary = summary,
                            isLoading = false,
                            isCalculating = false,
                            error = null,
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isCalculating = false,
                            error = throwable.message,
                        )
                    }
                }
        }
    }

    fun saveRollover(year: Int, month: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCalculating = true, error = null) }

            runCatching { calculateMonthEndRolloverUseCase(year, month) }
                .onFailure { throwable ->
                    _uiState.update { state ->
                        state.copy(
                            isCalculating = false,
                            error = throwable.message,
                        )
                    }
                    return@launch
                }
                .onSuccess { rolloverAmount ->
                    saveMonthlyRolloverUseCase(year, month, rolloverAmount)
                        .onSuccess {
                            refresh()
                            loadRolloverHistory()
                        }
                        .onFailure { throwable ->
                            _uiState.update { state ->
                                state.copy(
                                    isCalculating = false,
                                    error = throwable.message,
                                )
                            }
                        }
                }
        }
    }

    fun loadRolloverHistory() {
        rolloverHistoryJob?.cancel()
        rolloverHistoryJob = viewModelScope.launch {
            getRolloverHistoryUseCase()
                .catch { throwable ->
                    _uiState.update { state -> state.copy(error = throwable.message) }
                }
                .collect { history ->
                    _rolloverHistory.value = history
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun observeBudgetSummary() {
        summaryJob?.cancel()
        summaryJob = viewModelScope.launch {
            calculateSafeToSpendUseCase()
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isCalculating = false,
                            error = throwable.message,
                        )
                    }
                }
                .collect { summary ->
                    _uiState.update {
                        it.copy(
                            budgetSummary = summary,
                            isLoading = false,
                            isCalculating = false,
                            error = null,
                        )
                    }
                }
        }
    }

    private fun observeSpendingPoolAccounts() {
        spendingPoolJob?.cancel()
        spendingPoolJob = viewModelScope.launch {
            getSpendingPoolAccountsUseCase()
                .catch { throwable ->
                    _uiState.update { state -> state.copy(error = throwable.message) }
                }
                .collect { accounts ->
                    _uiState.update { state -> state.copy(spendingPoolAccounts = accounts) }
                }
        }
    }

    private fun observeCreditCardReservations() {
        reservationsJob?.cancel()
        reservationsJob = viewModelScope.launch {
            getCreditCardReservationsUseCase()
                .catch { throwable ->
                    _uiState.update { state -> state.copy(error = throwable.message) }
                }
                .collect { reservations ->
                    _uiState.update { state -> state.copy(creditCardReservations = reservations) }
                }
        }
    }
}
