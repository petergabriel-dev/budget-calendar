package com.petergabriel.budgetcalendar.features.notifications.presentation

import com.petergabriel.budgetcalendar.features.notifications.domain.model.Notification

data class NotificationCenterUiState(
    val notifications: List<Notification> = emptyList(),
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val error: String? = null,
    val expandedNotificationIds: Set<Long> = emptySet(),
    val dismissingNotificationIds: Set<Long> = emptySet(),
    val isDismissingAll: Boolean = false,
)
