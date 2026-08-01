package com.petergabriel.budgetcalendar.features.notifications.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.petergabriel.budgetcalendar.core.designsystem.component.BcButton
import com.petergabriel.budgetcalendar.core.designsystem.component.BcHeader
import com.petergabriel.budgetcalendar.core.designsystem.component.BcNotificationBell
import com.petergabriel.budgetcalendar.core.designsystem.component.ButtonVariant
import com.petergabriel.budgetcalendar.core.designsystem.component.form.BcInputGroup
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NotificationSettingsScreen(
    modifier: Modifier = Modifier,
    unreadCount: Int,
    onBellClick: () -> Unit,
    viewModel: NotificationSettingsViewModel = koinViewModel(),
) {
    val colors = BudgetCalendarTheme.colors
    val spacing = BudgetCalendarTheme.spacing
    val typography = BudgetCalendarTheme.typography
    val radius = BudgetCalendarTheme.radius

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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = colors.bgSurface,
                    shape = RoundedCornerShape(radius.lg),
                )
                .padding(spacing.spacing4),
            verticalArrangement = Arrangement.spacedBy(spacing.spacing3),
        ) {
            Text(
                text = "Notification Preferences",
                style = typography.cardTitle,
                color = colors.textPrimary,
            )

            BcInputGroup(
                label = "STS Low Alert Threshold (%)",
                value = uiState.thresholdInput,
                onValueChange = viewModel::onThresholdInputChange,
                placeholder = "15",
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                ),
                isError = !uiState.validationError.isNullOrBlank(),
                errorText = uiState.validationError,
            )

            Text(
                text = "Valid range: 1 to 100. Default is 15.",
                style = typography.bodySmall,
                color = colors.textSecondary,
            )

            BcButton(
                text = if (uiState.isSaving) "Saving..." else "Save",
                onClick = viewModel::save,
                variant = ButtonVariant.Primary,
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth(),
            )

            if (!uiState.error.isNullOrBlank()) {
                Text(
                    text = uiState.error.orEmpty(),
                    style = typography.bodySmall,
                    color = colors.colorError,
                )
            }

            if (uiState.didSave) {
                Text(
                    text = "Threshold saved",
                    style = typography.bodySmall,
                    color = colors.colorSuccess,
                )
            }
        }
    }
}
