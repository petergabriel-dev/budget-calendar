package com.petergabriel.budgetcalendar.features.accounts.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import com.petergabriel.budgetcalendar.core.designsystem.component.BcButton
import com.petergabriel.budgetcalendar.core.designsystem.component.BcConfirmDialog
import com.petergabriel.budgetcalendar.core.designsystem.component.BcSheetFooter
import com.petergabriel.budgetcalendar.core.designsystem.component.BcSheetShell
import com.petergabriel.budgetcalendar.core.designsystem.component.ButtonVariant
import com.petergabriel.budgetcalendar.core.designsystem.component.form.BcInputGroup
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.MoneyInputUtils
import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account
import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountFormSheet(
    isVisible: Boolean,
    initialData: Account?,
    isSubmitting: Boolean,
    errorMessage: String? = null,
    onSave: (
        name: String,
        type: AccountType,
        balance: Long,
        isInSpendingPool: Boolean,
        description: String,
        creditLimit: Long?,
        statementClosingDay: Int?,
        paymentDueDay: Int?,
    ) -> Unit,
    onCancel: () -> Unit,
) {
    if (!isVisible) return

    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing
    var name by remember(initialData?.id) { mutableStateOf(initialData?.name.orEmpty()) }
    var selectedType by remember(initialData?.id) { mutableStateOf(initialData?.type ?: AccountType.CHECKING) }
    var balanceInput by remember(initialData?.id) {
        mutableStateOf(MoneyInputUtils.centsToInput(initialData?.balance ?: 0L))
    }
    var includeInSpendingPool by remember(initialData?.id) {
        mutableStateOf(initialData?.isInSpendingPool ?: true)
    }
    var descriptionInput by remember(initialData?.id) {
        mutableStateOf(initialData?.description.orEmpty())
    }
    var creditLimitInput by remember(initialData?.id) { mutableStateOf("") }
    var statementClosingDayInput by remember(initialData?.id) { mutableStateOf("") }
    var paymentDueDayInput by remember(initialData?.id) { mutableStateOf("") }
    val validationErrors = remember(initialData?.id) { mutableStateMapOf<String, String>() }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var showDiscardConfirmation by remember(initialData?.id) { mutableStateOf(false) }

    LaunchedEffect(initialData?.id) {
        validationErrors.clear()
    }

    LaunchedEffect(selectedType) {
        if (selectedType == AccountType.CREDIT_CARD) {
            includeInSpendingPool = false
        }
    }

    val isDirty = name != initialData?.name.orEmpty() ||
        selectedType != (initialData?.type ?: AccountType.CHECKING) ||
        balanceInput != MoneyInputUtils.centsToInput(initialData?.balance ?: 0L) ||
        includeInSpendingPool != (initialData?.isInSpendingPool ?: true) ||
        descriptionInput != initialData?.description.orEmpty() ||
        creditLimitInput.isNotEmpty() ||
        statementClosingDayInput.isNotEmpty() ||
        paymentDueDayInput.isNotEmpty()

    fun requestDismiss() {
        if (isSubmitting) return
        if (isDirty) {
            showDiscardConfirmation = true
        } else {
            onCancel()
        }
    }

    BcSheetShell(
        title = if (initialData == null) "Add Account" else "Edit Account",
        onDismissRequest = ::requestDismiss,
        body = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.spacing3),
            ) {
                errorMessage?.let { error ->
                    Text(
                        text = error,
                        style = typography.bodySmall,
                        color = colors.colorErrorText,
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        validationErrors.remove("name")
                    },
                    label = { Text("Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(ACCOUNT_NAME_FIELD_TAG),
                    isError = validationErrors.containsKey("name"),
                    singleLine = true,
                )
                InlineError(validationErrors["name"])

                ExposedDropdownMenuBox(
                    expanded = typeMenuExpanded,
                    onExpandedChange = { typeMenuExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedType.displayName(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag(ACCOUNT_TYPE_FIELD_TAG),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded)
                        },
                    )
                    ExposedDropdownMenu(
                        expanded = typeMenuExpanded,
                        onDismissRequest = { typeMenuExpanded = false },
                    ) {
                        AccountType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName()) },
                                modifier = Modifier.testTag(accountTypeOptionTag(type)),
                                onClick = {
                                    selectedType = type
                                    typeMenuExpanded = false
                                },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = balanceInput,
                    onValueChange = {
                        balanceInput = it
                        validationErrors.remove("balance")
                    },
                    label = {
                        Text(
                            text = if (selectedType == AccountType.CREDIT_CARD && initialData == null) {
                                "Current balance owed"
                            } else if (initialData == null) {
                                "Initial Balance"
                            } else {
                                "Balance"
                            },
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(ACCOUNT_BALANCE_FIELD_TAG),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = validationErrors.containsKey("balance"),
                    singleLine = true,
                )
                InlineError(validationErrors["balance"])

                if (selectedType == AccountType.CREDIT_CARD && initialData == null) {
                    OutlinedTextField(
                        value = creditLimitInput,
                        onValueChange = {
                            creditLimitInput = it
                            validationErrors.remove("creditLimit")
                        },
                        label = { Text("Credit Limit") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(ACCOUNT_CREDIT_LIMIT_FIELD_TAG),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = validationErrors.containsKey("creditLimit"),
                        singleLine = true,
                    )
                    InlineError(validationErrors["creditLimit"])

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.spacing2),
                    ) {
                        OutlinedTextField(
                            value = statementClosingDayInput,
                            onValueChange = {
                                statementClosingDayInput = it
                                validationErrors.remove("statementClosingDay")
                            },
                            label = { Text("Closing Day") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag(ACCOUNT_CLOSING_DAY_FIELD_TAG),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = validationErrors.containsKey("statementClosingDay"),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = paymentDueDayInput,
                            onValueChange = {
                                paymentDueDayInput = it
                                validationErrors.remove("paymentDueDay")
                            },
                            label = { Text("Due Day") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag(ACCOUNT_DUE_DAY_FIELD_TAG),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = validationErrors.containsKey("paymentDueDay"),
                            singleLine = true,
                        )
                    }
                    InlineError(validationErrors["statementClosingDay"] ?: validationErrors["paymentDueDay"])
                }

                BcInputGroup(
                    label = "Description",
                    value = descriptionInput,
                    onValueChange = { descriptionInput = it },
                    placeholder = "e.g. Primary funding source",
                    modifier = Modifier.fillMaxWidth(),
                    isError = descriptionInput.length > ACCOUNT_DESCRIPTION_SOFT_CAP,
                    errorText = if (descriptionInput.length > ACCOUNT_DESCRIPTION_SOFT_CAP) {
                        "Recommended max 100 characters"
                    } else {
                        null
                    },
                )
                Text(
                    text = "${descriptionInput.length}/$ACCOUNT_DESCRIPTION_SOFT_CAP",
                    style = typography.bodySmall,
                    color = if (descriptionInput.length > ACCOUNT_DESCRIPTION_SOFT_CAP) {
                        colors.colorErrorText
                    } else {
                        colors.textSecondary
                    },
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (selectedType != AccountType.CREDIT_CARD) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Include in Safe to Spend",
                                style = typography.bodyMedium,
                                color = colors.textPrimary,
                            )
                            Text(
                                text = "Only enabled accounts are included in available funds.",
                                style = typography.bodySmall,
                                color = colors.textSecondary,
                            )
                        }
                        Switch(
                            modifier = Modifier.testTag(INCLUDE_IN_SPENDING_POOL_SWITCH_TAG),
                            checked = includeInSpendingPool,
                            onCheckedChange = { includeInSpendingPool = it },
                        )
                    }
                }
            }
        },
        footer = {
            BcSheetFooter {
                BcButton(
                    text = "Cancel",
                    onClick = ::requestDismiss,
                    modifier = Modifier.weight(1f),
                    variant = ButtonVariant.Outline,
                    enabled = !isSubmitting,
                )
                BcButton(
                    text = if (isSubmitting) "Saving…" else if (initialData == null) "Add Account" else "Save",
                    onClick = {
                        val errors = validateAccountForm(
                            name = name,
                            balanceInput = balanceInput,
                            isCreateCreditCard = selectedType == AccountType.CREDIT_CARD && initialData == null,
                            creditLimitInput = creditLimitInput,
                            statementClosingDayInput = statementClosingDayInput,
                            paymentDueDayInput = paymentDueDayInput,
                        )
                        validationErrors.clear()
                        validationErrors.putAll(errors)
                        if (errors.isNotEmpty()) return@BcButton

                        val balance = MoneyInputUtils.parseToCents(balanceInput.trim()) ?: return@BcButton
                        onSave(
                            name.trim(),
                            selectedType,
                            balance,
                            selectedType != AccountType.CREDIT_CARD && includeInSpendingPool,
                            descriptionInput,
                            MoneyInputUtils.parseToCents(creditLimitInput.trim()),
                            statementClosingDayInput.trim().toIntOrNull(),
                            paymentDueDayInput.trim().toIntOrNull(),
                        )
                    },
                    modifier = Modifier.weight(1f),
                    variant = ButtonVariant.Primary,
                    enabled = !isSubmitting,
                )
            }
        },
    )

    if (showDiscardConfirmation) {
        BcConfirmDialog(
            title = "Discard changes?",
            message = "Your account changes will be lost.",
            onDismissRequest = { showDiscardConfirmation = false },
            onConfirm = onCancel,
            confirmLabel = "Discard",
            dismissLabel = "Keep editing",
        )
    }
}

