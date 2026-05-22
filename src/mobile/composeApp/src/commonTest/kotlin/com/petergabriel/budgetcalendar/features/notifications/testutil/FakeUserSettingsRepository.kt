package com.petergabriel.budgetcalendar.features.notifications.testutil

import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.notifications.domain.model.UserSettings
import com.petergabriel.budgetcalendar.features.notifications.domain.repository.IUserSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeUserSettingsRepository : IUserSettingsRepository {
    private val settingsFlow = MutableStateFlow<UserSettings?>(null)
    private var writeCount = 0

    override fun getUserSettings(): Flow<UserSettings?> = settingsFlow

    override suspend fun upsert(thresholdPercent: Int) {
        val now = DateUtils.nowMillis()
        val current = settingsFlow.value
        settingsFlow.value = if (current == null) {
            UserSettings(
                id = 1L,
                stsLowThresholdPercent = thresholdPercent,
                createdAt = now,
                updatedAt = now,
            )
        } else {
            current.copy(
                stsLowThresholdPercent = thresholdPercent,
                updatedAt = now,
            )
        }
        writeCount += 1
    }

    fun getWriteCount(): Int = writeCount

    fun seed(settings: UserSettings?) {
        settingsFlow.value = settings
    }
}
