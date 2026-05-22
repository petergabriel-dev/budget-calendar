package com.petergabriel.budgetcalendar.features.notifications.domain.usecase

import com.petergabriel.budgetcalendar.features.notifications.domain.repository.INotificationRepository

class DismissAllNotificationsUseCase(
    private val notificationRepository: INotificationRepository,
) {
    suspend operator fun invoke() {
        notificationRepository.dismissAll()
    }
}
