package com.petergabriel.budgetcalendar.features.transactions.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils
import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionListItem(
    transaction: Transaction,
    accountName: String,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing
    val radius = BudgetCalendarTheme.radius

    val signedAmount = when (transaction.type) {
        TransactionType.INCOME -> transaction.amount
        TransactionType.EXPENSE -> -transaction.amount
        TransactionType.TRANSFER -> transaction.amount
    }

    val amountColor = when (transaction.type) {
        TransactionType.INCOME -> colors.colorSuccess
        TransactionType.EXPENSE -> colors.colorError
        TransactionType.TRANSFER -> colors.colorInfo
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = colors.bgSurface,
                shape = RoundedCornerShape(radius.lg),
            )
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress,
            )
            .padding(horizontal = spacing.spacing3, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = spacing.spacing3),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = transaction.category ?: "Uncategorized",
                style = typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
            )

            if (!transaction.description.isNullOrBlank()) {
                Text(
                    text = transaction.description,
                    style = typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = "$accountName • ${formatDate(transaction.date)}",
                style = typography.bodySmall,
                color = colors.textTertiary,
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(spacing.spacing1),
        ) {
            Text(
                text = CurrencyUtils.formatCents(
                    amountInCents = signedAmount,
                    includePlusSign = transaction.type != TransactionType.EXPENSE,
                ),
                style = typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor,
            )

            TransactionStatusBadge(status = transaction.status)
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
