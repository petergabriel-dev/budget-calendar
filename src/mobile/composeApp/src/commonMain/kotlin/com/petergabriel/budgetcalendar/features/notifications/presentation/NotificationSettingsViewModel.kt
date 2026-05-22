package com.petergabriel.budgetcalendar.features.notifications.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petergabriel.budgetcalendar.features.notifications.domain.usecase.GetUserSettingsUseCase
import com.petergabriel.budgetcalendar.features.notifications.domain.usecase.UpdateUserSettingsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotificationSettingsViewModel(
    private val getUserSettingsUseCase: GetUserSettingsUseCase,
    private val updateUserSettingsUseCase: UpdateUserSettingsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()

    init {
        observeSettings()
    }

    fun onThresholdInputChange(raw: String) {
        if (raw.any { character -> !character.isDigit() }) {
            return
        }

        _uiState.update { state ->
            state.copy(
                thresholdInput = raw.take(3),
                validationError = null,
                didSave = false,
            )
        }
    }

    fun save() {
        viewModelScope.launch {
            val threshold = uiState.value.thresholdInput.toIntOrNull()
            if (threshold == null) {
                _uiState.update { state ->
                    state.copy(
                        validationError = "Please enter a threshold between 1 and 100",
                        didSave = false,
                    )
                }
                return@launch
            }

            _uiState.update { state ->
                state.copy(
                    isSaving = true,
                    validationError = null,
                    error = null,
                    didSave = false,
                )
            }

            updateUserSettingsUseCase(threshold)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            isSaving = false,
                            didSave = true,
                            error = null,
                            validationError = null,
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update { state ->
                        state.copy(
                            isSaving = false,
                            didSave = false,
                            validationError = throwable.message,
                        )
                    }
                }
        }
    }

    fun clearSaveIndicator() {
        _uiState.update { state -> state.copy(didSave = false) }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            getUserSettingsUseCase()
                .catch { throwable ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            error = throwable.message,
                        )
                    }
                }
                .collect { settings ->
                    val threshold = settings?.stsLowThresholdPercent ?: 15
                    _uiState.update { state ->
                        state.copy(
                            thresholdInput = threshold.toString(),
                            isLoading = false,
                            isSaving = false,
                            error = null,
                            validationError = null,
                        )
                    }
                }
        }
    }
}
