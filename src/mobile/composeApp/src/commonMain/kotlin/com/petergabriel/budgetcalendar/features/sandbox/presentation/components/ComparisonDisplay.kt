package com.petergabriel.budgetcalendar.features.sandbox.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils

@Composable
fun ComparisonDisplay(
    realSafeToSpend: Long,
    sandboxSafeToSpend: Long,
    difference: Long,
    modifier: Modifier = Modifier,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing
    val radius = BudgetCalendarTheme.radius

    var expanded by remember { mutableStateOf(true) }
    val differenceColor = if (difference >= 0L) colors.colorSuccess else colors.colorError
    val differenceMarker = if (difference >= 0L) "^" else "v"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = colors.bgSurface,
                shape = RoundedCornerShape(radius.xl),
            )
            .clickable { expanded = !expanded }
            .padding(spacing.spacing3),
        verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
    ) {
        Text(
            text = if (expanded) "Comparison (tap to collapse)" else "Comparison (tap to expand)",
            style = typography.bodySmall,
            color = colors.textSecondary,
        )

        if (expanded) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Real",
                        color = colors.colorInfo,
                        style = typography.bodySmall,
                    )
                    Text(
                        text = CurrencyUtils.formatCents(realSafeToSpend),
                        style = typography.cardTitle,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Sandbox",
                        color = colors.colorWarning,
                        style = typography.bodySmall,
                    )
                    Text(
                        text = CurrencyUtils.formatCents(sandboxSafeToSpend),
                        style = typography.cardTitle,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                    )
                }
            }
        }

        Text(
            text = "Difference: $differenceMarker ${CurrencyUtils.formatCents(difference, includePlusSign = true)}",
            style = typography.bodyMedium,
            color = differenceColor,
            fontWeight = FontWeight.Bold,
        )
    }
}
