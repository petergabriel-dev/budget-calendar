package com.petergabriel.budgetcalendar.features.transactions.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.petergabriel.budgetcalendar.core.designsystem.component.AccountPickerModal
import com.petergabriel.budgetcalendar.core.designsystem.component.BcButton
import com.petergabriel.budgetcalendar.core.designsystem.component.BcConfirmDialog
import com.petergabriel.budgetcalendar.core.designsystem.component.BcSheetShell
import com.petergabriel.budgetcalendar.core.designsystem.component.BcSegmentedControl
import com.petergabriel.budgetcalendar.core.designsystem.component.ButtonVariant
import com.petergabriel.budgetcalendar.core.designsystem.component.form.BcCheckboxRow
import com.petergabriel.budgetcalendar.core.designsystem.component.form.BcInputGroup
import com.petergabriel.budgetcalendar.core.designsystem.component.form.LargeAmountInput
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils
import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.core.utils.MoneyInputUtils
import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType
import com.petergabriel.budgetcalendar.features.creditcard.domain.model.CreditCardSummary
import com.petergabriel.budgetcalendar.features.creditcard.domain.model.calculateCreditCardChargeImpact
import com.petergabriel.budgetcalendar.features.transactions.domain.model.CreateTransactionRequest
import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import com.petergabriel.budgetcalendar.features.transactions.presentation.TransactionFormUiState
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormSheet(
    isVisible: Boolean,
    uiState: TransactionFormUiState,
    initialDateMillis: Long,
    initialData: Transaction? = null,
    onSetType: (TransactionType) -> Unit,
    onSave: (CreateTransactionRequest) -> Unit,
    onCancel: () -> Unit,
    onDelete: ((Transaction) -> Unit)? = null,
    titleOverride: String? = null,
    disableDismissRequest: Boolean = false,
    confirmDiscardOnDirty: Boolean = false,
) {
    if (!isVisible) {
        return
    }

    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing
    val isEditMode = initialData != null
    val stateKey = initialData?.id ?: "new"
    val initialType = when (initialData?.type ?: uiState.selectedType) {
        TransactionType.INCOME -> TransactionType.INCOME
        TransactionType.TRANSFER -> TransactionType.TRANSFER
        else -> TransactionType.EXPENSE
    }
    var amountInput by remember(stateKey) {
        mutableStateOf(initialData?.amount?.let(MoneyInputUtils::centsToInput).orEmpty())
    }
    var selectedDateMillis by remember(stateKey) {
        mutableStateOf(DateUtils.startOfDayMillis(initialData?.date ?: initialDateMillis))
    }
    var userManuallyChangedDate by remember(stateKey) { mutableStateOf(false) }
    var selectedAccountId by remember(stateKey) { mutableStateOf(initialData?.accountId) }
    var categoryInput by remember(stateKey) { mutableStateOf(initialData?.category.orEmpty()) }
    var statusInput by remember(stateKey) { mutableStateOf(initialData?.status ?: TransactionStatus.PENDING) }
    var userManuallyChangedStatus by remember(stateKey) { mutableStateOf(false) }

    val localErrors = remember(stateKey) { mutableStateMapOf<String, String>() }
    var showAccountPicker by remember(stateKey) { mutableStateOf(false) }
    var showDatePicker by remember(stateKey) { mutableStateOf(false) }
    var destinationAccountId by remember(stateKey) { mutableStateOf(initialData?.destinationAccountId) }
    var showDestinationPicker by remember(stateKey) { mutableStateOf(false) }
    var showDiscardDialog by remember(stateKey) { mutableStateOf(false) }
    var showDeleteConfirmation by remember(stateKey) { mutableStateOf(false) }

    LaunchedEffect(stateKey) {
        onSetType(initialType)
        localErrors.clear()
        userManuallyChangedDate = false
        userManuallyChangedStatus = false
    }

    LaunchedEffect(initialDateMillis) {
        if (!isEditMode && !userManuallyChangedDate) {
            selectedDateMillis = DateUtils.startOfDayMillis(initialDateMillis)
        }
    }

    LaunchedEffect(uiState.availableAccounts, selectedAccountId) {
        if (selectedAccountId == null && uiState.availableAccounts.isNotEmpty()) {
            selectedAccountId = uiState.availableAccounts.first().id
        }
    }

    val selectedType = when (uiState.selectedType) {
        TransactionType.INCOME -> TransactionType.INCOME
        TransactionType.TRANSFER -> TransactionType.TRANSFER
        else -> TransactionType.EXPENSE
    }
    val selectedAccount = uiState.availableAccounts.firstOrNull { account -> account.id == selectedAccountId }
    val isSelectedAccountCreditCard = selectedAccount?.type == AccountType.CREDIT_CARD
    val effectiveSelectedType = transactionFormEffectiveType(selectedType, selectedAccount?.type)

    LaunchedEffect(isSelectedAccountCreditCard, selectedDateMillis) {
        if (isSelectedAccountCreditCard) {
            if (selectedType != TransactionType.EXPENSE) {
                destinationAccountId = null
                localErrors.remove("destinationAccountId")
                onSetType(TransactionType.EXPENSE)
            }
            if (!isEditMode && !userManuallyChangedStatus) {
                statusInput = defaultCardChargeStatus(selectedDateMillis, DateUtils.nowMillis())
            }
        }
    }
    val initialAccountId = initialData?.accountId ?: uiState.availableAccounts.firstOrNull()?.id
    val initialDestinationAccountId = initialData?.destinationAccountId
    val initialAmountInput = initialData?.amount?.let(MoneyInputUtils::centsToInput).orEmpty()
    val initialDateInput = DateUtils.startOfDayMillis(initialData?.date ?: initialDateMillis)
    val initialCategoryInput = initialData?.category.orEmpty()
    val initialStatusInput = initialData?.status ?: TransactionStatus.PENDING
    val isDirty = amountInput != initialAmountInput ||
        selectedDateMillis != initialDateInput ||
        selectedAccountId != initialAccountId ||
        categoryInput != initialCategoryInput ||
        statusInput != initialStatusInput ||
        effectiveSelectedType != initialType ||
        destinationAccountId != initialDestinationAccountId
    fun handleClose() {
        if (confirmDiscardOnDirty && isDirty) {
            showDiscardDialog = true
        } else {
            onCancel()
        }
    }
    val parsedAmount = MoneyInputUtils.parseToCents(amountInput)
    val selectedCreditCardSummary = uiState.creditCardSummaries.firstOrNull { summary -> summary.accountId == selectedAccountId }
    val isCreditCardExpense = effectiveSelectedType == TransactionType.EXPENSE && selectedAccount?.type == AccountType.CREDIT_CARD
    val statementContextLine = cardStatementContextLine(selectedCreditCardSummary, selectedDateMillis)
    val cardChargeImpact = if (isCreditCardExpense && parsedAmount != null && parsedAmount > 0L && selectedCreditCardSummary != null) {
        calculateCreditCardChargeImpact(
            currentBalance = selectedCreditCardSummary.currentBalance,
            reservedAmount = selectedCreditCardSummary.reservedAmount,
            availableCredit = selectedCreditCardSummary.availableCredit,
            amount = parsedAmount,
        )
    } else {
        null
    }
    val exceedsCredit = cardChargeImpact?.exceedsCredit == true
    val overLimitMessage = "This charge exceeds available credit. Raise the limit in card settings before saving."
    val showInsufficientFunds = (effectiveSelectedType == TransactionType.EXPENSE || effectiveSelectedType == TransactionType.TRANSFER) &&
        selectedAccount?.type != AccountType.CREDIT_CARD &&
        parsedAmount != null &&
        selectedAccount != null &&
        parsedAmount > selectedAccount.balance
    val ctaLabel = if (initialData == null) "Add ${if (isSelectedAccountCreditCard) "Charge" else effectiveSelectedType.displayName}" else "Save"

    BcSheetShell(
        title = titleOverride ?: if (initialData == null) "New Transaction" else "Edit Transaction",
        onDismissRequest = {
            if (!disableDismissRequest && !uiState.isSubmitting) {
                handleClose()
            }
        },
        closeContentDescription = "Close transaction form",
        body = {
            if (!uiState.error.isNullOrBlank()) {
                InlineError(uiState.error)
            }

            if (!isSelectedAccountCreditCard) {
                    BcSegmentedControl(
                        options = listOf("Expense", "Income", "Transfer"),
                        selectedIndex = when (selectedType) {
                            TransactionType.INCOME -> 1
                            TransactionType.TRANSFER -> 2
                            else -> 0
                        },
                        onSelectedIndexChange = { selectedIndex ->
                            val newType = when (selectedIndex) {
                                1 -> TransactionType.INCOME
                                2 -> TransactionType.TRANSFER
                                else -> TransactionType.EXPENSE
                            }
                            if (newType != TransactionType.TRANSFER) {
                                destinationAccountId = null
                            }
                            localErrors.remove("destinationAccountId")
                            onSetType(newType)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                LargeAmountInput(
                    label = "AMOUNT",
                    amount = amountInput,
                    onAmountChange = { value ->
                        amountInput = value
                        localErrors.remove("amount")
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                InlineError(resolveFieldError("amount", localErrors, uiState))

                BcInputGroup(
                    label = "Name",
                    value = categoryInput,
                    onValueChange = { value ->
                        categoryInput = value
                        localErrors.remove("category")
                    },
                    placeholder = "e.g. Groceries",
                    modifier = Modifier.fillMaxWidth(),
                    isError = hasFieldError("category", localErrors, uiState),
                    errorText = resolveFieldError("category", localErrors, uiState),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.spacing4),
                    verticalAlignment = Alignment.Top,
                ) {
                    BcInputGroup(
                        label = "Date",
                        value = formatDate(selectedDateMillis),
                        onValueChange = {},
                        placeholder = "Today",
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = !uiState.isSubmitting) { showDatePicker = true },
                        readOnly = true,
                        isError = hasFieldError("date", localErrors, uiState),
                        errorText = resolveFieldError("date", localErrors, uiState),
                    )

                    BcInputGroup(
                        label = "Account",
                        value = selectedAccount?.name.orEmpty(),
                        onValueChange = {},
                        placeholder = if (uiState.availableAccounts.isEmpty()) "No accounts" else "Select account",
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                enabled = !uiState.isSubmitting && uiState.availableAccounts.isNotEmpty(),
                            ) {
                                showAccountPicker = true
                            },
                        readOnly = true,
                        isError = hasFieldError("accountId", localErrors, uiState),
                        errorText = resolveFieldError("accountId", localErrors, uiState),
                    )
                }

                if (showAccountPicker) {
                    AccountPickerModal(
                        isVisible = showAccountPicker,
                        accounts = uiState.availableAccounts,
                        selectedAccountId = selectedAccountId,
                        title = "Select Account",
                        onAccountSelected = { id ->
                            selectedAccountId = id
                            if (destinationAccountId == id) {
                                destinationAccountId = null
                            }
                            localErrors.remove("accountId")
                            showAccountPicker = false
                        },
                        onDismiss = { showAccountPicker = false },
                    )
                }

                if (effectiveSelectedType == TransactionType.TRANSFER) {
                    val filteredAccounts = uiState.availableAccounts.filter { it.id != selectedAccountId }
                    val destinationAccount = filteredAccounts.find { it.id == destinationAccountId }

                    Spacer(modifier = Modifier.height(spacing.spacing2))

                    BcInputGroup(
                        label = "Destination Account",
                        value = destinationAccount?.name.orEmpty(),
                        onValueChange = {},
                        placeholder = if (filteredAccounts.isEmpty()) "No other accounts" else "Select destination",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                enabled = !uiState.isSubmitting && filteredAccounts.isNotEmpty(),
                            ) {
                                showDestinationPicker = true
                            },
                        readOnly = true,
                        isError = hasFieldError("destinationAccountId", localErrors, uiState),
                        errorText = resolveFieldError("destinationAccountId", localErrors, uiState),
                    )

                    if (showDestinationPicker) {
                        AccountPickerModal(
                            isVisible = showDestinationPicker,
                            accounts = filteredAccounts,
                            selectedAccountId = destinationAccountId,
                            title = "Select Destination",
                            onAccountSelected = { id ->
                                destinationAccountId = id
                                localErrors.remove("destinationAccountId")
                                showDestinationPicker = false
                            },
                            onDismiss = { showDestinationPicker = false },
                        )
                    }
                }

                if (showDatePicker) {
                    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = DateUtils.toUtcDayStartMillis(selectedDateMillis))
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    selectedDateMillis = DateUtils.toLocalDayStartMillis(
                                        datePickerState.selectedDateMillis ?: DateUtils.toUtcDayStartMillis(selectedDateMillis),
                                    )
                                    userManuallyChangedDate = true
                                    showDatePicker = false
                                },
                            ) {
                                Text("OK")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) {
                                Text("Cancel")
                            }
                        },
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(spacing.spacing4),
                ) {
                    Text(
                        text = "STATUS",
                        style = typography.caption,
                        color = colors.textSecondary,
                    )

                    BcCheckboxRow(
                        label = "Scheduled (Future)",
                        checked = statusInput == TransactionStatus.PENDING,
                        onCheckedChange = {
                            statusInput = TransactionStatus.PENDING
                            userManuallyChangedStatus = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    BcCheckboxRow(
                        label = if (isSelectedAccountCreditCard) "Posted" else "Paid / Cleared",
                        checked = statusInput == TransactionStatus.CONFIRMED,
                        onCheckedChange = {
                            statusInput = TransactionStatus.CONFIRMED
                            userManuallyChangedStatus = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (showInsufficientFunds) {
                    Text(
                        text = "Insufficient funds warning: this expense exceeds the selected account balance.",
                        color = colors.colorWarningText,
                        style = typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (isCreditCardExpense && statementContextLine != null) {
                    Text(
                        text = statementContextLine,
                        color = colors.textSecondary,
                        style = typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (isCreditCardExpense && selectedCreditCardSummary != null && cardChargeImpact != null) {
                    selectedCreditCardSummary.availableCredit?.let { availableCredit ->
                        Text(
                            text = "Available credit: ${CurrencyUtils.formatCents(availableCredit)}",
                            color = colors.textSecondary,
                            style = typography.bodySmall,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Text(
                        text = "Balance after charge: ${CurrencyUtils.formatCents(cardChargeImpact.owedAfter)}",
                        color = colors.textSecondary,
                        style = typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Text(
                        text = "Reserves ${CurrencyUtils.formatCents(cardChargeImpact.reserveDelta)} from Safe to Spend.",
                        color = colors.colorWarningText,
                        style = typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (exceedsCredit) {
                        Text(
                            text = overLimitMessage,
                            color = colors.colorErrorText,
                            style = typography.bodySmall,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

        },
        footer = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
            ) {
                BcButton(
                    text = ctaLabel,
                    onClick = {
                        val amount = MoneyInputUtils.parseToCents(amountInput.trim())
                        val accountId = selectedAccountId
                        val errors = mutableMapOf<String, String>()

                        if (amount == null || amount <= 0L) {
                            errors["amount"] = "Enter a valid amount"
                        }

                        if (accountId == null) {
                            errors["accountId"] = "Account is required"
                        }

                        if (effectiveSelectedType == TransactionType.TRANSFER && destinationAccountId == null) {
                            errors["destinationAccountId"] = "Destination account is required"
                        }
                        if (effectiveSelectedType == TransactionType.TRANSFER && destinationAccountId == selectedAccountId) {
                            errors["destinationAccountId"] = "Cannot transfer to the same account"
                        }

                        if (effectiveSelectedType != TransactionType.TRANSFER && categoryInput.isBlank()) {
                            errors["category"] = "Category is required"
                        }

                        if (exceedsCredit) {
                            errors["creditLimit"] = overLimitMessage
                        }

                        localErrors.clear()
                        localErrors.putAll(errors)
                        if (errors.isNotEmpty()) {
                            return@BcButton
                        }

                        onSave(
                            CreateTransactionRequest(
                                accountId = checkNotNull(accountId),
                                destinationAccountId = if (effectiveSelectedType == TransactionType.TRANSFER) destinationAccountId else null,
                                amount = checkNotNull(amount),
                                date = selectedDateMillis,
                                type = effectiveSelectedType,
                                status = statusInput,
                                description = null,
                                category = categoryInput.trim().ifBlank { null },
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    variant = ButtonVariant.Primary,
                    enabled = !uiState.isSubmitting && !exceedsCredit,
                )

                if (initialData != null && onDelete != null) {
                    BcButton(
                        text = "Delete",
                        onClick = { showDeleteConfirmation = true },
                        modifier = Modifier.fillMaxWidth(),
                        variant = ButtonVariant.Destructive,
                        enabled = !uiState.isSubmitting,
                    )
                }
            }
        },
    )

    if (showDiscardDialog) {
        BcConfirmDialog(
            title = "Discard transaction?",
            message = "Your changes will be lost.",
            onDismissRequest = { showDiscardDialog = false },
            onConfirm = {
                showDiscardDialog = false
                onCancel()
            },
            confirmLabel = "Discard",
            dismissLabel = "Keep editing",
        )
    }

    if (showDeleteConfirmation && initialData != null && onDelete != null) {
        BcConfirmDialog(
            title = "Delete transaction?",
            message = "This action cannot be undone.",
            onDismissRequest = { showDeleteConfirmation = false },
            onConfirm = {
                showDeleteConfirmation = false
                onDelete(initialData)
            },
            confirmLabel = "Delete",
        )
    }
}

private fun hasFieldError(
    key: String,
    localErrors: Map<String, String>,
    uiState: TransactionFormUiState,
): Boolean {
    return localErrors.containsKey(key) || uiState.validationErrors.containsKey(key)
}

private fun resolveFieldError(
    key: String,
    localErrors: Map<String, String>,
    uiState: TransactionFormUiState,
): String? {
    return localErrors[key] ?: uiState.validationErrors[key]
}

@Composable
private fun InlineError(message: String?) {
    if (message.isNullOrBlank()) {
        return
    }

    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing

    Text(
        text = message,
        color = colors.colorErrorText,
        style = typography.bodySmall,
        modifier = Modifier.padding(start = spacing.spacing1),
    )
}

private val TransactionType.displayName: String
    get() = when (this) {
        TransactionType.INCOME -> "Income"
        TransactionType.EXPENSE -> "Expense"
        TransactionType.TRANSFER -> "Transfer"
    }

private fun formatDate(dateMillis: Long): String {
    return Instant
        .fromEpochMilliseconds(dateMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
        .toString()
}

internal fun transactionFormEffectiveType(selectedType: TransactionType, accountType: AccountType?): TransactionType {
    return if (accountType == AccountType.CREDIT_CARD) TransactionType.EXPENSE else selectedType
}

internal fun defaultCardChargeStatus(
    chargeDateMillis: Long,
    nowMillis: Long,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): TransactionStatus {
    return if (DateUtils.startOfDayMillis(chargeDateMillis, timeZone) <= DateUtils.startOfDayMillis(nowMillis, timeZone)) {
        TransactionStatus.CONFIRMED
    } else {
        TransactionStatus.PENDING
    }
}

internal fun cardStatementContextLine(
    summary: CreditCardSummary?,
    chargeDateMillis: Long,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String? {
    val closingDay = summary?.statementClosingDay ?: return null
    val dueDay = summary.paymentDueDay ?: return null
    val closingMillis = DateUtils.statementClosingOnOrAfter(closingDay, chargeDateMillis, timeZone)
    val dueMillis = DateUtils.paymentDueAfter(dueDay, closingMillis, timeZone)
    return "Bills to statement closing ${DateUtils.formatMonthDay(closingMillis, timeZone)} · due ${DateUtils.formatMonthDay(dueMillis, timeZone)}"
}
