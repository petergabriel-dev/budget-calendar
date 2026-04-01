package com.petergabriel.budgetcalendar.features.transactions.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petergabriel.budgetcalendar.features.transactions.domain.model.CreateTransactionRequest
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import com.petergabriel.budgetcalendar.features.transactions.domain.model.UpdateTransactionStatusRequest
import com.petergabriel.budgetcalendar.features.transactions.domain.usecase.CreateTransactionUseCase
import com.petergabriel.budgetcalendar.features.transactions.domain.usecase.DeleteTransactionUseCase
import com.petergabriel.budgetcalendar.features.transactions.domain.usecase.GetOverdueTransactionsUseCase
import com.petergabriel.budgetcalendar.features.transactions.domain.usecase.GetPendingTransactionsUseCase
import com.petergabriel.budgetcalendar.features.transactions.domain.usecase.GetTransactionsUseCase
import com.petergabriel.budgetcalendar.features.transactions.domain.usecase.MarkOverdueTransactionsUseCase
import com.petergabriel.budgetcalendar.features.transactions.domain.usecase.TransactionFilter
import com.petergabriel.budgetcalendar.features.transactions.domain.usecase.UpdateTransactionStatusUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TransactionViewModel(
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val getPendingTransactionsUseCase: GetPendingTransactionsUseCase,
    private val getOverdueTransactionsUseCase: GetOverdueTransactionsUseCase,
    private val updateTransactionStatusUseCase: UpdateTransactionStatusUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val markOverdueTransactionsUseCase: MarkOverdueTransactionsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TransactionUiState(isLoading = true))
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    private var observeTransactionsJob: Job? = null

    init {
        observeTransactions(TransactionFilter())
    }

    fun createTransaction(request: CreateTransactionRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            createTransactionUseCase(request)
                .onFailure { throwable -> _uiState.update { state -> state.copy(error = throwable.message) } }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun updateStatus(id: Long, request: UpdateTransactionStatusRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            updateTransactionStatusUseCase(id, request)
                .onFailure { throwable -> _uiState.update { state -> state.copy(error = throwable.message) } }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            deleteTransactionUseCase(id)
                .onFailure { throwable -> _uiState.update { state -> state.copy(error = throwable.message) } }
            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    selectedTransaction = current.selectedTransaction?.takeUnless { selected -> selected.id == id },
                )
            }
        }
    }

    fun selectTransaction(id: Long) {
        viewModelScope.launch {
            val selected = uiState.value.transactions.firstOrNull { transaction -> transaction.id == id }
            _uiState.update { it.copy(selectedTransaction = selected) }
        }
    }

    fun applyDateRangeFilter(dateRange: DateRange?) {
        val filter = TransactionFilter(
            startDate = dateRange?.startDate,
            endDate = dateRange?.endDate,
            type = uiState.value.typeFilter,
        )
        _uiState.update { it.copy(selectedDateFilter = dateRange, isLoading = true) }
        observeTransactions(filter)
    }

    fun applyTypeFilter(type: TransactionType?) {
        val dateFilter = uiState.value.selectedDateFilter
        val filter = TransactionFilter(
            startDate = dateFilter?.startDate,
            endDate = dateFilter?.endDate,
            type = type,
        )
        _uiState.update { it.copy(typeFilter = type, isLoading = true) }
        observeTransactions(filter)
    }

    fun markOverdueTransactionsOnForeground() {
        viewModelScope.launch {
            markOverdueTransactionsUseCase()
        }
    }

    fun observePendingCount(onCountUpdated: (Int) -> Unit) {
        viewModelScope.launch {
            getPendingTransactionsUseCase().collect { transactions ->
                onCountUpdated(transactions.size)
            }
        }
    }

    fun observeOverdueCount(onCountUpdated: (Int) -> Unit) {
        viewModelScope.launch {
            getOverdueTransactionsUseCase().collect { transactions ->
                onCountUpdated(transactions.size)
            }
        }
    }

    private fun observeTransactions(filter: TransactionFilter) {
        observeTransactionsJob?.cancel()
        observeTransactionsJob = viewModelScope.launch {
            getTransactionsUseCase(filter).collect { transactions ->
                _uiState.update { state ->
                    state.copy(
                        transactions = transactions,
                        isLoading = false,
                        error = null,
                        selectedTransaction = state.selectedTransaction?.let { selected ->
                            transactions.firstOrNull { transaction -> transaction.id == selected.id }
                        },
                    )
                }
            }
        }
    }
}
