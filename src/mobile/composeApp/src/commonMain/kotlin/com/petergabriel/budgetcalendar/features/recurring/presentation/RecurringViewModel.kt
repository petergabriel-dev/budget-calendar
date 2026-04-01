package com.petergabriel.budgetcalendar.features.recurring.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petergabriel.budgetcalendar.features.accounts.domain.usecase.GetAccountsUseCase
import com.petergabriel.budgetcalendar.features.recurring.domain.model.CreateRecurringTransactionRequest
import com.petergabriel.budgetcalendar.features.recurring.domain.model.RecurringTransaction
import com.petergabriel.budgetcalendar.features.recurring.domain.model.UpdateRecurringTransactionRequest
import com.petergabriel.budgetcalendar.features.recurring.domain.usecase.CreateRecurringTransactionUseCase
import com.petergabriel.budgetcalendar.features.recurring.domain.usecase.DeleteRecurringTransactionUseCase
import com.petergabriel.budgetcalendar.features.recurring.domain.usecase.GenerateMonthlyTransactionsUseCase
import com.petergabriel.budgetcalendar.features.recurring.domain.usecase.GetRecurringTransactionsUseCase
import com.petergabriel.budgetcalendar.features.recurring.domain.usecase.GetUpcomingGeneratedTransactionsUseCase
import com.petergabriel.budgetcalendar.features.recurring.domain.usecase.ToggleRecurringActiveUseCase
import com.petergabriel.budgetcalendar.features.recurring.domain.usecase.UpdateRecurringTransactionUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RecurringViewModel(
    private val createRecurringTransactionUseCase: CreateRecurringTransactionUseCase,
    private val getRecurringTransactionsUseCase: GetRecurringTransactionsUseCase,
    private val updateRecurringTransactionUseCase: UpdateRecurringTransactionUseCase,
    private val toggleRecurringActiveUseCase: ToggleRecurringActiveUseCase,
    private val deleteRecurringTransactionUseCase: DeleteRecurringTransactionUseCase,
    private val generateMonthlyTransactionsUseCase: GenerateMonthlyTransactionsUseCase,
    private val getUpcomingGeneratedTransactionsUseCase: GetUpcomingGeneratedTransactionsUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecurringUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private var recurringJob: Job? = null
    private var upcomingJob: Job? = null
    private var accountsJob: Job? = null

    init {
        loadRecurring()
    }

    fun loadRecurring() {
        recurringJob?.cancel()
        upcomingJob?.cancel()
        accountsJob?.cancel()

        _uiState.update { it.copy(isLoading = true, error = null) }

        recurringJob = viewModelScope.launch {
            getRecurringTransactionsUseCase()
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message,
                        )
                    }
                }
                .collect { recurringTransactions ->
                    _uiState.update {
                        it.copy(
                            recurringTransactions = recurringTransactions,
                            isLoading = false,
                            error = null,
                        )
                    }
                }
        }

        upcomingJob = viewModelScope.launch {
            getUpcomingGeneratedTransactionsUseCase(monthsAhead = 3)
                .catch { throwable ->
                    _uiState.update { it.copy(error = throwable.message) }
                }
                .collect { upcoming ->
                    _uiState.update { it.copy(upcomingGenerated = upcoming) }
                }
        }

        accountsJob = viewModelScope.launch {
            getAccountsUseCase()
                .catch { throwable ->
                    _uiState.update { it.copy(error = throwable.message) }
                }
                .collect { accounts ->
                    _uiState.update { it.copy(availableAccounts = accounts) }
                }
        }
    }

    fun createRecurring(request: CreateRecurringTransactionRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            createRecurringTransactionUseCase(request)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showForm = false,
                            editingRecurring = null,
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

    fun updateRecurring(id: Long, request: UpdateRecurringTransactionRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            updateRecurringTransactionUseCase(id, request)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showForm = false,
                            editingRecurring = null,
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

    fun toggleActive(id: Long, isActive: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            toggleRecurringActiveUseCase(id, isActive)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
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

    fun deleteRecurring(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            deleteRecurringTransactionUseCase(id)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showForm = false,
                            editingRecurring = null,
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

    fun generateMonthly() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            generateMonthlyTransactionsUseCase()
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
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

    fun showCreateForm() {
        _uiState.update {
            it.copy(
                showForm = true,
                editingRecurring = null,
            )
        }
    }

    fun showEditForm(recurring: RecurringTransaction) {
        _uiState.update {
            it.copy(
                showForm = true,
                editingRecurring = recurring,
            )
        }
    }

    fun hideForm() {
        _uiState.update {
            it.copy(
                showForm = false,
                editingRecurring = null,
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
