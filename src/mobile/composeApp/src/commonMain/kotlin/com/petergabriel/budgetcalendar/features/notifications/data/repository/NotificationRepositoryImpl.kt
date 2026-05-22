package com.petergabriel.budgetcalendar.features.notifications.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.petergabriel.budgetcalendar.core.database.BudgetCalendarDatabase
import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.notifications.data.local.NotificationEntity
import com.petergabriel.budgetcalendar.features.notifications.data.mapper.NotificationMapper
import com.petergabriel.budgetcalendar.features.notifications.domain.model.Notification
import com.petergabriel.budgetcalendar.features.notifications.domain.model.NotificationType
import com.petergabriel.budgetcalendar.features.notifications.domain.repository.INotificationRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NotificationRepositoryImpl(
    private val database: BudgetCalendarDatabase,
    private val notificationMapper: NotificationMapper,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : INotificationRepository {

    override fun getActiveNotifications(): Flow<List<Notification>> {
        return database.notificationsQueries
            .getActiveNotifications(::toEntity)
            .asFlow()
            .mapToList(dispatcher)
            .map { entities -> entities.map(notificationMapper::toDomain) }
    }

    override fun getUnreadCount(): Flow<Int> {
        return database.notificationsQueries
            .getUnreadCount()
            .asFlow()
            .mapToOne(dispatcher)
            .map { count -> count.toInt() }
    }

    override suspend fun getByType(type: NotificationType): Notification? {
        return database.notificationsQueries
            .getNotificationByType(type.dbValue, ::toEntity)
            .executeAsOneOrNull()
            ?.let(notificationMapper::toDomain)
    }

    override suspend fun getByTypeAndMonth(type: NotificationType, year: Int, month: Int): Notification? {
        return database.notificationsQueries
            .getNotificationByTypeAndMonth(type.dbValue, year.toLong(), month.toLong(), ::toEntity)
            .executeAsOneOrNull()
            ?.let(notificationMapper::toDomain)
    }

    override suspend fun insert(notification: Notification): Notification {
        database.notificationsQueries.insertNotification(
            type = notification.type.dbValue,
            is_dismissed = if (notification.isDismissed) 1L else 0L,
            trigger_month = notification.triggerMonth?.toLong(),
            trigger_year = notification.triggerYear?.toLong(),
            overdue_count = notification.overdueCount?.toLong(),
            projected_rollover = notification.projectedRollover,
            days_remaining = notification.daysRemaining?.toLong(),
            current_sts = notification.currentSts,
            pool_balance = notification.poolBalance,
            created_at = notification.createdAt,
            updated_at = notification.updatedAt,
        )

        val insertedId = database.notificationsQueries.getLastInsertedNotificationId().executeAsOne()
        return checkNotNull(
            database.notificationsQueries
                .getNotificationById(insertedId, ::toEntity)
                .executeAsOneOrNull()
                ?.let(notificationMapper::toDomain),
        ) {
            "Failed to load newly inserted notification"
        }
    }

    override suspend fun updatePayload(
        id: Long,
        overdueCount: Int?,
        projectedRollover: Long?,
        daysRemaining: Int?,
        currentSts: Long?,
        poolBalance: Long?,
        updatedAt: Long,
    ) {
        database.notificationsQueries.updateNotificationPayload(
            overdue_count = overdueCount?.toLong(),
            projected_rollover = projectedRollover,
            days_remaining = daysRemaining?.toLong(),
            current_sts = currentSts,
            pool_balance = poolBalance,
            updated_at = updatedAt,
            id = id,
        )
    }

    override suspend fun dismiss(id: Long) {
        database.notificationsQueries.dismissNotification(
            updated_at = DateUtils.nowMillis(),
            id = id,
        )
    }

    override suspend fun dismissAll() {
        database.notificationsQueries.dismissAllNotifications(
            updated_at = DateUtils.nowMillis(),
        )
    }

    private fun toEntity(
        id: Long,
        type: String,
        isDismissed: Long,
        triggerMonth: Long?,
        triggerYear: Long?,
        overdueCount: Long?,
        projectedRollover: Long?,
        daysRemaining: Long?,
        currentSts: Long?,
        poolBalance: Long?,
        createdAt: Long,
        updatedAt: Long,
    ): NotificationEntity {
        return NotificationEntity(
            id = id,
            type = type,
            isDismissed = isDismissed == 1L,
            triggerMonth = triggerMonth,
            triggerYear = triggerYear,
            overdueCount = overdueCount,
            projectedRollover = projectedRollover,
            daysRemaining = daysRemaining,
            currentSts = currentSts,
            poolBalance = poolBalance,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }
}
