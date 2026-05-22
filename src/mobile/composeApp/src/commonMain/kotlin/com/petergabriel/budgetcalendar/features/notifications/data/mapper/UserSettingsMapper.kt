package com.petergabriel.budgetcalendar.features.notifications.data.mapper

import com.petergabriel.budgetcalendar.features.notifications.data.local.UserSettingsEntity
import com.petergabriel.budgetcalendar.features.notifications.domain.model.UserSettings

class UserSettingsMapper {
    fun toDomain(entity: UserSettingsEntity): UserSettings {
        return UserSettings(
            id = entity.id,
            stsLowThresholdPercent = entity.stsLowThresholdPercent.toInt(),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }
}
