package com.petergabriel.budgetcalendar.features.creditcard.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils
import com.petergabriel.budgetcalendar.features.creditcard.domain.model.CreditCardSummary
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@Composable
fun CreditCardListItem(
    summary: CreditCardSummary,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing
    val radius = BudgetCalendarTheme.radius

    val balanceColor = if (summary.currentBalance <= 0L) colors.colorError else colors.colorSuccess

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onTap),
        shape = RoundedCornerShape(radius.xl),
        colors = CardDefaults.cardColors(containerColor = colors.bgDark),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.spacing4),
            verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.accountName,
                        style = typography.cardTitle,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textInverted,
                    )
                    Text(
                        text = "Credit Card",
                        style = typography.bodySmall,
                        color = colors.textDisabled,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }

                Text(
                    text = CurrencyUtils.formatCents(summary.currentBalance),
                    style = typography.cardTitle,
                    fontWeight = FontWeight.Bold,
                    color = balanceColor,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.spacing2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(radius.full),
                    color = colors.bgMuted,
                ) {
                    Text(
                        text = "${CurrencyUtils.formatCents(summary.reservedAmount)} reserved",
                        style = typography.bodySmall,
                        color = colors.colorWarning,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = spacing.spacing1),
                    )
                }

                if (summary.dueDate != null) {
                    Text(
                        text = "Due ${formatDate(summary.dueDate)}",
                        style = typography.bodySmall,
                        color = colors.textDisabled,
                    )
                }
            }
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
