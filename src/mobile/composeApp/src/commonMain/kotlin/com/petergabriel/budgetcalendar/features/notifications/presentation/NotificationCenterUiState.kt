package com.petergabriel.budgetcalendar.features.notifications.presentation

import com.petergabriel.budgetcalendar.features.notifications.domain.model.Notification

data class NotificationCenterUiState(
    val notifications: List<Notification> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val expandedNotificationId: Long? = null,
)
