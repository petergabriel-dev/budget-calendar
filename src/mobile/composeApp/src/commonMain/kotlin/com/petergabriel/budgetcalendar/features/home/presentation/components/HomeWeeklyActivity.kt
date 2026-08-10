package com.petergabriel.budgetcalendar.features.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.petergabriel.budgetcalendar.core.designsystem.component.BcButton
import com.petergabriel.budgetcalendar.core.designsystem.component.BcSectionHeader
import com.petergabriel.budgetcalendar.core.designsystem.component.BcWeekDay
import com.petergabriel.budgetcalendar.core.designsystem.component.BcWeekPicker
import com.petergabriel.budgetcalendar.core.designsystem.component.ButtonVariant
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils


data class WeeklySpendSummaryState(
    val amount: Long,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

data class SelectedDayTransactionPreviewState(
    val dateLabel: String,
    val transactionCount: Int,
    val items: List<SelectedDayTransactionPreviewItem>,
    val hasMore: Boolean,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@Composable
fun HomeWeeklyActivity(
    weeklySpend: WeeklySpendSummaryState,
    weekDays: List<BcWeekDay>,
    selectedDayPreview: SelectedDayTransactionPreviewState,
    onDaySelected: (String) -> Unit,
    onTransactionClick: (Long) -> Unit,
    onViewAllClick: (String) -> Unit,
    onRetryWeeklySpend: () -> Unit,
    onRetryPreview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing
    val selectedDayId = weekDays.firstOrNull { it.isSelected }?.id

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.spacing3),
    ) {
        BcSectionHeader(
            title = "This Week",
            modifier = Modifier.fillMaxWidth(),
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.spacing1),
        ) {
            if (weeklySpend.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(spacing.spacing6)
                        .background(
                            color = colors.bgSurface,
                            shape = RoundedCornerShape(BudgetCalendarTheme.radius.md),
                        ),
                )
            } else if (!weeklySpend.errorMessage.isNullOrBlank()) {
                Text(
                    text = "Unable to load weekly spending",
                    style = typography.bodyMedium,
                    color = colors.colorErrorText,
                )
                BcButton(
                    text = "Try again",
                    onClick = onRetryWeeklySpend,
                    variant = ButtonVariant.Ghost,
                )
            } else {
                Text(
                    text = CurrencyUtils.formatCents(weeklySpend.amount),
                    style = typography.cardTitle,
                    color = colors.textPrimary,
                )
                Text(
                    text = "Spent This Week",
                    style = typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }

        BcWeekPicker(
            days = weekDays,
            onDaySelected = onDaySelected,
            modifier = Modifier.fillMaxWidth(),
        )

        SelectedDayTransactionPreview(
            state = selectedDayPreview,
            onTransactionClick = onTransactionClick,
            onViewAllClick = {
                selectedDayId?.let(onViewAllClick)
            },
            onRetry = onRetryPreview,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
