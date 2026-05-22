package com.petergabriel.budgetcalendar.features.notifications.presentation

data class NotificationSettingsUiState(
    val thresholdInput: String = "15",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val validationError: String? = null,
    val didSave: Boolean = false,
)
