package com.petergabriel.budgetcalendar.features.notifications.presentation

import com.petergabriel.budgetcalendar.features.notifications.domain.model.Notification
import com.petergabriel.budgetcalendar.features.notifications.domain.model.NotificationType
import com.petergabriel.budgetcalendar.features.notifications.domain.usecase.DismissAllNotificationsUseCase
import com.petergabriel.budgetcalendar.features.notifications.domain.usecase.DismissNotificationUseCase
import com.petergabriel.budgetcalendar.features.notifications.domain.usecase.GetActiveNotificationsUseCase
import com.petergabriel.budgetcalendar.features.notifications.domain.usecase.GetUnreadNotificationCountUseCase
import com.petergabriel.budgetcalendar.features.notifications.testutil.FakeNotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationCenterViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeNotificationRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeNotificationRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun emptyState_loadsWithoutError() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.notifications.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(null, viewModel.uiState.value.loadError)
    }

    @Test
    fun expansion_isIndependent_andDismissRemovesOne() = runTest(dispatcher) {
        val first = repository.insert(notification(NotificationType.OVERDUE_TRANSACTIONS, 1L))
        val second = repository.insert(notification(NotificationType.STS_LOW, 2L))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.toggleExpand(first.id)
        viewModel.toggleExpand(second.id)
        assertEquals(setOf(first.id, second.id), viewModel.uiState.value.expandedNotificationIds)

        viewModel.dismiss(first.id)
        advanceUntilIdle()

        assertEquals(listOf(second.id), viewModel.uiState.value.notifications.map { it.id })
        assertEquals(setOf(second.id), viewModel.uiState.value.expandedNotificationIds)
    }

    @Test
    fun clearAll_dismissesEveryNotification() = runTest(dispatcher) {
        repository.insert(notification(NotificationType.STS_LOW, 1L))
        repository.insert(notification(NotificationType.STS_NEGATIVE, 2L))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.dismissAll()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.notifications.isEmpty())
        assertFalse(viewModel.uiState.value.isDismissingAll)
    }

    @Test
    fun dismissFailure_keepsNotification_andShowsScopedError() = runTest(dispatcher) {
        val inserted = repository.insert(notification(NotificationType.STS_LOW, 1L))
        val viewModel = viewModel()
        advanceUntilIdle()
        repository.failDismiss = true

        viewModel.dismiss(inserted.id)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.notifications.size)
        assertNotNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.dismissingNotificationIds.isEmpty())
    }

    @Test
    fun loadFailure_canRetry() = runTest(dispatcher) {
        repository.failReads = true
        val viewModel = viewModel()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.loadError)

        repository.failReads = false
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.loadError)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    private fun viewModel(): NotificationCenterViewModel {
        return NotificationCenterViewModel(
            getActiveNotificationsUseCase = GetActiveNotificationsUseCase(repository),
            dismissNotificationUseCase = DismissNotificationUseCase(repository),
            dismissAllNotificationsUseCase = DismissAllNotificationsUseCase(repository),
            getUnreadNotificationCountUseCase = GetUnreadNotificationCountUseCase(repository),
        )
    }

    private fun notification(type: NotificationType, createdAt: Long): Notification {
        return Notification(
            id = 0L,
            type = type,
            isDismissed = false,
            triggerMonth = null,
            triggerYear = null,
            overdueCount = if (type == NotificationType.OVERDUE_TRANSACTIONS) 2 else null,
            projectedRollover = null,
            daysRemaining = null,
            currentSts = if (type == NotificationType.STS_LOW) 10L else null,
            poolBalance = if (type == NotificationType.STS_LOW) 100L else null,
            createdAt = createdAt,
            updatedAt = createdAt,
        )
    }
}
