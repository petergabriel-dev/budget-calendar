package com.petergabriel.budgetcalendar.features.creditcard.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.petergabriel.budgetcalendar.core.designsystem.component.BcButton
import com.petergabriel.budgetcalendar.core.designsystem.component.BcConfirmDialog
import com.petergabriel.budgetcalendar.core.designsystem.component.BcSheetFooter
import com.petergabriel.budgetcalendar.core.designsystem.component.BcSheetShell
import com.petergabriel.budgetcalendar.core.designsystem.component.ButtonVariant
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.MoneyInputUtils
import com.petergabriel.budgetcalendar.features.creditcard.domain.model.CreditCardSettings
import com.petergabriel.budgetcalendar.features.creditcard.domain.model.UpdateCreditCardSettingsRequest

@Composable
fun CreditCardSettingsForm(
    isVisible: Boolean,
    initialSettings: CreditCardSettings?,
    isSubmitting: Boolean = false,
    errorMessage: String? = null,
    onSave: (UpdateCreditCardSettingsRequest) -> Unit,
    onCancel: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    if (!isVisible) return

    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing
    var creditLimitInput by remember(initialSettings?.accountId) {
        mutableStateOf(initialSettings?.creditLimit?.let(MoneyInputUtils::centsToInput).orEmpty())
    }
    var statementClosingDayInput by remember(initialSettings?.accountId) {
        mutableStateOf(initialSettings?.statementClosingDay?.toString().orEmpty())
    }
    var paymentDueDayInput by remember(initialSettings?.accountId) {
        mutableStateOf(initialSettings?.paymentDueDay?.toString().orEmpty())
    }
    val validationErrors = remember(initialSettings?.accountId) { mutableStateMapOf<String, String>() }
    var showDiscardConfirmation by remember(initialSettings?.accountId) { mutableStateOf(false) }
    var showDeleteConfirmation by remember(initialSettings?.accountId) { mutableStateOf(false) }

    val isDirty = creditLimitInput != initialSettings?.creditLimit?.let(MoneyInputUtils::centsToInput).orEmpty() ||
        statementClosingDayInput != initialSettings?.statementClosingDay?.toString().orEmpty() ||
        paymentDueDayInput != initialSettings?.paymentDueDay?.toString().orEmpty()

    fun requestDismiss() {
        if (isSubmitting) return
        if (isDirty) showDiscardConfirmation = true else onCancel()
    }

    BcSheetShell(
        title = "Credit Card Settings",
        onDismissRequest = ::requestDismiss,
        body = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.spacing3),
            ) {
                errorMessage?.let { error ->
                    Text(text = error, style = typography.bodySmall, color = colors.colorErrorText)
                }
                OutlinedTextField(
                    value = creditLimitInput,
                    onValueChange = {
                        creditLimitInput = it
                        validationErrors.remove("creditLimit")
                    },
                    label = { Text("Credit Limit") },
                    modifier = Modifier.fillMaxWidth(),
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
                        label = { Text("Closing day (1-31)") },
                        modifier = Modifier.weight(1f),
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
                        label = { Text("Due day (1-31)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = validationErrors.containsKey("paymentDueDay"),
                        singleLine = true,
                    )
                }
                InlineError(validationErrors["statementClosingDay"] ?: validationErrors["paymentDueDay"])

                onDelete?.let { delete ->
                    BcButton(
                        text = "Delete Card",
                        onClick = { showDeleteConfirmation = true },
                        modifier = Modifier.fillMaxWidth(),
                        variant = ButtonVariant.Destructive,
                        enabled = !isSubmitting,
                    )
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
                    text = if (isSubmitting) "Saving…" else "Save",
                    onClick = {
                        val errors = mutableMapOf<String, String>()
                        val creditLimit = parseRequiredMoney(creditLimitInput, "credit limit", errors, "creditLimit")
                        val statementClosingDay = parseRequiredDay(statementClosingDayInput, "closing day", errors, "statementClosingDay")
                        val paymentDueDay = parseRequiredDay(paymentDueDayInput, "due day", errors, "paymentDueDay")
                        if (creditLimit != null && creditLimit <= 0L) {
                            errors["creditLimit"] = "Credit limit must be greater than 0"
                        }
                        validationErrors.clear()
                        validationErrors.putAll(errors)
                        if (errors.isNotEmpty()) return@BcButton

                        onSave(
                            UpdateCreditCardSettingsRequest(
                                creditLimit = creditLimit,
                                statementBalance = initialSettings?.statementBalance,
                                dueDate = initialSettings?.dueDate,
                                statementClosingDay = statementClosingDay,
                                paymentDueDay = paymentDueDay,
                            ),
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
            title = "Discard settings?",
            message = "Your card settings changes will be lost.",
            onDismissRequest = { showDiscardConfirmation = false },
            onConfirm = onCancel,
            confirmLabel = "Discard",
            dismissLabel = "Keep editing",
        )
    }

    if (showDeleteConfirmation && onDelete != null) {
        BcConfirmDialog(
            title = "Delete card?",
            message = "This action cannot be undone. Delete card only after confirming its consequences.",
            onDismissRequest = { if (!isSubmitting) showDeleteConfirmation = false },
            onConfirm = onDelete,
            confirmLabel = "Delete card",
            dismissLabel = "Keep card",
            isBusy = isSubmitting,
            busyLabel = "Deleting…",
        )
    }
}

private fun parseRequiredMoney(
    value: String,
    fieldName: String,
    errors: MutableMap<String, String>,
    key: String,
): Long? {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) {
        errors[key] = "${fieldName.replaceFirstChar { it.titlecase() }} is required"
        return null
    }
    val parsed = MoneyInputUtils.parseToCents(trimmed)
    if (parsed == null) errors[key] = "Enter a valid $fieldName amount"
    return parsed
}

private fun parseRequiredDay(
    value: String,
    fieldName: String,
    errors: MutableMap<String, String>,
    key: String,
): Int? {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) {
        errors[key] = "${fieldName.replaceFirstChar { it.titlecase() }} is required"
        return null
    }
    val parsed = trimmed.toIntOrNull()
    if (parsed == null || parsed !in 1..31) errors[key] = "Enter a valid $fieldName from 1 to 31"
    return parsed
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
