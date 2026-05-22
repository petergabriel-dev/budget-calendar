package com.petergabriel.budgetcalendar.features.notifications.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.petergabriel.budgetcalendar.core.database.BudgetCalendarDatabase
import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.notifications.data.local.UserSettingsEntity
import com.petergabriel.budgetcalendar.features.notifications.data.mapper.UserSettingsMapper
import com.petergabriel.budgetcalendar.features.notifications.domain.model.UserSettings
import com.petergabriel.budgetcalendar.features.notifications.domain.repository.IUserSettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserSettingsRepositoryImpl(
    private val database: BudgetCalendarDatabase,
    private val userSettingsMapper: UserSettingsMapper,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : IUserSettingsRepository {

    override fun getUserSettings(): Flow<UserSettings?> {
        return database.user_settingsQueries
            .getUserSettings(::toEntity)
            .asFlow()
            .mapToOneOrNull(dispatcher)
            .map { entity -> entity?.let(userSettingsMapper::toDomain) }
    }

    override suspend fun upsert(thresholdPercent: Int) {
        val now = DateUtils.nowMillis()
        database.user_settingsQueries.upsertUserSettings(
            threshold = thresholdPercent.toLong(),
            created_at = now,
            updated_at = now,
        )
    }

    private fun toEntity(
        id: Long,
        threshold: Long,
        createdAt: Long,
        updatedAt: Long,
    ): UserSettingsEntity {
        return UserSettingsEntity(
            id = id,
            stsLowThresholdPercent = threshold,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }
}
