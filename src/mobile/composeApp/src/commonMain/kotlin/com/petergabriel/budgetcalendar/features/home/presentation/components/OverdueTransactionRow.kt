package com.petergabriel.budgetcalendar.features.home.presentation.components

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.petergabriel.budgetcalendar.core.designsystem.component.BcBadge
import com.petergabriel.budgetcalendar.core.designsystem.component.BadgeVariant
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils
import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType

@Composable
fun OverdueTransactionRow(
    transaction: Transaction,
    accountName: String,
    destinationAccountName: String? = null,
    relativeDateLabel: String,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing
    val radius = BudgetCalendarTheme.radius
    val title = remember(transaction.id, transaction.type, transaction.category) {
        if (transaction.type == TransactionType.TRANSFER) {
            "Transfer"
        } else {
            transaction.category?.takeIf(String::isNotBlank) ?: "Uncategorized"
        }
    }
    val accountLabel = if (transaction.type == TransactionType.TRANSFER && destinationAccountName != null) {
        "$accountName → $destinationAccountName"
    } else {
        accountName
    }
    val amountLabel = CurrencyUtils.formatCents(
        amountInCents = if (transaction.type == TransactionType.TRANSFER) transaction.amount else transaction.signedAmount,
        includePlusSign = transaction.type != TransactionType.TRANSFER && transaction.signedAmount >= 0,
    )
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
                contentDescription = "$title, $accountLabel, $relativeDateLabel, $amountLabel, overdue"
            }
            .padding(spacing.spacing3),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.spacing1),
        ) {
            Text(
                text = title,
                style = typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            transaction.description?.takeIf(String::isNotBlank)?.let { description ->
                Text(
                    text = description,
                    style = typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "$accountLabel · $relativeDateLabel",
                style = typography.bodySmall,
                color = colors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(spacing.spacing1),
        ) {
            Text(
                text = amountLabel,
                style = typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor,
            )
            BcBadge(
                text = "Overdue",
                variant = BadgeVariant.Overdue,
            )
        }
    }
}
