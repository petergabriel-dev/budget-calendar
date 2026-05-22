package com.petergabriel.budgetcalendar.features.notifications.domain.usecase

import com.petergabriel.budgetcalendar.features.notifications.domain.model.UserSettings
import com.petergabriel.budgetcalendar.features.notifications.domain.repository.IUserSettingsRepository
import kotlinx.coroutines.flow.Flow

class GetUserSettingsUseCase(
    private val userSettingsRepository: IUserSettingsRepository,
) {
    operator fun invoke(): Flow<UserSettings?> = userSettingsRepository.getUserSettings()
}
