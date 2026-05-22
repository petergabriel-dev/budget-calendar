package com.petergabriel.budgetcalendar.features.notifications.data.local

data class NotificationEntity(
    val id: Long,
    val type: String,
    val isDismissed: Boolean,
    val triggerMonth: Long?,
    val triggerYear: Long?,
    val overdueCount: Long?,
    val projectedRollover: Long?,
    val daysRemaining: Long?,
    val currentSts: Long?,
    val poolBalance: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)
