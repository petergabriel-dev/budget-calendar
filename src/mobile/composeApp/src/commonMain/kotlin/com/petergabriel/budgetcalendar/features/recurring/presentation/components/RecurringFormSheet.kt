package com.petergabriel.budgetcalendar.features.recurring.presentation.components

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
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
import com.petergabriel.budgetcalendar.core.designsystem.component.AccountPickerModal
import com.petergabriel.budgetcalendar.core.designsystem.component.BcOptionPickerModal
import com.petergabriel.budgetcalendar.core.designsystem.component.form.BcInputGroup
import com.petergabriel.budgetcalendar.core.designsystem.component.form.BcSelectField
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
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

    var showAccountPicker by remember { mutableStateOf(false) }
    var showDestinationPicker by remember { mutableStateOf(false) }
    var showDayPicker by remember { mutableStateOf(false) }

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
                .padding(
                    horizontal = BudgetCalendarTheme.spacing.spacing4,
                    vertical = BudgetCalendarTheme.spacing.spacing2,
                ),
            verticalArrangement = Arrangement.spacedBy(BudgetCalendarTheme.spacing.spacing3),
        ) {
            Text(
                text = if (initialData == null) "Add Recurring" else "Edit Recurring",
                style = BudgetCalendarTheme.typography.cardTitle,
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

            BcInputGroup(
                label = "Amount",
                value = amountInput,
                onValueChange = {
                    amountInput = it
                    localErrors.remove("amount")
                },
                placeholder = "",
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = localErrors.containsKey("amount"),
            )
            InlineError(localErrors["amount"])

            BcSelectField(
                label = "Day of month",
                value = dayOfMonth.toString(),
                onClick = { showDayPicker = true },
                modifier = Modifier.fillMaxWidth(),
            )
            InlineError(localErrors["dayOfMonth"])

            if (showDayPicker) {
                BcOptionPickerModal(
                    title = "Select Day of Month",
                    options = (1..31).map(Int::toString),
                    selectedIndex = dayOfMonth - 1,
                    onSelect = { index ->
                        dayOfMonth = index + 1
                        showDayPicker = false
                        localErrors.remove("dayOfMonth")
                    },
                    onDismiss = { showDayPicker = false },
                )
            }

            BcInputGroup(
                label = "Account",
                value = availableAccounts.firstOrNull { account -> account.id == selectedAccountId }?.name.orEmpty(),
                onValueChange = {},
                placeholder = "Select account",
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                onClick = if (availableAccounts.isNotEmpty()) {
                    { showAccountPicker = true }
                } else {
                    null
                },
                isError = localErrors.containsKey("accountId"),
            )
            InlineError(localErrors["accountId"])

            AccountPickerModal(
                isVisible = showAccountPicker,
                accounts = availableAccounts,
                selectedAccountId = selectedAccountId,
                title = "Select Account",
                onAccountSelected = { id ->
                    selectedAccountId = id
                    showAccountPicker = false
                    localErrors.remove("accountId")
                    if (destinationAccountId == id) {
                        destinationAccountId = null
                    }
                },
                onDismiss = { showAccountPicker = false },
            )

            if (selectedType == RecurrenceType.TRANSFER) {
                val destinationAccounts = availableAccounts.filter { account -> account.id != selectedAccountId }
                BcInputGroup(
                    label = "Destination Account",
                    value = destinationAccounts.firstOrNull { account -> account.id == destinationAccountId }?.name.orEmpty(),
                    onValueChange = {},
                    placeholder = "Select account",
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    onClick = if (destinationAccounts.isNotEmpty()) {
                        { showDestinationPicker = true }
                    } else {
                        null
                    },
                    isError = localErrors.containsKey("destinationAccountId"),
                )
                InlineError(localErrors["destinationAccountId"])

                AccountPickerModal(
                    isVisible = showDestinationPicker,
                    accounts = destinationAccounts,
                    selectedAccountId = destinationAccountId,
                    title = "Select Destination Account",
                    onAccountSelected = { id ->
                        destinationAccountId = id
                        showDestinationPicker = false
                        localErrors.remove("destinationAccountId")
                    },
                    onDismiss = { showDestinationPicker = false },
                )
            }

            BcInputGroup(
                label = "Description (optional)",
                value = description,
                onValueChange = {
                    description = it.take(200)
                    localErrors.remove("description")
                },
                placeholder = "",
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3,
                isError = localErrors.containsKey("description"),
                errorText = localErrors["description"],
            )
            Text(
                text = "${description.length}/200",
                style = BudgetCalendarTheme.typography.bodySmall,
                color = BudgetCalendarTheme.colors.textSecondary,
            )

            Spacer(modifier = Modifier.height(BudgetCalendarTheme.spacing.spacing1))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(BudgetCalendarTheme.spacing.spacing2),
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
                        color = BudgetCalendarTheme.colors.colorErrorText,
                    )
                }
            }

            Spacer(modifier = Modifier.height(BudgetCalendarTheme.spacing.spacing2))
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
private fun InlineError(message: String?) {
    if (message.isNullOrBlank()) {
        return
    }

    Text(
        text = message,
        color = BudgetCalendarTheme.colors.colorErrorText,
        style = BudgetCalendarTheme.typography.bodySmall,
        modifier = Modifier.padding(start = BudgetCalendarTheme.spacing.spacing1),
    )
}
