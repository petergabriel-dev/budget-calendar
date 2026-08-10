package com.petergabriel.budgetcalendar.features.notifications.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petergabriel.budgetcalendar.features.notifications.domain.usecase.DismissAllNotificationsUseCase
import com.petergabriel.budgetcalendar.features.notifications.domain.usecase.DismissNotificationUseCase
import com.petergabriel.budgetcalendar.features.notifications.domain.usecase.GetActiveNotificationsUseCase
import com.petergabriel.budgetcalendar.features.notifications.domain.usecase.GetUnreadNotificationCountUseCase
import kotlinx.coroutines.Job
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
    private var notificationsJob: Job? = null

    val unreadCount = getUnreadNotificationCountUseCase()

    init {
        observeNotifications()
    }

    fun toggleExpand(id: Long) {
        _uiState.update { state ->
            val expanded = state.expandedNotificationIds.toMutableSet()
            if (!expanded.add(id)) {
                expanded.remove(id)
            }
            state.copy(expandedNotificationIds = expanded)
        }
    }

    fun retry() {
        _uiState.update { state -> state.copy(isLoading = true, loadError = null) }
        observeNotifications()
    }

    fun dismiss(id: Long) {
        val state = _uiState.value
        if (id in state.dismissingNotificationIds || state.isDismissingAll) {
            return
        }

        _uiState.update { current ->
            current.copy(
                dismissingNotificationIds = current.dismissingNotificationIds + id,
                error = null,
            )
        }
        viewModelScope.launch {
            runCatching { dismissNotificationUseCase(id) }
                .onSuccess {
                    _uiState.update { current ->
                        current.copy(dismissingNotificationIds = current.dismissingNotificationIds - id)
                    }
                }
                .onFailure { throwable ->
                    _uiState.update { current ->
                        current.copy(
                            dismissingNotificationIds = current.dismissingNotificationIds - id,
                            error = throwable.message ?: "Unable to dismiss notification",
                        )
                    }
                }
        }
    }

    fun dismissAll() {
        val state = _uiState.value
        if (state.isDismissingAll || state.dismissingNotificationIds.isNotEmpty() || state.notifications.isEmpty()) {
            return
        }

        _uiState.update { current ->
            current.copy(
                isDismissingAll = true,
                error = null,
            )
        }
        viewModelScope.launch {
            runCatching { dismissAllNotificationsUseCase() }
                .onSuccess {
                    _uiState.update { current -> current.copy(isDismissingAll = false) }
                }
                .onFailure { throwable ->
                    _uiState.update { current ->
                        current.copy(
                            isDismissingAll = false,
                            error = throwable.message ?: "Unable to clear notifications",
                        )
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { state -> state.copy(error = null) }
    }

    private fun observeNotifications() {
        notificationsJob?.cancel()
        notificationsJob = viewModelScope.launch {
            getActiveNotificationsUseCase()
                .catch { throwable ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            loadError = throwable.message ?: "Unable to load notifications",
                        )
                    }
                }
                .collect { notifications ->
                    _uiState.update { state ->
                        state.copy(
                            notifications = notifications,
                            isLoading = false,
                            loadError = null,
                            expandedNotificationIds = state.expandedNotificationIds
                                .intersect(notifications.map { notification -> notification.id }.toSet()),
                        )
                    }
                }
        }
    }
}
