package com.petergabriel.budgetcalendar.features.transactions.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils
import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

enum class TransactionItemDensity {
    Regular,
    Compact,
}

@Composable
fun TransactionListItem(
    transaction: Transaction,
    accountName: String,
    destinationAccountName: String? = null,
    modifier: Modifier = Modifier,
    density: TransactionItemDensity = TransactionItemDensity.Regular,
    onTap: () -> Unit,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing
    val radius = BudgetCalendarTheme.radius

    val displayAmount = if (transaction.type == TransactionType.TRANSFER) {
        transaction.amount
    } else {
        transaction.signedAmount
    }
    val amountText = CurrencyUtils.formatCents(
        amountInCents = displayAmount,
        includePlusSign = transaction.type != TransactionType.TRANSFER && transaction.signedAmount >= 0,
    )
    val titleText = if (transaction.type == TransactionType.TRANSFER) {
        "Transfer"
    } else {
        transaction.category ?: "Uncategorized"
    }
    val accountText = if (transaction.type == TransactionType.TRANSFER && destinationAccountName != null) {
        if (density == TransactionItemDensity.Compact) {
            "$accountName → $destinationAccountName"
        } else {
            "$accountName → $destinationAccountName • ${formatDate(transaction.date)}"
        }
    } else if (density == TransactionItemDensity.Compact) {
        accountName
    } else {
        "$accountName • ${formatDate(transaction.date)}"
    }

    val amountColor = when (transaction.type) {
        TransactionType.INCOME -> colors.colorSuccessText
        TransactionType.EXPENSE -> colors.colorErrorText
        TransactionType.TRANSFER -> colors.colorInfoText
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = spacing.spacing12)
            .background(
                color = colors.bgSurface,
                shape = RoundedCornerShape(radius.lg),
            )
            .clickable(onClick = onTap)
            .semantics {
                contentDescription = "$titleText, $accountText, $amountText, ${transaction.status.name.lowercase()}"
            }
            .padding(
                horizontal = spacing.spacing3,
                vertical = if (density == TransactionItemDensity.Compact) spacing.spacing3 else spacing.spacing4,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = spacing.spacing3),
            verticalArrangement = Arrangement.spacedBy(spacing.spacing1),
        ) {
            Text(
                text = titleText,
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
                text = accountText,
                style = typography.bodySmall,
                color = colors.textTertiary,
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(spacing.spacing1),
        ) {
            Text(
                text = amountText,
                style = typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor,
            )

            if (density == TransactionItemDensity.Regular || transaction.status == TransactionStatus.PENDING || transaction.status == TransactionStatus.OVERDUE) {
                TransactionStatusBadge(status = transaction.status)
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
