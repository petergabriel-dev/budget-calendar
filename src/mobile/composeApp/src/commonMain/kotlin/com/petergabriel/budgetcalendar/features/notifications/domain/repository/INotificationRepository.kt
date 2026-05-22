package com.petergabriel.budgetcalendar.features.notifications.domain.repository

import com.petergabriel.budgetcalendar.features.notifications.domain.model.Notification
import com.petergabriel.budgetcalendar.features.notifications.domain.model.NotificationType
import kotlinx.coroutines.flow.Flow

interface INotificationRepository {
    fun getActiveNotifications(): Flow<List<Notification>>
    fun getUnreadCount(): Flow<Int>

    suspend fun getByType(type: NotificationType): Notification?
    suspend fun getByTypeAndMonth(type: NotificationType, year: Int, month: Int): Notification?

    suspend fun insert(notification: Notification): Notification
    suspend fun updatePayload(
        id: Long,
        overdueCount: Int?,
        projectedRollover: Long?,
        daysRemaining: Int?,
        currentSts: Long?,
        poolBalance: Long?,
        updatedAt: Long,
    )

    suspend fun dismiss(id: Long)
    suspend fun dismissAll()
}
