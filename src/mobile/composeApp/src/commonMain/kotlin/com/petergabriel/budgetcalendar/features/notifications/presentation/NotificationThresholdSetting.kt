package com.petergabriel.budgetcalendar.features.notifications.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardType
import com.petergabriel.budgetcalendar.core.designsystem.component.BcButton
import com.petergabriel.budgetcalendar.core.designsystem.component.ButtonVariant
import com.petergabriel.budgetcalendar.core.designsystem.component.form.BcInputGroup
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme

@Composable
fun NotificationThresholdSetting(
    value: String,
    isLoading: Boolean,
    isSaving: Boolean,
    validationError: String?,
    saveError: String?,
    didSave: Boolean,
    loadError: String? = null,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    onThresholdInputFocusChanged: (Boolean) -> Unit = {},
) {
    val colors = BudgetCalendarTheme.colors
    val spacing = BudgetCalendarTheme.spacing
    val typography = BudgetCalendarTheme.typography
    val radius = BudgetCalendarTheme.radius
    val canEdit = !isLoading && (loadError.isNullOrBlank() || value.isNotBlank())

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bgSurface, RoundedCornerShape(radius.lg))
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
            value = value,
            onValueChange = onValueChange,
            placeholder = if (isLoading) "Loading..." else "15",
            readOnly = !canEdit,
            fieldModifier = Modifier.onFocusChanged {
                onThresholdInputFocusChanged(it.isFocused)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = !validationError.isNullOrBlank(),
            errorText = validationError,
            modifier = Modifier.semantics {
                contentDescription = "STS Low Alert Threshold percent, whole number, valid range 1 to 100"
                if (isLoading) {
                    progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                    liveRegion = LiveRegionMode.Polite
                }
                if (!validationError.isNullOrBlank()) {
                    stateDescription = validationError
                }
            },
        )

        Text(
            text = "Valid range: 1 to 100. Default is 15.",
            style = typography.bodySmall,
            color = colors.textSecondary,
        )

        BcButton(
            text = if (isSaving) "Saving..." else "Save",
            onClick = onSave,
            variant = ButtonVariant.Primary,
            enabled = !isLoading && !isSaving,
            modifier = Modifier.fillMaxWidth(),
        )

        if (!loadError.isNullOrBlank()) {
            FeedbackText(
                text = loadError,
                color = colors.colorErrorText,
            )
            onRetry?.let { retry ->
                BcButton(
                    text = "Retry",
                    onClick = retry,
                    variant = ButtonVariant.Outline,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (!saveError.isNullOrBlank()) {
            FeedbackText(
                text = saveError,
                color = colors.colorErrorText,
            )
        }

        if (didSave) {
            FeedbackText(
                text = "Threshold saved",
                color = colors.colorSuccessText,
            )
        }
    }
}

@Composable
private fun FeedbackText(
    text: String,
    color: androidx.compose.ui.graphics.Color,
) {
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing

    Text(
        text = text,
        style = typography.bodySmall,
        color = color,
        modifier = Modifier
            .padding(start = spacing.spacing1)
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}
