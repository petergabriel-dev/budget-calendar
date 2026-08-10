package com.petergabriel.budgetcalendar.features.transactions.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.petergabriel.budgetcalendar.core.designsystem.component.BcConfirmDialog
import com.petergabriel.budgetcalendar.core.designsystem.component.BcSheetShell
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils
import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@Composable
fun TransactionDetailsSheet(
    isVisible: Boolean,
    transaction: Transaction,
    accountName: String,
    destinationAccountName: String? = null,
    onDismiss: () -> Unit,
    onEdit: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit,
) {
    if (!isVisible) {
        return
    }

    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing
    var actionsExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    BcSheetShell(
        title = "Transaction Details",
        onDismissRequest = onDismiss,
        closeContentDescription = "Close transaction details",
        headerContent = {
            Box {
                IconButton(
                    onClick = { actionsExpanded = true },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "Transaction actions",
                        tint = colors.textPrimary,
                    )
                }
                DropdownMenu(
                    expanded = actionsExpanded,
                    onDismissRequest = { actionsExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            actionsExpanded = false
                            onEdit(transaction)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = colors.colorErrorText) },
                        onClick = {
                            actionsExpanded = false
                            showDeleteConfirmation = true
                        },
                    )
                }
            }
        },
        body = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.spacing4),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.spacing1)) {
                    Text(
                        text = "AMOUNT",
                        style = typography.caption.copy(letterSpacing = typography.caption.letterSpacing),
                        color = colors.textSecondary,
                    )
                    Text(
                        text = transaction.detailsAmountText(),
                        style = typography.displayMedium,
                        color = transaction.amountColor(),
                    )
                }

                TransactionDetailRow(
                    label = "Type",
                    value = transaction.type.displayName,
                )
                TransactionDetailRow(
                    label = "Name",
                    value = transaction.category?.takeIf(String::isNotBlank) ?: transaction.type.displayName,
                )
                TransactionDetailRow(
                    label = "Date",
                    value = formatTransactionDetailsDate(transaction.date),
                )
                TransactionDetailStatusRow(transaction = transaction)
                TransactionDetailRow(
                    label = "Source account",
                    value = accountName,
                )

                val destinationAccountId = transaction.destinationAccountId
                if (transaction.type == TransactionType.TRANSFER && destinationAccountId != null) {
                    TransactionDetailRow(
                        label = "Destination account",
                        value = destinationAccountName ?: "Account #$destinationAccountId",
                    )
                }

                transaction.description?.takeIf(String::isNotBlank)?.let { description ->
                    TransactionDetailRow(
                        label = "Description",
                        value = description,
                    )
                }
            }
        },
    )

    if (showDeleteConfirmation) {
        BcConfirmDialog(
            title = "Delete transaction?",
            message = "This action cannot be undone.",
            onDismissRequest = { showDeleteConfirmation = false },
            onConfirm = {
                showDeleteConfirmation = false
                onDelete(transaction)
            },
            confirmLabel = "Delete",
        )
    }
}

@Composable
private fun TransactionDetailRow(
    label: String,
    value: String,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.spacing1)) {
        Text(
            text = label.uppercase(),
            style = typography.caption,
            color = colors.textSecondary,
        )
        Text(
            text = value,
            style = typography.bodyLarge,
            color = colors.textPrimary,
        )
    }
}

@Composable
private fun TransactionDetailStatusRow(transaction: Transaction) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.spacing1)) {
        Text(
            text = "STATUS",
            style = typography.caption,
            color = colors.textSecondary,
        )
        TransactionStatusBadge(status = transaction.status)
    }
}

@Composable
private fun Transaction.amountColor() = when (type) {
    TransactionType.INCOME -> BudgetCalendarTheme.colors.colorSuccessText
    TransactionType.EXPENSE -> BudgetCalendarTheme.colors.colorErrorText
    TransactionType.TRANSFER -> BudgetCalendarTheme.colors.colorInfoText
}

private fun Transaction.detailsAmountText(): String {
    val displayAmount = if (type == TransactionType.TRANSFER) amount else signedAmount
    return CurrencyUtils.formatCents(
        amountInCents = displayAmount,
        includePlusSign = type != TransactionType.TRANSFER && signedAmount >= 0,
    )
}

private val TransactionType.displayName: String
    get() = when (this) {
        TransactionType.INCOME -> "Income"
        TransactionType.EXPENSE -> "Expense"
        TransactionType.TRANSFER -> "Transfer"
    }

private fun formatTransactionDetailsDate(dateMillis: Long): String {
    return Instant
        .fromEpochMilliseconds(dateMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
        .toString()
}
