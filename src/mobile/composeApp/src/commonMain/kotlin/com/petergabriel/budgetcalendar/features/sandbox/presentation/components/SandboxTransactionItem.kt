package com.petergabriel.budgetcalendar.features.sandbox.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.petergabriel.budgetcalendar.core.designsystem.component.BadgeVariant
import com.petergabriel.budgetcalendar.core.designsystem.component.BcBadge
import com.petergabriel.budgetcalendar.core.designsystem.component.BcButton
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
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing
    val radius = BudgetCalendarTheme.radius

    var showPromoteConfirm by remember { mutableStateOf(false) }
    val signedAmount = when (transaction.type) {
        TransactionType.INCOME -> transaction.amount
        TransactionType.EXPENSE -> -transaction.amount
        TransactionType.TRANSFER -> transaction.amount
    }
    val amountColor = if (signedAmount >= 0L) colors.colorSuccess else colors.colorError

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = colors.bgSurface,
                shape = RoundedCornerShape(radius.lg),
            )
            .padding(spacing.spacing3),
        verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                BcBadge(
                    text = if (transaction.type == TransactionType.INCOME) "Income" else "Expense",
                    variant = if (transaction.type == TransactionType.INCOME) BadgeVariant.Success else BadgeVariant.Error,
                )

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
                    )
                }
                Text(
                    text = formatDate(transaction.date),
                    style = typography.bodySmall,
                    color = colors.textTertiary,
                )
            }

            Text(
                text = CurrencyUtils.formatCents(
                    amountInCents = signedAmount,
                    includePlusSign = signedAmount >= 0L,
                ),
                style = typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            if (transaction.originalTransactionId == null) {
                BcButton(
                    text = "Promote",
                    onClick = { showPromoteConfirm = true },
                    variant = ButtonVariant.Ghost,
                )
            }

            BcButton(
                text = "Remove",
                onClick = { onRemove(transaction.id) },
                variant = ButtonVariant.Destructive,
            )
        }
    }

    if (showPromoteConfirm) {
        AlertDialog(
            onDismissRequest = { showPromoteConfirm = false },
            title = { Text("Promote transaction?") },
            text = { Text("This adds the transaction to real data and removes it from sandbox.") },
            dismissButton = {
                TextButton(onClick = { showPromoteConfirm = false }) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onPromote(transaction.id)
                        showPromoteConfirm = false
                    },
                ) {
                    Text("Promote")
                }
            },
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
