package com.petergabriel.budgetcalendar.features.notifications.domain.model

data class UserSettings(
    val id: Long,
    val stsLowThresholdPercent: Int,
    val createdAt: Long,
    val updatedAt: Long,
)
