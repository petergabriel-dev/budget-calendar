package com.petergabriel.budgetcalendar.features.sandbox.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxComparison

@Composable
fun ComparisonDeltaRow(
    comparison: SandboxComparison,
    modifier: Modifier = Modifier,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography

    val deltaColor = when {
        comparison.difference > 0L -> colors.colorSuccess
        comparison.difference < 0L -> colors.colorError
        else -> colors.textSecondary
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "vs ${CurrencyUtils.formatCents(comparison.realSafeToSpend)} real",
            style = typography.bodyMedium,
            color = colors.textSecondary,
        )
        Text(
            text = CurrencyUtils.formatCents(
                amountInCents = comparison.difference,
                includePlusSign = true,
            ),
            style = typography.bodyMedium,
            color = deltaColor,
        )
    }
}
