package com.petergabriel.budgetcalendar.features.creditcard.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils
import com.petergabriel.budgetcalendar.core.utils.MoneyInputUtils
import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account
import com.petergabriel.budgetcalendar.features.creditcard.domain.model.CreditCardSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardPaymentSheet(
    isVisible: Boolean,
    ccSummary: CreditCardSummary,
    suggestedAmount: Long,
    spendingPoolAccounts: List<Account>,
    onConfirmPayment: (sourceAccountId: Long, amount: Long) -> Unit,
    onCancel: () -> Unit,
) {
    if (!isVisible) {
        return
    }

    var amountInput by remember(ccSummary.accountId, suggestedAmount) {
        mutableStateOf(MoneyInputUtils.centsToInput(suggestedAmount))
    }
    var selectedSourceAccountId by remember(ccSummary.accountId) {
        mutableStateOf(spendingPoolAccounts.firstOrNull()?.id)
    }
    var accountMenuExpanded by remember { mutableStateOf(false) }
    val validationErrors = remember(ccSummary.accountId) { mutableStateMapOf<String, String>() }

    LaunchedEffect(spendingPoolAccounts) {
        if (selectedSourceAccountId == null || spendingPoolAccounts.none { account -> account.id == selectedSourceAccountId }) {
            selectedSourceAccountId = spendingPoolAccounts.firstOrNull()?.id
        }
    }

    val selectedSourceLabel = spendingPoolAccounts
        .firstOrNull { account -> account.id == selectedSourceAccountId }
        ?.name
        ?: "Select source account"
    val parsedAmount = MoneyInputUtils.parseToCents(amountInput.trim())
    val exceedsReserved = parsedAmount != null && parsedAmount > ccSummary.reservedAmount

    ModalBottomSheet(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Pay ${ccSummary.accountName}",
                style = MaterialTheme.typography.titleLarge,
            )

            Text(
                text = "Suggested payment: ${CurrencyUtils.formatCents(suggestedAmount)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = amountInput,
                onValueChange = {
                    amountInput = it
                    validationErrors.remove("amount")
                },
                label = { Text("Payment Amount") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = validationErrors.containsKey("amount"),
                singleLine = true,
            )
            InlineError(validationErrors["amount"])

            OutlinedTextField(
                value = selectedSourceLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Source Account") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { accountMenuExpanded = true },
                trailingIcon = { Text("v") },
                isError = validationErrors.containsKey("source"),
            )
            DropdownMenu(
                expanded = accountMenuExpanded,
                onDismissRequest = { accountMenuExpanded = false },
            ) {
                spendingPoolAccounts.forEach { account ->
                    DropdownMenuItem(
                        text = { Text(account.name) },
                        onClick = {
                            selectedSourceAccountId = account.id
                            accountMenuExpanded = false
                            validationErrors.remove("source")
                        },
                    )
                }
            }
            InlineError(validationErrors["source"])

            if (spendingPoolAccounts.isEmpty()) {
                Text(
                    text = "No spending pool account available. Add one in Accounts before paying a card.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            } else if (exceedsReserved) {
                Text(
                    text = "Warning: payment exceeds reserved amount (${CurrencyUtils.formatCents(ccSummary.reservedAmount)}).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        val amount = MoneyInputUtils.parseToCents(amountInput.trim())
                        val errors = mutableMapOf<String, String>()
                        if (amount == null || amount <= 0L) {
                            errors["amount"] = "Enter a valid amount greater than 0"
                        }
                        if (selectedSourceAccountId == null) {
                            errors["source"] = "Select a source account"
                        }
                        validationErrors.clear()
                        validationErrors.putAll(errors)
                        if (errors.isNotEmpty()) {
                            return@Button
                        }

                        onConfirmPayment(
                            checkNotNull(selectedSourceAccountId),
                            checkNotNull(amount),
                        )
                    },
                    modifier = Modifier.weight(1f),
                    enabled = spendingPoolAccounts.isNotEmpty(),
                ) {
                    Text("Confirm")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun InlineError(message: String?) {
    if (message.isNullOrBlank()) {
        return
    }

    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(start = 4.dp),
    )
}
