package com.petergabriel.budgetcalendar.features.budget.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petergabriel.budgetcalendar.features.budget.domain.model.BudgetSummary
import com.petergabriel.budgetcalendar.features.budget.domain.usecase.CalculateSafeToSpendUseCase
import com.petergabriel.budgetcalendar.features.notifications.domain.usecase.GenerateNotificationsUseCase
import com.petergabriel.budgetcalendar.features.notifications.domain.usecase.GetUserSettingsUseCase
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
    private val generateNotificationsUseCase: GenerateNotificationsUseCase,
    private val getUserSettingsUseCase: GetUserSettingsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState(isLoading = true))
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    private var summaryJob: Job? = null

    init {
        observeBudgetSummary()
    }

    private fun observeBudgetSummary() {
        summaryJob?.cancel()
        summaryJob = viewModelScope.launch {
            calculateSafeToSpendUseCase()
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message,
                        )
                    }
                }
                .collect { summary ->
                    _uiState.update {
                        it.copy(
                            budgetSummary = summary,
                            isLoading = false,
                            error = null,
                        )
                    }
                    triggerReactiveStsNotifications(summary)
                }
        }
    }

    private suspend fun triggerReactiveStsNotifications(summary: BudgetSummary) {
        val thresholdPercent = runCatching {
            getUserSettingsUseCase().first()?.stsLowThresholdPercent ?: 15
        }.getOrDefault(15)

        runCatching {
            generateNotificationsUseCase.checkStsNotifications(
                currentSts = summary.availableToSpend,
                poolBalance = summary.totalLiquidAssets,
                thresholdPercent = thresholdPercent,
            )
        }.onFailure { throwable ->
            _uiState.update { state ->
                state.copy(error = throwable.message ?: state.error)
            }
        }
    }
}
