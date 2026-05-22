package com.petergabriel.budgetcalendar.features.notifications.domain.usecase

import com.petergabriel.budgetcalendar.features.notifications.domain.repository.INotificationRepository
import kotlinx.coroutines.flow.Flow

class GetUnreadNotificationCountUseCase(
    private val notificationRepository: INotificationRepository,
) {
    operator fun invoke(): Flow<Int> = notificationRepository.getUnreadCount()
}
