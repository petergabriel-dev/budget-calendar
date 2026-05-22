package com.petergabriel.budgetcalendar.features.notifications.domain.usecase

import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.notifications.domain.model.Notification
import com.petergabriel.budgetcalendar.features.notifications.domain.model.NotificationType
import com.petergabriel.budgetcalendar.features.notifications.domain.repository.INotificationRepository
import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import kotlinx.datetime.TimeZone

class GenerateNotificationsUseCase(
    private val notificationRepository: INotificationRepository,
) {
    suspend operator fun invoke(
        overdueTransactions: List<Transaction>,
        daysRemaining: Int,
        projectedRollover: Long,
        currentSts: Long,
        poolBalance: Long,
        thresholdPercent: Int,
        nowMillis: Long = DateUtils.nowMillis(),
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ) {
        checkOverdueNotification(
            overdueTransactions = overdueTransactions,
            nowMillis = nowMillis,
        )

        checkRolloverNotifications(
            daysRemaining = daysRemaining,
            projectedRollover = projectedRollover,
            nowMillis = nowMillis,
            timeZone = timeZone,
        )

        checkStsNotifications(
            currentSts = currentSts,
            poolBalance = poolBalance,
            thresholdPercent = thresholdPercent,
            nowMillis = nowMillis,
        )
    }

    suspend fun checkOverdueNotification(
        overdueTransactions: List<Transaction>,
        nowMillis: Long = DateUtils.nowMillis(),
    ) {
        val overdueCount = overdueTransactions.size
        val existing = notificationRepository.getByType(NotificationType.OVERDUE_TRANSACTIONS)

        if (existing != null) {
            notificationRepository.updatePayload(
                id = existing.id,
                overdueCount = overdueCount,
                projectedRollover = null,
                daysRemaining = null,
                currentSts = null,
                poolBalance = null,
                updatedAt = nowMillis,
            )
            return
        }

        if (overdueCount <= 0) {
            return
        }

        notificationRepository.insert(
            createNotification(
                type = NotificationType.OVERDUE_TRANSACTIONS,
                nowMillis = nowMillis,
                overdueCount = overdueCount,
            ),
        )
    }

    suspend fun checkRolloverNotifications(
        daysRemaining: Int,
        projectedRollover: Long,
        nowMillis: Long = DateUtils.nowMillis(),
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ) {
        val type = when (daysRemaining) {
            7 -> NotificationType.ROLLOVER_REMINDER_7
            5 -> NotificationType.ROLLOVER_REMINDER_5
            3 -> NotificationType.ROLLOVER_REMINDER_3
            else -> null
        } ?: return

        val (year, month) = DateUtils.currentYearMonth(nowMillis, timeZone)

        val existingForMonth = notificationRepository.getByTypeAndMonth(
            type = type,
            year = year,
            month = month,
        )
        if (existingForMonth != null) {
            return
        }

        notificationRepository.insert(
            createNotification(
                type = type,
                nowMillis = nowMillis,
                triggerMonth = month,
                triggerYear = year,
                projectedRollover = projectedRollover,
                daysRemaining = daysRemaining,
            ),
        )
    }

    suspend fun checkStsNotifications(
        currentSts: Long,
        poolBalance: Long,
        thresholdPercent: Int,
        nowMillis: Long = DateUtils.nowMillis(),
    ) {
        val lowThreshold = (poolBalance * thresholdPercent) / 100L
        val shouldShowLow = currentSts < lowThreshold
        val shouldShowNegative = currentSts < 0L

        upsertSingleActiveType(
            type = NotificationType.STS_LOW,
            shouldCreateOrUpdate = shouldShowLow,
            currentSts = currentSts,
            poolBalance = poolBalance,
            nowMillis = nowMillis,
        )

        upsertSingleActiveType(
            type = NotificationType.STS_NEGATIVE,
            shouldCreateOrUpdate = shouldShowNegative,
            currentSts = currentSts,
            poolBalance = poolBalance,
            nowMillis = nowMillis,
        )
    }

    private suspend fun upsertSingleActiveType(
        type: NotificationType,
        shouldCreateOrUpdate: Boolean,
        currentSts: Long,
        poolBalance: Long,
        nowMillis: Long,
    ) {
        if (!shouldCreateOrUpdate) {
            return
        }

        val existing = notificationRepository.getByType(type)
        if (existing != null) {
            notificationRepository.updatePayload(
                id = existing.id,
                overdueCount = null,
                projectedRollover = null,
                daysRemaining = null,
                currentSts = currentSts,
                poolBalance = poolBalance,
                updatedAt = nowMillis,
            )
            return
        }

        notificationRepository.insert(
            createNotification(
                type = type,
                nowMillis = nowMillis,
                currentSts = currentSts,
                poolBalance = poolBalance,
            ),
        )
    }

    private fun createNotification(
        type: NotificationType,
        nowMillis: Long,
        triggerMonth: Int? = null,
        triggerYear: Int? = null,
        overdueCount: Int? = null,
        projectedRollover: Long? = null,
        daysRemaining: Int? = null,
        currentSts: Long? = null,
        poolBalance: Long? = null,
    ): Notification {
        return Notification(
            id = 0L,
            type = type,
            isDismissed = false,
            triggerMonth = triggerMonth,
            triggerYear = triggerYear,
            overdueCount = overdueCount,
            projectedRollover = projectedRollover,
            daysRemaining = daysRemaining,
            currentSts = currentSts,
            poolBalance = poolBalance,
            createdAt = nowMillis,
            updatedAt = nowMillis,
        )
    }
}
