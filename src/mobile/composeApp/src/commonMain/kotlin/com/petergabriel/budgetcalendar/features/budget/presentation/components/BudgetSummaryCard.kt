package com.petergabriel.budgetcalendar.features.budget.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils
import com.petergabriel.budgetcalendar.features.budget.domain.model.BudgetSummary

@Composable
fun BudgetSummaryCard(
    summary: BudgetSummary,
    onDetailsTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing
    val radius = BudgetCalendarTheme.radius

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                isExpanded = !isExpanded
                onDetailsTap()
            },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = spacing.spacing4, vertical = spacing.spacing3),
            verticalArrangement = Arrangement.spacedBy(spacing.spacing3),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Budget Summary",
                    style = typography.cardTitle,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
                Text(
                    text = if (isExpanded) "Hide" else "Details",
                    style = typography.bodyMedium,
                    color = colors.colorInfo,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
                    .clickable {
                        isExpanded = !isExpanded
                        onDetailsTap()
                    }
                    .background(
                        color = colors.colorInfoBg,
                        shape = RoundedCornerShape(radius.lg),
                    )
                    .padding(horizontal = spacing.spacing3, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Available to Spend",
                    style = typography.bodyMedium,
                    color = colors.textPrimary,
                )
                Text(
                    text = CurrencyUtils.formatCents(summary.availableToSpend),
                    style = typography.cardTitle,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
            }

            HorizontalDivider(color = colors.borderStrong)

            BudgetSummaryRow(
                label = "Spending Pool Total",
                amount = CurrencyUtils.formatCents(summary.totalLiquidAssets),
            )

            BudgetSummaryRow(
                label = "Pending Reservations",
                amount = CurrencyUtils.formatCents(-summary.pendingReservations),
                valueColor = colors.colorError,
            )

            if (isExpanded) {
                BudgetSummaryRow(
                    label = "Overdue Reservations",
                    amount = CurrencyUtils.formatCents(-summary.overdueReservations),
                    valueColor = colors.colorError,
                )
                BudgetSummaryRow(
                    label = "Confirmed This Month",
                    amount = CurrencyUtils.formatCents(summary.confirmedSpending),
                    valueColor = colors.colorSuccess,
                )
                BudgetSummaryRow(
                    label = "Rollover From Last Month",
                    amount = CurrencyUtils.formatCents(
                        amountInCents = summary.rolloverAmount,
                        includePlusSign = true,
                    ),
                    valueColor = if (summary.rolloverAmount >= 0L) colors.colorSuccess else colors.colorError,
                )
            }
        }
    }
}

@Composable
private fun BudgetSummaryRow(
    label: String,
    amount: String,
    valueColor: Color = BudgetCalendarTheme.colors.textPrimary,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = typography.bodyMedium,
            color = colors.textSecondary,
        )
        Text(
            text = amount,
            style = typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor,
        )
    }
}
