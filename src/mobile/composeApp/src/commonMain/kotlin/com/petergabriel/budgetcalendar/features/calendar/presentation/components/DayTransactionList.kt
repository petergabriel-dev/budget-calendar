package com.petergabriel.budgetcalendar.features.calendar.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import com.petergabriel.budgetcalendar.features.transactions.presentation.components.TransactionListItem

@Composable
fun DayTransactionList(
    transactions: List<Transaction>,
    modifier: Modifier = Modifier,
    accountNameProvider: (Long) -> String = { accountId -> "Account #$accountId" },
    onTransactionTap: (Transaction) -> Unit = {},
    onTransactionLongPress: (Transaction) -> Unit = {},
    onViewAllTransactions: (() -> Unit)? = null,
) {
    if (transactions.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No transactions",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val visibleItems = if (transactions.size > 20) transactions.take(20) else transactions

    Column(modifier = modifier.fillMaxWidth()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(visibleItems, key = { transaction -> transaction.id }) { transaction ->
                TransactionListItem(
                    transaction = transaction,
                    accountName = accountNameProvider(transaction.accountId),
                    onTap = { onTransactionTap(transaction) },
                    onLongPress = { onTransactionLongPress(transaction) },
                )
            }
        }

        if (transactions.size > 20) {
            TextButton(
                onClick = { onViewAllTransactions?.invoke() },
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(text = "View all ${transactions.size} transactions")
            }
        }
    }
}
