package com.petergabriel.budgetcalendar.features.transactions.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petergabriel.budgetcalendar.features.accounts.domain.usecase.GetAccountsUseCase
import com.petergabriel.budgetcalendar.features.transactions.domain.model.CreateTransactionRequest
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import com.petergabriel.budgetcalendar.features.transactions.domain.model.UpdateTransactionStatusRequest
import com.petergabriel.budgetcalendar.features.transactions.domain.usecase.CreateTransactionUseCase
import com.petergabriel.budgetcalendar.features.transactions.domain.usecase.DeleteTransactionUseCase
import com.petergabriel.budgetcalendar.features.transactions.domain.usecase.UpdateTransactionStatusUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TransactionFormViewModel(
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val updateTransactionStatusUseCase: UpdateTransactionStatusUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionFormUiState(isLoading = true))
    val uiState: StateFlow<TransactionFormUiState> = _uiState.asStateFlow()

    private var accountsJob: Job? = null

    init {
        loadAccounts()
    }

    fun setType(type: TransactionType) {
        _uiState.update {
            it.copy(
                selectedType = type,
                validationErrors = it.validationErrors.filterKeys { key -> key != "destinationAccountId" },
            )
        }
    }

    fun submit(request: CreateTransactionRequest) {
        val errors = validateRequest(request)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(validationErrors = errors) }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSubmitting = true,
                    error = null,
                    validationErrors = emptyMap(),
                )
            }

            createTransactionUseCase(request)
                .onSuccess {
                    _uiState.update { it.copy(isSubmitting = false, error = null, validationErrors = emptyMap()) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            error = throwable.message,
                            validationErrors = mapFailureToFieldErrors(throwable.message),
                        )
                    }
                }
        }
    }

    fun replace(existingTransactionId: Long, request: CreateTransactionRequest) {
        val errors = validateRequest(request)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(validationErrors = errors) }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSubmitting = true,
                    error = null,
                    validationErrors = emptyMap(),
                )
            }

            createTransactionUseCase(request)
                .onSuccess {
                    deleteTransactionUseCase(existingTransactionId)
                        .onSuccess {
                            _uiState.update {
                                it.copy(
                                    isSubmitting = false,
                                    error = null,
                                    validationErrors = emptyMap(),
                                )
                            }
                        }
                        .onFailure { throwable ->
                            _uiState.update {
                                it.copy(
                                    isSubmitting = false,
                                    error = throwable.message ?: "Could not finish transaction update",
                                )
                            }
                        }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            error = throwable.message,
                            validationErrors = mapFailureToFieldErrors(throwable.message),
                        )
                    }
                }
        }
    }

    fun updateStatus(id: Long, status: TransactionStatus) {
        viewModelScope.launch {
            updateTransactionStatusUseCase(
                id = id,
                request = UpdateTransactionStatusRequest(status = status),
            ).onFailure { throwable ->
                _uiState.update { it.copy(error = throwable.message) }
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            deleteTransactionUseCase(id)
                .onFailure { throwable ->
                    _uiState.update { it.copy(error = throwable.message) }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun loadAccounts() {
        accountsJob?.cancel()

        accountsJob = viewModelScope.launch {
            getAccountsUseCase()
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message,
                        )
                    }
                }
                .collect { accounts ->
                    _uiState.update {
                        it.copy(
                            availableAccounts = accounts,
                            isLoading = false,
                            error = null,
                        )
                    }
                }
        }
    }

    private fun validateRequest(request: CreateTransactionRequest): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        if (request.amount <= 0L) {
            errors["amount"] = "Amount must be greater than zero"
        }

        if (request.accountId <= 0L) {
            errors["accountId"] = "Account is required"
        }

        if (request.date <= 0L) {
            errors["date"] = "Date is required"
        }

        if (request.type == TransactionType.TRANSFER) {
            val destinationId = request.destinationAccountId
            if (destinationId == null) {
                errors["destinationAccountId"] = "Destination account is required"
            } else if (destinationId == request.accountId) {
                errors["destinationAccountId"] = "Source and destination must be different"
            }
        }

        if ((request.type == TransactionType.INCOME || request.type == TransactionType.EXPENSE) && request.category.isNullOrBlank()) {
            errors["category"] = "Category is required"
        }

        return errors
    }

    private fun mapFailureToFieldErrors(message: String?): Map<String, String> {
        if (message.isNullOrBlank()) {
            return emptyMap()
        }

        val lowered = message.lowercase()
        return when {
            "amount" in lowered -> mapOf("amount" to message)
            "destination" in lowered || "transfer" in lowered -> mapOf("destinationAccountId" to message)
            "income" in lowered || "expense" in lowered || "date" in lowered -> mapOf("date" to message)
            "category" in lowered -> mapOf("category" to message)
            else -> emptyMap()
        }
    }
}
