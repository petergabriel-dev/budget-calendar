package com.petergabriel.budgetcalendar.features.creditcard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petergabriel.budgetcalendar.features.accounts.domain.usecase.GetSpendingPoolAccountsUseCase
import com.petergabriel.budgetcalendar.features.creditcard.domain.model.UpdateCreditCardSettingsRequest
import com.petergabriel.budgetcalendar.features.creditcard.domain.usecase.GetCreditCardSettingsUseCase
import com.petergabriel.budgetcalendar.features.creditcard.domain.usecase.GetCreditCardSummariesUseCase
import com.petergabriel.budgetcalendar.features.creditcard.domain.usecase.GetCreditCardSummaryByIdUseCase
import com.petergabriel.budgetcalendar.features.creditcard.domain.usecase.MakeCreditCardPaymentUseCase
import com.petergabriel.budgetcalendar.features.creditcard.domain.usecase.UpdateCreditCardSettingsUseCase
import com.petergabriel.budgetcalendar.features.transactions.domain.usecase.GetPendingAndOverdueExpensesByAccountUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreditCardViewModel(
    private val getCreditCardSummariesUseCase: GetCreditCardSummariesUseCase,
    private val getCreditCardSummaryByIdUseCase: GetCreditCardSummaryByIdUseCase,
    private val getCreditCardSettingsUseCase: GetCreditCardSettingsUseCase,
    private val updateCreditCardSettingsUseCase: UpdateCreditCardSettingsUseCase,
    private val makeCreditCardPaymentUseCase: MakeCreditCardPaymentUseCase,
    private val getPendingAndOverdueExpensesByAccountUseCase: GetPendingAndOverdueExpensesByAccountUseCase,
    private val getSpendingPoolAccountsUseCase: GetSpendingPoolAccountsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreditCardUiState(isLoading = true))
    val uiState: StateFlow<CreditCardUiState> = _uiState.asStateFlow()

    private var summariesJob: Job? = null
    private var selectedCardTransactionsJob: Job? = null
    private var spendingPoolAccountsJob: Job? = null

    init {
        observeCreditCardSummaries()
        observeSpendingPoolAccounts()
    }

    fun selectCard(accountId: Long) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isLoading = true,
                    error = null,
                    isPaymentSheetVisible = false,
                )
            }

            val summary = getCreditCardSummaryByIdUseCase(accountId).getOrElse { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        error = throwable.message,
                    )
                }
                return@launch
            }

            val settings = getCreditCardSettingsUseCase(accountId).getOrElse { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        error = throwable.message,
                    )
                }
                return@launch
            }

            selectedCardTransactionsJob?.cancel()
            selectedCardTransactionsJob = viewModelScope.launch {
                getPendingAndOverdueExpensesByAccountUseCase(accountId)
                    .catch { throwable ->
                        _uiState.update { state -> state.copy(error = throwable.message) }
                    }
                    .collect { transactions ->
                        _uiState.update { state ->
                            if (state.selectedCard?.accountId != accountId) {
                                state
                            } else {
                                state.copy(selectedCardTransactions = transactions)
                            }
                        }
                    }
            }

            _uiState.update { state ->
                state.copy(
                    selectedCard = summary,
                    selectedCardSettings = settings,
                    suggestedPaymentAmount = summary.reservedAmount,
                    selectedCardTransactions = emptyList(),
                    isLoading = false,
                    error = null,
                )
            }
        }
    }

    fun dismissSelectedCard() {
        selectedCardTransactionsJob?.cancel()
        _uiState.update { state ->
            state.copy(
                selectedCard = null,
                selectedCardSettings = null,
                selectedCardTransactions = emptyList(),
                isPaymentSheetVisible = false,
                suggestedPaymentAmount = 0L,
                error = null,
            )
        }
    }

    fun openPaymentSheet() {
        _uiState.update { state ->
            val selectedCard = state.selectedCard ?: return@update state
            state.copy(
                isPaymentSheetVisible = true,
                suggestedPaymentAmount = selectedCard.reservedAmount,
            )
        }
    }

    fun closePaymentSheet() {
        _uiState.update { state -> state.copy(isPaymentSheetVisible = false) }
    }

    fun makePayment(sourceAccountId: Long, amount: Long) {
        val selectedCardId = _uiState.value.selectedCard?.accountId ?: return
        viewModelScope.launch {
            _uiState.update { state -> state.copy(isLoading = true, error = null) }
            makeCreditCardPaymentUseCase(
                sourceAccountId = sourceAccountId,
                ccAccountId = selectedCardId,
                amount = amount,
            ).onSuccess {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        isPaymentSheetVisible = false,
                        error = null,
                    )
                }
                refreshSelectedCard(selectedCardId)
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        error = throwable.message,
                    )
                }
            }
        }
    }

    fun updateSettings(request: UpdateCreditCardSettingsRequest) {
        val selectedCardId = _uiState.value.selectedCard?.accountId ?: return
        viewModelScope.launch {
            _uiState.update { state -> state.copy(isLoading = true, error = null) }
            updateCreditCardSettingsUseCase(
                accountId = selectedCardId,
                creditLimit = request.creditLimit,
                statementBalance = request.statementBalance,
                dueDate = request.dueDate,
            ).onSuccess { updatedSettings ->
                _uiState.update { state ->
                    state.copy(
                        selectedCardSettings = updatedSettings,
                        isLoading = false,
                        error = null,
                    )
                }
                refreshSelectedCard(selectedCardId)
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        error = throwable.message,
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { state -> state.copy(error = null) }
    }

    private fun observeCreditCardSummaries() {
        summariesJob?.cancel()
        summariesJob = viewModelScope.launch {
            getCreditCardSummariesUseCase()
                .catch { throwable ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            error = throwable.message,
                        )
                    }
                }
                .collect { summaries ->
                    _uiState.update { state ->
                        val selectedId = state.selectedCard?.accountId
                        val resolvedSelected = selectedId?.let { id ->
                            summaries.firstOrNull { summary -> summary.accountId == id }
                        }
                        val selectedStillExists = resolvedSelected != null
                        state.copy(
                            creditCards = summaries,
                            selectedCard = resolvedSelected,
                            selectedCardSettings = state.selectedCardSettings.takeIf { selectedStillExists },
                            selectedCardTransactions = state.selectedCardTransactions.takeIf { selectedStillExists } ?: emptyList(),
                            isPaymentSheetVisible = state.isPaymentSheetVisible && selectedStillExists,
                            isLoading = false,
                            error = null,
                        )
                    }
                }
        }
    }

    private fun observeSpendingPoolAccounts() {
        spendingPoolAccountsJob?.cancel()
        spendingPoolAccountsJob = viewModelScope.launch {
            getSpendingPoolAccountsUseCase()
                .catch { throwable ->
                    _uiState.update { state -> state.copy(error = throwable.message) }
                }
                .collect { accounts ->
                    _uiState.update { state -> state.copy(spendingPoolAccounts = accounts) }
                }
        }
    }

    private suspend fun refreshSelectedCard(accountId: Long) {
        val summary = getCreditCardSummaryByIdUseCase(accountId).getOrNull()
        val settings = getCreditCardSettingsUseCase(accountId).getOrNull()
        if (summary != null || settings != null) {
            _uiState.update { state ->
                state.copy(
                    selectedCard = summary ?: state.selectedCard,
                    selectedCardSettings = settings ?: state.selectedCardSettings,
                    suggestedPaymentAmount = (summary ?: state.selectedCard)?.reservedAmount ?: state.suggestedPaymentAmount,
                )
            }
        }
    }
}
