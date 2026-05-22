package com.petergabriel.budgetcalendar.features.notifications.domain.usecase

import com.petergabriel.budgetcalendar.features.notifications.domain.model.Notification
import com.petergabriel.budgetcalendar.features.notifications.domain.repository.INotificationRepository
import kotlinx.coroutines.flow.Flow

class GetActiveNotificationsUseCase(
    private val notificationRepository: INotificationRepository,
) {
    operator fun invoke(): Flow<List<Notification>> = notificationRepository.getActiveNotifications()
}
