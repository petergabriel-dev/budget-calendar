package com.petergabriel.budgetcalendar.features.sandbox.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils

@Composable
fun HeroProjectedSpend(
    projectedSafeToSpend: Long,
    dailyRate: Long? = null,
    modifier: Modifier = Modifier,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing
    val isNegativeOrZero = projectedSafeToSpend <= 0L

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.spacing1),
    ) {
        Text(
            text = "PROJECTED SPEND",
            style = typography.bodyMedium.copy(
                letterSpacing = typography.caption.letterSpacing,
            ),
            color = colors.textSecondary,
        )

        if (isNegativeOrZero) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.spacing2),
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = "Projected spend warning",
                    tint = colors.colorError,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = CurrencyUtils.formatCents(projectedSafeToSpend),
                    style = typography.displayMedium,
                    color = colors.colorError,
                )
            }
        } else {
            Text(
                text = CurrencyUtils.formatCents(projectedSafeToSpend),
                style = typography.displayMedium,
                color = colors.textPrimary,
            )
        }

        if (dailyRate != null) {
            Text(
                text = "${CurrencyUtils.formatCents(dailyRate)} / day",
                style = typography.bodyMedium,
                color = colors.textSecondary,
            )
        }
    }
}
