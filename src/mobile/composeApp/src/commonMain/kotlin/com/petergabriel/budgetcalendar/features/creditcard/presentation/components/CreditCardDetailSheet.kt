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
import com.petergabriel.budgetcalendar.features.creditcard.domain.model.CreditCardSettings
import com.petergabriel.budgetcalendar.features.creditcard.domain.model.CreditCardSummary
import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import com.petergabriel.budgetcalendar.features.transactions.presentation.components.TransactionListItem
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

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
                label = "Reserved Amount",
                value = CurrencyUtils.formatCents(summary.reservedAmount),
                valueColor = Color(0xFFB26A00),
            )
            SummaryRow(
                label = "Statement Balance",
                value = settings?.statementBalance?.let(CurrencyUtils::formatCents) ?: "Not set",
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
                label = "Due Date",
                value = settings?.dueDate?.let(::formatDate) ?: "Not set",
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
                        onLongPress = {},
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

private fun formatDate(dateMillis: Long): String {
    return Instant
        .fromEpochMilliseconds(dateMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
        .toString()
}
