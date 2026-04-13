package com.petergabriel.budgetcalendar.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils

@Composable
fun HeroSafeToSpend(
    amount: Long,
    dailyRate: Long? = null,
    daysRemaining: Int? = null,
    isLastDayOfMonth: Boolean = false,
    label: String = "SAFE TO SPEND",
    modifier: Modifier = Modifier,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing
    val radius = BudgetCalendarTheme.radius
    var showTooltip by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(dailyRate, isLastDayOfMonth) {
        if (dailyRate == null || !isLastDayOfMonth) {
            showTooltip = false
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.spacing1),
    ) {
        Text(
            text = label,
            style = typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = typography.caption.letterSpacing,
            ),
            color = colors.textSecondary,
        )

        Text(
            text = CurrencyUtils.formatCents(amount),
            style = typography.displayMedium,
            color = colors.textPrimary,
        )

        if (dailyRate != null) {
            val resolvedDaysRemaining = daysRemaining?.coerceAtLeast(1)
            val remainingDaysLabel = when (resolvedDaysRemaining) {
                1 -> "1 day left"
                null -> null
                else -> "$resolvedDaysRemaining days left"
            }
            val rowColor = if (isLastDayOfMonth) {
                colors.colorError
            } else {
                colors.textSecondary
            }
            val dailyRateLabel = buildString {
                append("${CurrencyUtils.formatCents(dailyRate)} / day")
                if (remainingDaysLabel != null) {
                    append(" · $remainingDaysLabel")
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.spacing1),
            ) {
                Row(
                    modifier = if (isLastDayOfMonth) {
                        Modifier.clickable {
                            showTooltip = !showTooltip
                        }
                    } else {
                        Modifier
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.spacing2),
                ) {
                    if (resolvedDaysRemaining != null) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = "Days remaining this month",
                            tint = rowColor,
                            modifier = Modifier.size(spacing.spacing4),
                        )
                    }

                    Text(
                        text = dailyRateLabel,
                        style = typography.bodyMedium.copy(fontWeight = FontWeight.Light),
                        color = rowColor,
                    )
                }

                if (isLastDayOfMonth && showTooltip) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = colors.bgDark,
                                shape = RoundedCornerShape(radius.md),
                            )
                            .padding(
                                horizontal = spacing.spacing3,
                                vertical = spacing.spacing2,
                            ),
                    ) {
                        Text(
                            text = "You have 1 day left this month. This is your full Safe to Spend balance.",
                            style = typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = colors.textInverted,
                        )
                    }
                }
            }
        }
    }
}
