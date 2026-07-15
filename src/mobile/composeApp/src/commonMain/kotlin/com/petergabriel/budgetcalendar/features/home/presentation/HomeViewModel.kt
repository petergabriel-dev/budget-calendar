package com.petergabriel.budgetcalendar.features.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petergabriel.budgetcalendar.features.creditcard.domain.model.CreditCardSummary
import com.petergabriel.budgetcalendar.features.creditcard.domain.usecase.GetCreditCardSummariesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getCreditCardSummariesUseCase: GetCreditCardSummariesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getCreditCardSummariesUseCase()
                .map(::totalAvailableCreditForSpending)
                .catch {
                    _uiState.update { state -> state.copy(availableCreditForSpending = null) }
                }
                .collect { availableCreditForSpending ->
                    _uiState.update { state -> state.copy(availableCreditForSpending = availableCreditForSpending) }
                }
        }
    }
}

private fun totalAvailableCreditForSpending(summaries: List<CreditCardSummary>): Long? {
    val configuredAvailableCredit = summaries.mapNotNull { summary -> summary.availableCredit }
    return configuredAvailableCredit
        .takeIf { it.isNotEmpty() }
        ?.sumOf { availableCredit -> availableCredit.coerceAtLeast(0L) }
}
