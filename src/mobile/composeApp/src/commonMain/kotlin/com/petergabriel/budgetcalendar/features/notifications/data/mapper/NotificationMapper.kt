package com.petergabriel.budgetcalendar.features.notifications.data.mapper

import com.petergabriel.budgetcalendar.features.notifications.data.local.NotificationEntity
import com.petergabriel.budgetcalendar.features.notifications.domain.model.Notification
import com.petergabriel.budgetcalendar.features.notifications.domain.model.NotificationType

class NotificationMapper {
    fun toDomain(entity: NotificationEntity): Notification {
        return Notification(
            id = entity.id,
            type = NotificationType.fromDbValue(entity.type),
            isDismissed = entity.isDismissed,
            triggerMonth = entity.triggerMonth?.toInt(),
            triggerYear = entity.triggerYear?.toInt(),
            overdueCount = entity.overdueCount?.toInt(),
            projectedRollover = entity.projectedRollover,
            daysRemaining = entity.daysRemaining?.toInt(),
            currentSts = entity.currentSts,
            poolBalance = entity.poolBalance,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }
}