private fun validateAccountForm(
    name: String,
    balanceInput: String,
    isCreateCreditCard: Boolean,
    creditLimitInput: String,
    statementClosingDayInput: String,
    paymentDueDayInput: String,
): Map<String, String> {
    val errors = mutableMapOf<String, String>()

    if (name.trim().isEmpty()) errors["name"] = "Name is required"
    if (MoneyInputUtils.parseToCents(balanceInput.trim()) == null) errors["balance"] = "Enter a valid amount"

    if (isCreateCreditCard) {
        val creditLimit = MoneyInputUtils.parseToCents(creditLimitInput.trim())
        if (creditLimit == null || creditLimit <= 0L) errors["creditLimit"] = "Credit limit is required"
        if (statementClosingDayInput.trim().toIntOrNull() !in 1..31) {
            errors["statementClosingDay"] = "Closing day must be 1-31"
        }
        if (paymentDueDayInput.trim().toIntOrNull() !in 1..31) {
            errors["paymentDueDay"] = "Due day must be 1-31"
        }
    }

    return errors
}

@Composable
private fun InlineError(message: String?) {
    if (message.isNullOrBlank()) return
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    Text(
        text = message,
        color = colors.colorErrorText,
        style = typography.bodySmall,
        modifier = Modifier.padding(start = BudgetCalendarTheme.spacing.spacing1),
    )
}

private fun AccountType.displayName(): String {
    return name
        .lowercase()
        .replace('_', ' ')
        .split(' ')
        .joinToString(" ") { part ->
            part.replaceFirstChar { first ->
                if (first.isLowerCase()) first.titlecase() else first.toString()
            }
        }
}

private fun accountTypeOptionTag(type: AccountType): String = "account_type_option_${type.name}"

private const val ACCOUNT_DESCRIPTION_SOFT_CAP = 100
private const val ACCOUNT_NAME_FIELD_TAG = "account_name_field"
private const val ACCOUNT_TYPE_FIELD_TAG = "account_type_field"
private const val ACCOUNT_BALANCE_FIELD_TAG = "account_balance_field"
private const val ACCOUNT_CREDIT_LIMIT_FIELD_TAG = "account_credit_limit_field"
private const val ACCOUNT_CLOSING_DAY_FIELD_TAG = "account_closing_day_field"
private const val ACCOUNT_DUE_DAY_FIELD_TAG = "account_due_day_field"
private const val INCLUDE_IN_SPENDING_POOL_SWITCH_TAG = "include_in_spending_pool_switch"
