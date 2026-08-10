package com.petergabriel.budgetcalendar.features.calendar.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.petergabriel.budgetcalendar.core.designsystem.component.BcButton
import com.petergabriel.budgetcalendar.core.designsystem.component.ButtonVariant
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils

@Composable
fun EndOfMonthProjectionPill(
    projectionAmount: Long,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRetry: (() -> Unit)? = null,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing
    val radius = BudgetCalendarTheme.radius

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = colors.bgSurface,
                shape = RoundedCornerShape(radius.lg),
            )
            .padding(horizontal = spacing.spacing4, vertical = spacing.spacing3),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "End of Month Projection",
            style = typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = colors.textSecondary,
        )

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = spacing.spacing10, minHeight = spacing.spacing12)
                        .background(
                            color = colors.bgPrimary,
                            shape = RoundedCornerShape(radius.md),
                        )
                        .semantics {
                            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                        },
                )
            }

            !errorMessage.isNullOrBlank() -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.spacing2),
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        style = typography.bodySmall,
                        color = colors.colorErrorText,
                    )
                    onRetry?.let { retry ->
                        BcButton(
                            text = "Retry",
                            contentDescription = "Retry end of month projection",
                            onClick = retry,
                            variant = ButtonVariant.Outline,
                        )
                    }
                }
            }

            else -> {
                Text(
                    text = CurrencyUtils.formatCents(projectionAmount),
                    style = typography.bodyLarge.copy(
                        fontFamily = typography.title.fontFamily,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = if (projectionAmount <= 0L) colors.colorErrorText else colors.textPrimary,
                    modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                    },
                )
            }
        }
    }
}
