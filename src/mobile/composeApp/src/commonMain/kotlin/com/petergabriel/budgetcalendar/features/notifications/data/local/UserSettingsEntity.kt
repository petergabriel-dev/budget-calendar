package com.petergabriel.budgetcalendar.features.notifications.data.local

data class UserSettingsEntity(
    val id: Long,
    val stsLowThresholdPercent: Long,
    val createdAt: Long,
    val updatedAt: Long,
)
