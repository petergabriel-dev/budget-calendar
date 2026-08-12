package com.petergabriel.budgetcalendar.features.creditcard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType
import com.petergabriel.budgetcalendar.features.accounts.domain.usecase.GetAccountsUseCase
import com.petergabriel.budgetcalendar.features.creditcard.domain.model.UpdateCreditCardSettingsRequest
import com.petergabriel.budgetcalendar.features.creditcard.domain.usecase.GetCreditCardSettingsUseCase
import com.petergabriel.budgetcalendar.features.creditcard.domain.usecase.GetCreditCardSummariesUseCase
import com.petergabriel.budgetcalendar.features.creditcard.domain.usecase.GetCreditCardSummaryByIdUseCase
import com.petergabriel.budgetcalendar.features.creditcard.domain.usecase.MakeCreditCardPaymentUseCase
import com.petergabriel.budgetcalendar.features.creditcard.domain.usecase.UpdateCreditCardSettingsUseCase
import com.petergabriel.budgetcalendar.features.transactions.domain.usecase.GetPendingAndOverdueExpensesByAccountUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    private val getAccountsUseCase: GetAccountsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreditCardUiState(isLoading = true))
    val uiState: StateFlow<CreditCardUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<CreditCardEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<CreditCardEffect> = _effects.asSharedFlow()

    private var summariesJob: Job? = null
    private var selectedCardTransactionsJob: Job? = null
    private var paymentSourceAccountsJob: Job? = null

    init {
        observeCreditCardSummaries()
        observePaymentSourceAccounts()
    }

    fun selectCard(accountId: Long) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isLoading = true,
                    error = null,
                    paymentError = null,
                    settingsError = null,
                    isPaymentSheetVisible = false,
                )
            }

            val summary = getCreditCardSummaryByIdUseCase(accountId).getOrElse { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        error = throwable.message ?: "Unable to load card",
                    )
                }
                return@launch
            }

            val settings = getCreditCardSettingsUseCase(accountId).getOrElse { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        error = throwable.message ?: "Unable to load card settings",
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
                paymentError = null,
                settingsError = null,
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
                paymentError = null,
            )
        }
    }

    fun closePaymentSheet() {
        if (_uiState.value.isPaymentSubmitting) return
        _uiState.update { state -> state.copy(isPaymentSheetVisible = false, paymentError = null) }
    }

    fun makePayment(sourceAccountId: Long, amount: Long) {
        if (_uiState.value.isPaymentSubmitting || _uiState.value.isSettingsSubmitting) return
        val selectedCardId = _uiState.value.selectedCard?.accountId ?: return
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isPaymentSubmitting = true, paymentError = null)
            }
            makeCreditCardPaymentUseCase(
                sourceAccountId = sourceAccountId,
                ccAccountId = selectedCardId,
                amount = amount,
            ).onSuccess {
                _uiState.update { state ->
                    state.copy(
                        isPaymentSubmitting = false,
                        isPaymentSheetVisible = false,
                        paymentError = null,
                    )
                }
                refreshSelectedCard(selectedCardId)
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isPaymentSubmitting = false,
                        paymentError = throwable.message ?: "Unable to make payment",
                    )
                }
            }
        }
    }

    fun updateSettings(request: UpdateCreditCardSettingsRequest) {
        if (_uiState.value.isPaymentSubmitting || _uiState.value.isSettingsSubmitting) return
        val selectedCardId = _uiState.value.selectedCard?.accountId ?: return
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isSettingsSubmitting = true, settingsError = null)
            }
            updateCreditCardSettingsUseCase(
                accountId = selectedCardId,
                creditLimit = request.creditLimit,
                statementBalance = request.statementBalance,
                dueDate = request.dueDate,
                statementClosingDay = request.statementClosingDay,
                paymentDueDay = request.paymentDueDay,
            ).onSuccess { updatedSettings ->
                _uiState.update { state ->
                    state.copy(
                        selectedCardSettings = updatedSettings,
                        isSettingsSubmitting = false,
                        settingsError = null,
                    )
                }
                _effects.tryEmit(CreditCardEffect.SettingsUpdated)
                refreshSelectedCard(selectedCardId)
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isSettingsSubmitting = false,
                        settingsError = throwable.message ?: "Unable to save card settings",
                    )
                }
            }
        }
    }

    fun retryCreditCards() {
        _uiState.update { state ->
            state.copy(
                isLoading = true,
                creditCardsError = null,
            )
        }
        observeCreditCardSummaries()
    }

    fun clearError() {
        _uiState.update {
            it.copy(
                error = null,
                creditCardsError = null,
                paymentError = null,
                settingsError = null,
            )
        }
    }

    private fun observeCreditCardSummaries() {
        summariesJob?.cancel()
        summariesJob = viewModelScope.launch {
            getCreditCardSummariesUseCase()
                .catch { throwable ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            creditCardsError = throwable.message ?: "Unable to load credit cards",
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
                            creditCardsError = null,
                        )
                    }
                }
        }
    }

    private fun observePaymentSourceAccounts() {
        paymentSourceAccountsJob?.cancel()
        paymentSourceAccountsJob = viewModelScope.launch {
            getAccountsUseCase()
                .catch { throwable ->
                    _uiState.update { state -> state.copy(error = throwable.message) }
                }
                .collect { accounts ->
                    _uiState.update { state ->
                        state.copy(
                            paymentSourceAccounts = accounts.filter { account ->
                                account.type != AccountType.CREDIT_CARD
                            },
                        )
                    }
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
