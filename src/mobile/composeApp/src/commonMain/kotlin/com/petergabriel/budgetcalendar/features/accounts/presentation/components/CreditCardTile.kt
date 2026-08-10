package com.petergabriel.budgetcalendar.features.accounts.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils
import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.creditcard.domain.model.CreditCardSummary
import kotlin.math.abs

@Composable
fun CreditCardTile(
    summary: CreditCardSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing
    val radius = BudgetCalendarTheme.radius
    val owed = abs(summary.currentBalance)
    val availableCredit = summary.availableCredit
    val creditLimit = summary.creditLimit
    val utilization = if (creditLimit != null && creditLimit > 0L) {
        (owed.toFloat() / creditLimit.toFloat()).coerceIn(0f, 1f)
    } else {
        null
    }
    val statementDue = summary.statementDueAmount?.let { amount ->
        val dateText = summary.statementDueDate
            ?.let { dueDate -> " due ${DateUtils.formatMonthDay(dueDate)}" }
            .orEmpty()
        "${CurrencyUtils.formatCents(amount)}$dateText"
    }
    val secondary = buildString {
        append("Owed ${CurrencyUtils.formatCents(owed)}")
        if (statementDue != null) append(" · $statementDue")
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = spacing.spacing12)
            .clip(RoundedCornerShape(radius.xl))
            .background(colors.bgSurface)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = buildString {
                    append("${summary.accountName}, credit card, ")
                    append(availableCredit?.let(CurrencyUtils::formatCents) ?: "Limit not set")
                    append(", $secondary")
                    utilization?.let { append(", utilization ${(it * 100).toInt()} percent") }
                }
            }
            .padding(spacing.spacing4),
        verticalArrangement = Arrangement.spacedBy(spacing.spacing3),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "CREDIT CARD",
                style = typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = colors.textSecondary,
                modifier = Modifier
                    .background(colors.bgSurface, RoundedCornerShape(radius.md))
                    .border(spacing.spacing1 / 2, colors.borderStrong, RoundedCornerShape(radius.md))
                    .padding(horizontal = spacing.spacing3, vertical = spacing.spacing2),
            )
            Text(
                text = summary.accountName,
                style = typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(spacing.spacing1)) {
            Text(
                text = availableCredit?.let(CurrencyUtils::formatCents) ?: "Limit not set",
                style = typography.section,
                fontWeight = FontWeight.ExtraBold,
                color = colors.textPrimary,
            )
            Text(
                text = if (creditLimit == null) secondary else "$secondary · of ${CurrencyUtils.formatCents(creditLimit)}",
                style = typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (utilization != null) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.spacing1)) {
                LinearProgressIndicator(
                    progress = { utilization },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radius.full)),
                    color = colors.bgDark,
                    trackColor = colors.borderSubtle,
                )
                Text(
                    text = "Utilization ${(utilization * 100).toInt()}%",
                    style = typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
    }
}
