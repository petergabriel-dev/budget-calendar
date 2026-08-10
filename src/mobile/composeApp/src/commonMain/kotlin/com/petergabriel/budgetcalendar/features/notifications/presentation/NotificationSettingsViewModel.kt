package com.petergabriel.budgetcalendar.features.notifications.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petergabriel.budgetcalendar.features.notifications.domain.usecase.GetUserSettingsUseCase
import com.petergabriel.budgetcalendar.features.notifications.domain.usecase.UpdateUserSettingsUseCase
import kotlinx.coroutines.Job
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
    private var settingsJob: Job? = null

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
                saveError = null,
                didSave = false,
            )
        }
    }

    fun retry() {
        if (_uiState.value.isSaving) {
            return
        }
        _uiState.update { state -> state.copy(isLoading = true, loadError = null) }
        observeSettings()
    }

    fun save() {
        if (_uiState.value.isSaving || _uiState.value.isLoading) {
            return
        }

        val threshold = _uiState.value.thresholdInput.toIntOrNull()
        if (threshold == null || threshold !in 1..100) {
            _uiState.update { state ->
                state.copy(
                    validationError = "Please enter a threshold between 1 and 100",
                    saveError = null,
                    didSave = false,
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isSaving = true,
                    validationError = null,
                    saveError = null,
                    didSave = false,
                )
            }

            updateUserSettingsUseCase(threshold)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            isSaving = false,
                            loadError = null,
                            saveError = null,
                            didSave = true,
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update { state ->
                        state.copy(
                            isSaving = false,
                            saveError = throwable.message ?: "Unable to save threshold",
                            didSave = false,
                        )
                    }
                }
        }
    }

    private fun observeSettings() {
        settingsJob?.cancel()
        settingsJob = viewModelScope.launch {
            getUserSettingsUseCase()
                .catch { throwable ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            loadError = throwable.message ?: "Unable to load settings",
                        )
                    }
                }
                .collect { settings ->
                    _uiState.update { state ->
                        state.copy(
                            thresholdInput = (settings?.stsLowThresholdPercent ?: 15).toString(),
                            isLoading = false,
                            loadError = null,
                            validationError = null,
                        )
                    }
                }
        }
    }
}
