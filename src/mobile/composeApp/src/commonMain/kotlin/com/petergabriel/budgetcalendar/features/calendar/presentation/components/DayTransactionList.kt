package com.petergabriel.budgetcalendar.features.calendar.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import com.petergabriel.budgetcalendar.core.designsystem.component.BcButton
import com.petergabriel.budgetcalendar.core.designsystem.component.ButtonVariant
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import com.petergabriel.budgetcalendar.features.transactions.presentation.components.TransactionItemDensity
import com.petergabriel.budgetcalendar.features.transactions.presentation.components.TransactionListItem

@Composable
fun DayTransactionList(
    transactions: List<Transaction>,
    modifier: Modifier = Modifier,
    accountNameProvider: (Long) -> String = { accountId -> "Account #$accountId" },
    maxItems: Int? = null,
    density: TransactionItemDensity = TransactionItemDensity.Regular,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRetry: (() -> Unit)? = null,
    onTransactionTap: (Transaction) -> Unit = {},
) {
    val spacing = BudgetCalendarTheme.spacing
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val displayTransactions = buildDisplayTransactions(
        transactions = transactions,
        maxItems = maxItems,
    )

    if (isLoading && displayTransactions.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Loading transactions…",
                style = typography.bodyMedium,
                color = colors.textSecondary,
            )
        }
        return
    }

    if (!errorMessage.isNullOrBlank()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = spacing.spacing2),
            verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
        ) {
            Text(
                text = errorMessage,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = typography.bodySmall,
                color = colors.colorErrorText,
            )
            onRetry?.let { retry ->
                BcButton(
                    text = "Retry",
                    contentDescription = "Retry loading selected-day transactions",
                    onClick = retry,
                    variant = ButtonVariant.Outline,
                )
            }
        }
        if (displayTransactions.isEmpty()) {
            return
        }
    }

    if (displayTransactions.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = spacing.spacing3),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No transactions",
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = typography.bodyMedium,
                color = colors.textSecondary,
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
    ) {
        items(displayTransactions, key = { transaction -> transaction.id }) { transaction ->
            TransactionListItem(
                transaction = transaction,
                accountName = accountNameProvider(transaction.accountId),
                destinationAccountName = transaction.destinationAccountId?.let(accountNameProvider),
                density = density,
                onTap = { onTransactionTap(transaction) },
            )
        }
    }
}

private fun buildDisplayTransactions(
    transactions: List<Transaction>,
    maxItems: Int?,
): List<Transaction> {
    val collapsedTransfers = transactions.filterNot { transaction ->
        transaction.type == TransactionType.TRANSFER &&
            transaction.signedAmount > 0 &&
            transaction.linkedTransactionId != null &&
            transactions.any { other -> other.id == transaction.linkedTransactionId }
    }

    val sorted = collapsedTransfers.sortedWith(
        compareByDescending<Transaction> { it.createdAt }
            .thenByDescending { it.updatedAt }
            .thenByDescending { it.id },
    )

    return maxItems?.let(sorted::take) ?: sorted
}
