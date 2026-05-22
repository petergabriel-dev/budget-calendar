package com.petergabriel.budgetcalendar.features.notifications.domain.repository

import com.petergabriel.budgetcalendar.features.notifications.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface IUserSettingsRepository {
    fun getUserSettings(): Flow<UserSettings?>
    suspend fun upsert(thresholdPercent: Int)
}
