package com.petergabriel.budgetcalendar.features.notifications.domain.usecase

import com.petergabriel.budgetcalendar.features.notifications.domain.repository.IUserSettingsRepository

class UpdateUserSettingsUseCase(
    private val userSettingsRepository: IUserSettingsRepository,
) {
    suspend operator fun invoke(thresholdPercent: Int): Result<Unit> {
        if (thresholdPercent !in 1..100) {
            return Result.failure(IllegalArgumentException("Threshold percent must be between 1 and 100"))
        }

        return runCatching {
            userSettingsRepository.upsert(thresholdPercent)
        }
    }
}
