package com.petergabriel.budgetcalendar.features.recurring.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.petergabriel.budgetcalendar.core.utils.MoneyInputUtils
import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account
import com.petergabriel.budgetcalendar.features.recurring.domain.model.CreateRecurringTransactionRequest
import com.petergabriel.budgetcalendar.features.recurring.domain.model.RecurringTransaction
import com.petergabriel.budgetcalendar.features.recurring.domain.model.RecurrenceType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringFormSheet(
    isVisible: Boolean,
    availableAccounts: List<Account>,
    isSubmitting: Boolean,
    error: String?,
    initialData: RecurringTransaction? = null,
    onSave: (CreateRecurringTransactionRequest) -> Unit,
    onCancel: () -> Unit,
    onDelete: ((RecurringTransaction) -> Unit)? = null,
) {
    if (!isVisible) {
        return
    }

    val stateKey = initialData?.id ?: 0L
    var selectedType by remember(stateKey) { mutableStateOf(initialData?.type ?: RecurrenceType.EXPENSE) }
    var amountInput by remember(stateKey) {
        mutableStateOf(initialData?.amount?.let(MoneyInputUtils::centsToInput).orEmpty())
    }
    var dayOfMonth by remember(stateKey) { mutableStateOf(initialData?.dayOfMonth ?: 1) }
    var selectedAccountId by remember(stateKey) { mutableStateOf(initialData?.accountId) }
    var destinationAccountId by remember(stateKey) { mutableStateOf(initialData?.destinationAccountId) }
    var description by remember(stateKey) { mutableStateOf(initialData?.description.orEmpty()) }

    val localErrors = remember(stateKey) { mutableStateMapOf<String, String>() }

    var accountMenuExpanded by remember { mutableStateOf(false) }
    var destinationMenuExpanded by remember { mutableStateOf(false) }
    var dayMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(stateKey, availableAccounts) {
        localErrors.clear()
        if (selectedAccountId == null && availableAccounts.isNotEmpty()) {
            selectedAccountId = availableAccounts.first().id
        }
    }

    ModalBottomSheet(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = if (initialData == null) "Add Recurring" else "Edit Recurring",
                style = MaterialTheme.typography.titleLarge,
            )

            if (!error.isNullOrBlank()) {
                InlineError(error)
            }

            val types = listOf(RecurrenceType.INCOME, RecurrenceType.EXPENSE, RecurrenceType.TRANSFER)
            TabRow(selectedTabIndex = types.indexOf(selectedType)) {
                types.forEach { type ->
                    Tab(
                        selected = selectedType == type,
                        onClick = {
                            selectedType = type
                            if (type != RecurrenceType.TRANSFER) {
                                destinationAccountId = null
                            }
                            localErrors.remove("destinationAccountId")
                        },
                        text = { Text(type.name) },
                    )
                }
            }

            OutlinedTextField(
                value = amountInput,
                onValueChange = {
                    amountInput = it
                    localErrors.remove("amount")
                },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = localErrors.containsKey("amount"),
                singleLine = true,
            )
            InlineError(localErrors["amount"])

            OutlinedTextField(
                value = dayOfMonth.toString(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Day of month") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { dayMenuExpanded = true },
                trailingIcon = { Text("v") },
                isError = localErrors.containsKey("dayOfMonth"),
            )
            DropdownMenu(
                expanded = dayMenuExpanded,
                onDismissRequest = { dayMenuExpanded = false },
            ) {
                (1..31).forEach { day ->
                    DropdownMenuItem(
                        text = { Text(day.toString()) },
                        onClick = {
                            dayOfMonth = day
                            dayMenuExpanded = false
                            localErrors.remove("dayOfMonth")
                        },
                    )
                }
            }
            InlineError(localErrors["dayOfMonth"])

            AccountDropdown(
                label = "Account",
                accounts = availableAccounts,
                selectedAccountId = selectedAccountId,
                expanded = accountMenuExpanded,
                onExpandedChange = { accountMenuExpanded = it },
                onAccountSelected = { account ->
                    selectedAccountId = account.id
                    accountMenuExpanded = false
                    localErrors.remove("accountId")
                    if (destinationAccountId == account.id) {
                        destinationAccountId = null
                    }
                },
                isError = localErrors.containsKey("accountId"),
                errorText = localErrors["accountId"],
            )

            if (selectedType == RecurrenceType.TRANSFER) {
                AccountDropdown(
                    label = "Destination Account",
                    accounts = availableAccounts.filter { account -> account.id != selectedAccountId },
                    selectedAccountId = destinationAccountId,
                    expanded = destinationMenuExpanded,
                    onExpandedChange = { destinationMenuExpanded = it },
                    onAccountSelected = { account ->
                        destinationAccountId = account.id
                        destinationMenuExpanded = false
                        localErrors.remove("destinationAccountId")
                    },
                    isError = localErrors.containsKey("destinationAccountId"),
                    errorText = localErrors["destinationAccountId"],
                )
            }

            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it.take(200)
                    localErrors.remove("description")
                },
                label = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3,
                isError = localErrors.containsKey("description"),
            )
            InlineError(localErrors["description"])
            Text(
                text = "${description.length}/200",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    enabled = !isSubmitting,
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        val errors = validateRecurringForm(
                            selectedType = selectedType,
                            amountInput = amountInput,
                            dayOfMonth = dayOfMonth,
                            accountId = selectedAccountId,
                            destinationAccountId = destinationAccountId,
                            description = description,
                        )

                        localErrors.clear()
                        localErrors.putAll(errors)
                        if (errors.isNotEmpty()) {
                            return@Button
                        }

                        onSave(
                            CreateRecurringTransactionRequest(
                                accountId = checkNotNull(selectedAccountId),
                                destinationAccountId = if (selectedType == RecurrenceType.TRANSFER) destinationAccountId else null,
                                amount = checkNotNull(MoneyInputUtils.parseToCents(amountInput.trim())),
                                dayOfMonth = dayOfMonth,
                                type = selectedType,
                                description = description.trim().ifBlank { null },
                            ),
                        )
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isSubmitting,
                ) {
                    Text(text = if (isSubmitting) "Saving..." else "Save")
                }
            }

            if (initialData != null && onDelete != null) {
                TextButton(
                    onClick = { onDelete(initialData) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting,
                ) {
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private fun validateRecurringForm(
    selectedType: RecurrenceType,
    amountInput: String,
    dayOfMonth: Int,
    accountId: Long?,
    destinationAccountId: Long?,
    description: String,
): Map<String, String> {
    val errors = mutableMapOf<String, String>()

    val amount = MoneyInputUtils.parseToCents(amountInput.trim())
    if (amount == null || amount <= 0L) {
        errors["amount"] = "Amount must be greater than zero"
    }

    if (dayOfMonth !in 1..31) {
        errors["dayOfMonth"] = "Day of month must be between 1 and 31"
    }

    if (accountId == null) {
        errors["accountId"] = "Account is required"
    }

    if (selectedType == RecurrenceType.TRANSFER) {
        if (destinationAccountId == null) {
            errors["destinationAccountId"] = "Destination account is required"
        } else if (destinationAccountId == accountId) {
            errors["destinationAccountId"] = "Cannot transfer to the same account"
        }
    }

    if (description.length > 200) {
        errors["description"] = "Description cannot exceed 200 characters"
    }

    return errors
}

@Composable
private fun AccountDropdown(
    label: String,
    accounts: List<Account>,
    selectedAccountId: Long?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAccountSelected: (Account) -> Unit,
    isError: Boolean,
    errorText: String?,
) {
    val selectedLabel = accounts.firstOrNull { account -> account.id == selectedAccountId }?.name ?: "Select account"

    OutlinedTextField(
        value = selectedLabel,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpandedChange(true) },
        trailingIcon = { Text("v") },
        isError = isError,
    )

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { onExpandedChange(false) },
    ) {
        accounts.forEach { account ->
            DropdownMenuItem(
                text = { Text(account.name) },
                onClick = { onAccountSelected(account) },
            )
        }
    }

    InlineError(errorText)
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
