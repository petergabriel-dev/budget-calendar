package com.petergabriel.budgetcalendar.features.recurring.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils
import com.petergabriel.budgetcalendar.features.recurring.domain.model.RecurringTransaction
import com.petergabriel.budgetcalendar.features.recurring.domain.model.RecurrenceType
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@Composable
fun RecurringCard(
    recurring: RecurringTransaction,
    nextScheduledDate: Long,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing
    val radius = BudgetCalendarTheme.radius

    val iconLabel = when (recurring.type) {
        RecurrenceType.INCOME -> "v"
        RecurrenceType.EXPENSE -> "^"
        RecurrenceType.TRANSFER -> "<>"
    }

    val iconColor = when (recurring.type) {
        RecurrenceType.INCOME -> colors.colorSuccess
        RecurrenceType.EXPENSE -> colors.colorError
        RecurrenceType.TRANSFER -> colors.colorInfo
    }

    val signedAmount = when (recurring.type) {
        RecurrenceType.INCOME -> recurring.amount
        RecurrenceType.EXPENSE -> -recurring.amount
        RecurrenceType.TRANSFER -> recurring.amount
    }

    val amountColor = when (recurring.type) {
        RecurrenceType.INCOME -> colors.colorSuccess
        RecurrenceType.EXPENSE -> colors.colorError
        RecurrenceType.TRANSFER -> colors.colorInfo
    }

    val statusDotColor = if (recurring.isActive) colors.colorSuccess else colors.textTertiary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = colors.bgSurface,
                shape = RoundedCornerShape(radius.lg),
            )
            .clickable(onClick = onTap)
            .padding(horizontal = spacing.spacing3, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.spacing1),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.spacing2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = iconLabel,
                    color = iconColor,
                    style = typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = recurring.type.name,
                    style = typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(statusDotColor, CircleShape),
                )
            }

            Text(
                text = CurrencyUtils.formatCents(
                    amountInCents = signedAmount,
                    includePlusSign = recurring.type == RecurrenceType.INCOME,
                ),
                style = typography.cardTitle,
                color = amountColor,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = "Day ${recurring.dayOfMonth} of month",
                style = typography.bodySmall,
                color = colors.textSecondary,
            )

            Text(
                text = "Next: ${formatDate(nextScheduledDate)}",
                style = typography.bodySmall,
                color = colors.textSecondary,
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(spacing.spacing1),
        ) {
            Text(
                text = if (recurring.isActive) "Active" else "Inactive",
                style = typography.bodySmall,
                color = colors.textSecondary,
            )

            Switch(
                checked = recurring.isActive,
                onCheckedChange = onToggleActive,
            )
        }
    }
}

private fun formatDate(dateMillis: Long): String {
    return Instant
        .fromEpochMilliseconds(dateMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
        .toString()
}
