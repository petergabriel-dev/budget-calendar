package com.petergabriel.budgetcalendar.features.creditcard.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils
import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.creditcard.domain.model.CreditCardSettings
import com.petergabriel.budgetcalendar.features.creditcard.domain.model.CreditCardSummary
import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import com.petergabriel.budgetcalendar.features.transactions.presentation.components.TransactionListItem
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardDetailSheet(
    isVisible: Boolean,
    summary: CreditCardSummary,
    settings: CreditCardSettings?,
    pendingTransactions: List<Transaction>,
    onMakePayment: () -> Unit,
    onEditSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!isVisible) {
        return
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = summary.accountName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            SummaryRow(
                label = "Current Balance",
                value = CurrencyUtils.formatCents(summary.currentBalance),
                valueColor = if (summary.currentBalance <= 0L) Color(0xFFC62828) else Color(0xFF2E7D32),
            )
            SummaryRow(
                label = "Statement Balance",
                value = statementDueText(summary),
            )
            SummaryRow(
                label = "Credit Limit",
                value = settings?.creditLimit?.let(CurrencyUtils::formatCents) ?: "Not set",
            )
            SummaryRow(
                label = "Available Credit",
                value = summary.availableCredit?.let(CurrencyUtils::formatCents) ?: "Not set",
            )
            SummaryRow(
                label = "Utilization",
                value = utilizationText(summary),
            )
            SummaryRow(
                label = "🗓 Next Due Date",
                value = nextDueDateText(
                    statementClosingDay = summary.statementClosingDay,
                    paymentDueDay = summary.paymentDueDay,
                ) ?: "Not set",
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onMakePayment,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Make Payment")
                }
                OutlinedButton(
                    onClick = onEditSettings,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Edit Settings")
                }
            }

            Text(
                text = "Pending & Overdue Expenses",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 6.dp),
            )

            if (pendingTransactions.isEmpty()) {
                Text(
                    text = "No pending or overdue expenses for this card.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                pendingTransactions.forEach { transaction ->
                    TransactionListItem(
                        transaction = transaction,
                        accountName = summary.accountName,
                        onTap = {},
                    )
                }
            }

            androidx.compose.foundation.layout.Box(modifier = Modifier.size(4.dp))
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
        )
    }
}

private fun statementDueText(summary: CreditCardSummary): String {
    val amount = summary.statementDueAmount ?: return "Not set"
    val dueDate = summary.statementDueDate?.let(DateUtils::formatMonthDay)
    return if (dueDate == null) {
        CurrencyUtils.formatCents(amount)
    } else {
        "${CurrencyUtils.formatCents(amount)} due $dueDate"
    }
}

private fun utilizationText(summary: CreditCardSummary): String {
    val limit = summary.creditLimit?.takeIf { amount -> amount > 0L } ?: return "Not set"
    val utilization = (abs(summary.currentBalance).toDouble() / limit.toDouble()).coerceIn(0.0, 1.0)
    return "${(utilization * 100).toInt()}%"
}

private fun nextDueDateText(
    statementClosingDay: Int?,
    paymentDueDay: Int?,
    nowMillis: Long = DateUtils.nowMillis(),
): String? {
    if (statementClosingDay == null || paymentDueDay == null) return null
    val lastClose = DateUtils.lastStatementClose(statementClosingDay, nowMillis)
    val dueMillis = DateUtils.paymentDueAfter(paymentDueDay, lastClose)
    return DateUtils.formatMonthDay(dueMillis)
}
