package com.petergabriel.budgetcalendar.features.creditcard.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils
import com.petergabriel.budgetcalendar.core.utils.MoneyInputUtils
import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account
import com.petergabriel.budgetcalendar.features.creditcard.domain.model.CreditCardSummary

@Composable
fun CreditCardPaymentSheet(
    isVisible: Boolean,
    ccSummary: CreditCardSummary,
    suggestedAmount: Long,
    spendingPoolAccounts: List<Account>,
    isSubmitting: Boolean = false,
    errorMessage: String? = null,
    onConfirmPayment: (sourceAccountId: Long, amount: Long) -> Unit,
    onCancel: () -> Unit,
) {
    if (!isVisible) return

    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing
    var amountInput by remember(ccSummary.accountId, suggestedAmount) {
        mutableStateOf(MoneyInputUtils.centsToInput(suggestedAmount))
    }
    var selectedSourceAccountId by remember(ccSummary.accountId) {
        mutableStateOf(spendingPoolAccounts.firstOrNull()?.id)
    }
    var accountMenuExpanded by remember { mutableStateOf(false) }
    val validationErrors = remember(ccSummary.accountId) { mutableStateMapOf<String, String>() }
    var showDiscardConfirmation by remember(ccSummary.accountId) { mutableStateOf(false) }

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
    val isDirty = amountInput != MoneyInputUtils.centsToInput(suggestedAmount) ||
        selectedSourceAccountId != spendingPoolAccounts.firstOrNull()?.id

    fun requestDismiss() {
        if (isSubmitting) return
        if (isDirty) showDiscardConfirmation = true else onCancel()
    }

    BcSheetShell(
        title = "Pay ${ccSummary.accountName}",
        onDismissRequest = ::requestDismiss,
        body = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.spacing3),
            ) {
                Text(
                    text = "Suggested payment: ${CurrencyUtils.formatCents(suggestedAmount)}",
                    style = typography.bodyMedium,
                    color = colors.textSecondary,
                )
                errorMessage?.let { error ->
                    Text(text = error, style = typography.bodySmall, color = colors.colorErrorText)
                }

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
                        .clickable(enabled = !isSubmitting) { accountMenuExpanded = true },
                    trailingIcon = { Text("⌄", color = colors.textSecondary) },
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
                        style = typography.bodySmall,
                        color = colors.colorWarningText,
                    )
                } else if (exceedsReserved) {
                    Text(
                        text = "Warning: payment exceeds reserved amount (${CurrencyUtils.formatCents(ccSummary.reservedAmount)}).",
                        style = typography.bodySmall,
                        color = colors.colorWarningText,
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
                    text = if (isSubmitting) "Confirming…" else "Confirm",
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
                        if (errors.isNotEmpty()) return@BcButton
                        onConfirmPayment(selectedSourceAccountId ?: return@BcButton, amount ?: return@BcButton)
                    },
                    modifier = Modifier.weight(1f),
                    variant = ButtonVariant.Primary,
                    enabled = spendingPoolAccounts.isNotEmpty() && !isSubmitting,
                )
            }
        },
    )

    if (showDiscardConfirmation) {
        BcConfirmDialog(
            title = "Discard payment?",
            message = "Your payment details will be lost.",
            onDismissRequest = { showDiscardConfirmation = false },
            onConfirm = onCancel,
            confirmLabel = "Discard",
            dismissLabel = "Keep editing",
        )
    }
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
