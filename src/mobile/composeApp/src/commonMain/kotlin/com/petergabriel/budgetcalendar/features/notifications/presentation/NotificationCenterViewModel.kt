package com.petergabriel.budgetcalendar.features.notifications.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petergabriel.budgetcalendar.features.notifications.domain.usecase.DismissAllNotificationsUseCase
import com.petergabriel.budgetcalendar.features.notifications.domain.usecase.DismissNotificationUseCase
import com.petergabriel.budgetcalendar.features.notifications.domain.usecase.GetActiveNotificationsUseCase
import com.petergabriel.budgetcalendar.features.notifications.domain.usecase.GetUnreadNotificationCountUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotificationCenterViewModel(
    private val getActiveNotificationsUseCase: GetActiveNotificationsUseCase,
    private val dismissNotificationUseCase: DismissNotificationUseCase,
    private val dismissAllNotificationsUseCase: DismissAllNotificationsUseCase,
    getUnreadNotificationCountUseCase: GetUnreadNotificationCountUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationCenterUiState())
    val uiState: StateFlow<NotificationCenterUiState> = _uiState.asStateFlow()

    val unreadCount = getUnreadNotificationCountUseCase()

    init {
        observeNotifications()
    }

    fun toggleExpand(id: Long) {
        _uiState.update { state ->
            state.copy(
                expandedNotificationId = if (state.expandedNotificationId == id) {
                    null
                } else {
                    id
                },
            )
        }
    }

    fun dismiss(id: Long) {
        viewModelScope.launch {
            runCatching { dismissNotificationUseCase(id) }
                .onFailure { throwable ->
                    _uiState.update { state -> state.copy(error = throwable.message) }
                }
        }
    }

    fun dismissAll() {
        viewModelScope.launch {
            runCatching { dismissAllNotificationsUseCase() }
                .onFailure { throwable ->
                    _uiState.update { state -> state.copy(error = throwable.message) }
                }
        }
    }

    fun clearError() {
        _uiState.update { state -> state.copy(error = null) }
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            getActiveNotificationsUseCase()
                .catch { throwable ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            error = throwable.message,
                        )
                    }
                }
                .collect { notifications ->
                    _uiState.update { state ->
                        val expandedStillExists = notifications.any { notification ->
                            notification.id == state.expandedNotificationId
                        }
                        state.copy(
                            notifications = notifications,
                            isLoading = false,
                            error = null,
                            expandedNotificationId = if (expandedStillExists) state.expandedNotificationId else null,
                        )
                    }
                }
        }
    }
}
