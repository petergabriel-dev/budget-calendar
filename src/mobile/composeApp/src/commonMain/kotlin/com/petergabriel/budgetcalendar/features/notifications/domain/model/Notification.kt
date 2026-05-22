package com.petergabriel.budgetcalendar.features.notifications.domain.model

data class Notification(
    val id: Long,
    val type: NotificationType,
    val isDismissed: Boolean,
    val triggerMonth: Int?,
    val triggerYear: Int?,
    val overdueCount: Int?,
    val projectedRollover: Long?,
    val daysRemaining: Int?,
    val currentSts: Long?,
    val poolBalance: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)
