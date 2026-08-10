package com.petergabriel.budgetcalendar.features.notifications.testutil

import com.petergabriel.budgetcalendar.features.notifications.domain.model.Notification
import com.petergabriel.budgetcalendar.features.notifications.domain.model.NotificationType
import com.petergabriel.budgetcalendar.features.notifications.domain.repository.INotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class FakeNotificationRepository : INotificationRepository {
    private val notifications = mutableListOf<Notification>()
    private val notificationsFlow = MutableStateFlow<List<Notification>>(emptyList())
    private var nextId = 1L
    var failReads: Boolean = false
    var failDismiss: Boolean = false
    var failDismissAll: Boolean = false

    override fun getActiveNotifications(): Flow<List<Notification>> {
        if (failReads) {
            return flow { throw IllegalStateException("notification load failed") }
        }
        return notificationsFlow.map { all ->
            all.filterNot { notification -> notification.isDismissed }
                .sortedByDescending { notification -> notification.createdAt }
        }
    }

    override fun getUnreadCount(): Flow<Int> {
        return notificationsFlow.map { all ->
            all.count { notification -> !notification.isDismissed }
        }
    }

    override suspend fun getByType(type: NotificationType): Notification? {
        return notifications.firstOrNull { notification ->
            notification.type == type && !notification.isDismissed
        }
    }

    override suspend fun getByTypeAndMonth(type: NotificationType, year: Int, month: Int): Notification? {
        return notifications.firstOrNull { notification ->
            notification.type == type &&
                notification.triggerYear == year &&
                notification.triggerMonth == month
        }
    }

    override suspend fun insert(notification: Notification): Notification {
        val inserted = notification.copy(id = nextId++)
        notifications += inserted
        emitNotifications()
        return inserted
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
        val index = notifications.indexOfFirst { notification -> notification.id == id }
        if (index < 0) {
            return
        }

        notifications[index] = notifications[index].copy(
            overdueCount = overdueCount,
            projectedRollover = projectedRollover,
            daysRemaining = daysRemaining,
            currentSts = currentSts,
            poolBalance = poolBalance,
            updatedAt = updatedAt,
        )
        emitNotifications()
    }

    override suspend fun dismiss(id: Long) {
        if (failDismiss) {
            throw IllegalStateException("notification dismiss failed")
        }
        val index = notifications.indexOfFirst { notification -> notification.id == id }
        if (index < 0) {
            return
        }

        notifications[index] = notifications[index].copy(
            isDismissed = true,
        )
        emitNotifications()
    }

    override suspend fun dismissAll() {
        if (failDismissAll) {
            throw IllegalStateException("notification clear failed")
        }
        var changed = false
        for (index in notifications.indices) {
            if (!notifications[index].isDismissed) {
                notifications[index] = notifications[index].copy(isDismissed = true)
                changed = true
            }
        }

        if (changed) {
            emitNotifications()
        }
    }

    fun allNotifications(): List<Notification> = notifications.toList()

    private fun emitNotifications() {
        notificationsFlow.value = notifications.toList()
    }
}
