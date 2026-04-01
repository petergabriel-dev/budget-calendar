package com.petergabriel.budgetcalendar.features.sandbox.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
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
import com.petergabriel.budgetcalendar.core.designsystem.component.BcButton
import com.petergabriel.budgetcalendar.core.designsystem.component.ButtonVariant
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxTransaction
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SandboxTransactionItem(
    transaction: SandboxTransaction,
    modifier: Modifier = Modifier,
    onPromote: (SandboxTransaction) -> Unit,
    onRemove: (SandboxTransaction) -> Unit,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing
    val radius = BudgetCalendarTheme.radius

    var showPromoteSheet by remember { mutableStateOf(false) }
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
                TextButton(onClick = { showPromoteSheet = true }) {
                    Text("Promote", color = colors.colorInfo)
                }
            }

            TextButton(
                onClick = { onRemove(transaction) },
            ) {
                Text("Remove", color = colors.colorError)
            }
        }
    }

    if (showPromoteSheet) {
        ModalBottomSheet(onDismissRequest = { showPromoteSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.spacing4),
                verticalArrangement = Arrangement.spacedBy(spacing.spacing3),
            ) {
                Text(
                    text = "Promote transaction to real data?",
                    style = typography.section,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
                Text(
                    text = "This will add it to your real transactions and remove it from this sandbox.",
                    style = typography.bodyMedium,
                    color = colors.textSecondary,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.spacing2),
                ) {
                    BcButton(
                        text = "Cancel",
                        onClick = { showPromoteSheet = false },
                        modifier = Modifier.weight(1f),
                        variant = ButtonVariant.Outline,
                    )

                    BcButton(
                        text = "Promote",
                        onClick = {
                            onPromote(transaction)
                            showPromoteSheet = false
                        },
                        modifier = Modifier.weight(1f),
                        variant = ButtonVariant.Primary,
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
