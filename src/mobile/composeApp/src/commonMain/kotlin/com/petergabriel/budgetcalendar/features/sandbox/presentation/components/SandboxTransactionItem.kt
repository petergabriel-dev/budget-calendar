package com.petergabriel.budgetcalendar.features.sandbox.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.petergabriel.budgetcalendar.core.designsystem.component.BadgeVariant
import com.petergabriel.budgetcalendar.core.designsystem.component.BcBadge
import com.petergabriel.budgetcalendar.core.designsystem.component.BcButton
import com.petergabriel.budgetcalendar.core.designsystem.component.BcConfirmDialog
import com.petergabriel.budgetcalendar.core.designsystem.component.ButtonVariant
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxTransaction
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@Composable
fun SandboxTransactionItem(
    transaction: SandboxTransaction,
    modifier: Modifier = Modifier,
    onPromote: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    isBusy: Boolean = false,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing
    val radius = BudgetCalendarTheme.radius
    var showPromoteConfirm by remember { mutableStateOf(false) }
    var showRemoveConfirm by remember { mutableStateOf(false) }

    val signedAmount = transaction.signedAmount ?: when (transaction.type) {
        TransactionType.INCOME -> transaction.amount
        TransactionType.EXPENSE -> -transaction.amount
        TransactionType.TRANSFER -> transaction.amount
    }
    val amountColor = when (transaction.type) {
        TransactionType.INCOME -> colors.colorSuccessText
        TransactionType.EXPENSE -> colors.colorErrorText
        TransactionType.TRANSFER -> colors.colorInfoText
    }
    val typeLabel = when (transaction.type) {
        TransactionType.INCOME -> "Income"
        TransactionType.EXPENSE -> "Expense"
        TransactionType.TRANSFER -> "Transfer"
    }
    val category = transaction.category?.ifBlank { null } ?: "Uncategorized"
    val promoted = transaction.originalTransactionId != null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bgSurface, RoundedCornerShape(radius.lg))
            .padding(spacing.spacing3)
            .semantics {
                contentDescription = "$typeLabel, $category, ${CurrencyUtils.formatCents(signedAmount)}, " +
                    if (promoted) "promoted" else "not promoted"
            },
        verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.spacing3),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.spacing1),
            ) {
                BcBadge(
                    text = typeLabel,
                    variant = when (transaction.type) {
                        TransactionType.INCOME -> BadgeVariant.Success
                        TransactionType.EXPENSE -> BadgeVariant.Error
                        TransactionType.TRANSFER -> BadgeVariant.Info
                    },
                )
                Text(
                    text = category,
                    style = typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
                transaction.description?.takeIf { it.isNotBlank() }?.let { description ->
                    Text(
                        text = description,
                        style = typography.bodySmall,
                        color = colors.textSecondary,
                    )
                }
                Text(
                    text = formatDate(transaction.date),
                    style = typography.bodySmall,
                    color = colors.textTertiary,
                )
            }

            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text(
                    text = CurrencyUtils.formatCents(
                        amountInCents = signedAmount,
                        includePlusSign = signedAmount >= 0L,
                    ),
                    style = typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor,
                )
                if (promoted) {
                    BcBadge(text = "Promoted", variant = BadgeVariant.Success)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.spacing2),
        ) {
            if (!promoted) {
                BcButton(
                    text = "Promote",
                    onClick = { showPromoteConfirm = true },
                    variant = ButtonVariant.Outline,
                    enabled = !isBusy,
                    modifier = Modifier.weight(1f),
                )
            }
            BcButton(
                text = "Remove",
                onClick = { showRemoveConfirm = true },
                variant = ButtonVariant.Destructive,
                enabled = !isBusy,
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (showPromoteConfirm) {
        BcConfirmDialog(
            title = "Promote transaction?",
            message = "This adds the transaction to real data and removes it from sandbox.",
            onDismissRequest = { if (!isBusy) showPromoteConfirm = false },
            onConfirm = {
                onPromote(transaction.id)
                showPromoteConfirm = false
            },
            confirmLabel = "Promote",
            isBusy = isBusy,
        )
    }

    if (showRemoveConfirm) {
        BcConfirmDialog(
            title = "Remove sandbox transaction?",
            message = "This removes this transaction from the sandbox. This action cannot be undone.",
            onDismissRequest = { if (!isBusy) showRemoveConfirm = false },
            onConfirm = {
                onRemove(transaction.id)
                showRemoveConfirm = false
            },
            confirmLabel = "Remove",
            isBusy = isBusy,
        )
    }
}

private fun formatDate(dateMillis: Long): String {
    return Instant
        .fromEpochMilliseconds(dateMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
        .toString()
}
