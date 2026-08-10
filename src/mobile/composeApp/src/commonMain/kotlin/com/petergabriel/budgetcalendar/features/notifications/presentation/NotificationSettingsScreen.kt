package com.petergabriel.budgetcalendar.features.notifications.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.petergabriel.budgetcalendar.core.designsystem.component.BcHeader
import com.petergabriel.budgetcalendar.core.designsystem.component.BcNotificationBell
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NotificationSettingsScreen(
    modifier: Modifier = Modifier,
    unreadCount: Int,
    onBellClick: () -> Unit,
    viewModel: NotificationSettingsViewModel = koinViewModel(),
) {
    val spacing = BudgetCalendarTheme.spacing
    val colors = BudgetCalendarTheme.colors
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgPrimary)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.spacing4, vertical = spacing.spacing4),
        verticalArrangement = Arrangement.spacedBy(spacing.spacing4),
    ) {
        BcHeader(
            title = "Settings",
            modifier = Modifier.fillMaxWidth(),
            actionContent = {
                BcNotificationBell(
                    unreadCount = unreadCount,
                    onClick = onBellClick,
                )
            },
        )

        NotificationThresholdSetting(
            value = uiState.thresholdInput,
            isLoading = uiState.isLoading,
            isSaving = uiState.isSaving,
            validationError = uiState.validationError,
            saveError = uiState.saveError,
            didSave = uiState.didSave,
            loadError = uiState.loadError,
            onValueChange = viewModel::onThresholdInputChange,
            onSave = viewModel::save,
            onRetry = viewModel::retry,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
