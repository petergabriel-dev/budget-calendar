package com.petergabriel.budgetcalendar.features.notifications.domain.usecase

import com.petergabriel.budgetcalendar.features.notifications.domain.repository.INotificationRepository

class DismissNotificationUseCase(
    private val notificationRepository: INotificationRepository,
) {
    suspend operator fun invoke(id: Long) {
        notificationRepository.dismiss(id)
    }
}
