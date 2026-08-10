package com.petergabriel.budgetcalendar.features.sandbox.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import com.petergabriel.budgetcalendar.core.designsystem.component.BcButton
import com.petergabriel.budgetcalendar.core.designsystem.component.BcSectionHeader
import com.petergabriel.budgetcalendar.core.designsystem.component.ButtonVariant
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxTransaction

@Composable
fun SandboxTransactionList(
    transactions: List<SandboxTransaction>,
    onAddTap: () -> Unit,
    onPromote: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRetry: (() -> Unit)? = null,
    removingTransactionIds: Set<Long> = emptySet(),
    promotingTransactionIds: Set<Long> = emptySet(),
) {
    val colors = BudgetCalendarTheme.colors
    val spacing = BudgetCalendarTheme.spacing
    val typography = BudgetCalendarTheme.typography

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.spacing3),
    ) {
        BcSectionHeader(
            title = "Sandbox Transactions",
            actionText = "Add",
            onActionClick = onAddTap,
            modifier = Modifier.fillMaxWidth(),
        )

        if (isLoading && transactions.isEmpty()) {
            Text(
                text = "Loading sandbox transactions...",
                style = typography.bodyMedium,
                color = colors.textSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.spacing4)
                    .semantics {
                        progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                        liveRegion = LiveRegionMode.Polite
                    },
            )
        } else if (transactions.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.spacing5),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.spacing3),
            ) {
                Text(
                    text = "No transactions yet. Tap Add to start planning.",
                    style = typography.bodyMedium,
                    color = colors.textSecondary,
                )
                BcButton(
                    text = "Add",
                    onClick = onAddTap,
                    variant = ButtonVariant.Primary,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = spacing.spacing12 * 8),
                verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
            ) {
                items(transactions, key = { transaction -> transaction.id }) { transaction ->
                    SandboxTransactionItem(
                        transaction = transaction,
                        onPromote = onPromote,
                        onRemove = onRemove,
                        isBusy = transaction.id in removingTransactionIds || transaction.id in promotingTransactionIds,
                    )
                }
            }
        }

        if (!errorMessage.isNullOrBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.spacing2)) {
                Text(
                    text = errorMessage,
                    style = typography.bodySmall,
                    color = colors.colorErrorText,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                onRetry?.let { retry ->
                    BcButton(
                        text = "Retry",
                        onClick = retry,
                        variant = ButtonVariant.Outline,
                    )
                }
            }
        }
    }
}
