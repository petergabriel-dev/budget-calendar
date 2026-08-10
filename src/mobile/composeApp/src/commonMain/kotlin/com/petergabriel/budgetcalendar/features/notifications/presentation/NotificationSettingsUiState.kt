package com.petergabriel.budgetcalendar.features.notifications.presentation

data class NotificationSettingsUiState(
    val thresholdInput: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val loadError: String? = null,
    val validationError: String? = null,
    val saveError: String? = null,
    val didSave: Boolean = false,
)
