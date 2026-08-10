package com.petergabriel.budgetcalendar.features.sandbox.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import com.petergabriel.budgetcalendar.core.designsystem.component.AccountPickerModal
import com.petergabriel.budgetcalendar.core.designsystem.component.BcButton
import com.petergabriel.budgetcalendar.core.designsystem.component.BcSegmentedControl
import com.petergabriel.budgetcalendar.core.designsystem.component.BcSheetShell
import com.petergabriel.budgetcalendar.core.designsystem.component.ButtonVariant
import com.petergabriel.budgetcalendar.core.designsystem.component.form.BcInputGroup
import com.petergabriel.budgetcalendar.core.designsystem.component.form.LargeAmountInput
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.core.utils.MoneyInputUtils
import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.AddSandboxTransactionRequest
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSandboxTransactionSheet(
    isVisible: Boolean,
    snapshotId: Long?,
    accounts: List<Account>,
    onAdd: (AddSandboxTransactionRequest) -> Unit,
    onDismiss: () -> Unit,
    isSubmitting: Boolean = false,
    errorMessage: String? = null,
) {
    if (!isVisible) {
        return
    }

    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing

    var selectedType by rememberSaveable { mutableStateOf(TransactionType.EXPENSE) }
    var amountInput by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var selectedAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var destinationAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedDateMillis by rememberSaveable { mutableStateOf(DateUtils.startOfDayMillis(DateUtils.nowMillis())) }
    var userManuallyChangedDate by remember { mutableStateOf(false) }
    var currentDayMillis by remember { mutableLongStateOf(DateUtils.startOfDayMillis(DateUtils.nowMillis())) }
    var showAccountPicker by remember { mutableStateOf(false) }
    var showDestinationPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    val localErrors = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(Unit) {
        while (true) {
            val delay = DateUtils.millisUntilNextDay()
            kotlinx.coroutines.delay(delay)
            currentDayMillis = DateUtils.startOfDayMillis(DateUtils.nowMillis())
        }
    }

    LaunchedEffect(currentDayMillis) {
        if (!userManuallyChangedDate) {
            selectedDateMillis = currentDayMillis
        }
    }

    LaunchedEffect(accounts) {
        if (selectedAccountId == null || accounts.none { account -> account.id == selectedAccountId }) {
            selectedAccountId = accounts.firstOrNull()?.id
        }
    }

    val selectedAccount = accounts.firstOrNull { account -> account.id == selectedAccountId }
    val destinationAccounts = accounts.filter { account -> account.id != selectedAccountId }
    val destinationAccount = destinationAccounts.firstOrNull { account -> account.id == destinationAccountId }
    val parsedAmount = MoneyInputUtils.parseToCents(amountInput)
    val isDirty = amountInput.isNotBlank() ||
        category.isNotBlank() ||
        selectedType != TransactionType.EXPENSE ||
        selectedDateMillis != currentDayMillis ||
        destinationAccountId != null
    val ctaLabel = when (selectedType) {
        TransactionType.INCOME -> "Add Income"
        TransactionType.TRANSFER -> "Add Transfer"
        else -> "Add Expense"
    }

    fun closeSheet() {
        if (isSubmitting) return
        if (isDirty) {
            showDiscardDialog = true
        } else {
            onDismiss()
        }
    }

    BcSheetShell(
        title = "New Sandbox Transaction",
        onDismissRequest = ::closeSheet,
        bodyScrollable = false,
        body = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(spacing.spacing4),
            ) {
                BcSegmentedControl(
                    options = listOf("Expense", "Income", "Transfer"),
                    selectedIndex = when (selectedType) {
                        TransactionType.INCOME -> 1
                        TransactionType.TRANSFER -> 2
                        else -> 0
                    },
                    onSelectedIndexChange = { index ->
                        selectedType = when (index) {
                            1 -> TransactionType.INCOME
                            2 -> TransactionType.TRANSFER
                            else -> TransactionType.EXPENSE
                        }
                        if (selectedType != TransactionType.TRANSFER) {
                            destinationAccountId = null
                        }
                        localErrors.remove("destinationAccountId")
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                LargeAmountInput(
                    label = "AMOUNT",
                    amount = amountInput,
                    onAmountChange = { value ->
                        amountInput = value
                        localErrors.remove("amount")
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                InlineError(localErrors["amount"])

                BcInputGroup(
                    label = "Name",
                    value = category,
                    onValueChange = { value ->
                        category = value.take(80)
                        localErrors.remove("category")
                    },
                    placeholder = "e.g. Groceries",
                    modifier = Modifier.fillMaxWidth(),
                    isError = localErrors.containsKey("category"),
                    errorText = localErrors["category"],
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.spacing3),
                    verticalAlignment = Alignment.Top,
                ) {
                    BcInputGroup(
                        label = "Date",
                        value = formatDate(selectedDateMillis),
                        onValueChange = {},
                        placeholder = "Today",
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = !isSubmitting) { showDatePicker = true },
                        readOnly = true,
                    )
                    BcInputGroup(
                        label = "Account",
                        value = selectedAccount?.name.orEmpty(),
                        onValueChange = {},
                        placeholder = if (accounts.isEmpty()) "No accounts" else "Select account",
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = accounts.isNotEmpty() && !isSubmitting) { showAccountPicker = true },
                        readOnly = true,
                        isError = localErrors.containsKey("accountId"),
                        errorText = localErrors["accountId"],
                    )
                }

                if (selectedType == TransactionType.TRANSFER) {
                    Spacer(modifier = Modifier.height(spacing.spacing2))
                    BcInputGroup(
                        label = "Destination Account",
                        value = destinationAccount?.name.orEmpty(),
                        onValueChange = {},
                        placeholder = if (destinationAccounts.isEmpty()) "No other accounts" else "Select destination",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = destinationAccounts.isNotEmpty() && !isSubmitting) { showDestinationPicker = true },
                        readOnly = true,
                        isError = localErrors.containsKey("destinationAccountId"),
                        errorText = localErrors["destinationAccountId"],
                    )
                }

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        style = typography.bodySmall,
                        color = colors.colorErrorText,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
        },
        footer = {
            BcButton(
                text = if (isSubmitting) "Adding..." else ctaLabel,
                onClick = {
                    val errors = mutableMapOf<String, String>()
                    if (snapshotId == null) errors["snapshotId"] = "Sandbox snapshot is required"
                    if (selectedAccountId == null) errors["accountId"] = "Account is required"
                    if (parsedAmount == null || parsedAmount <= 0L) errors["amount"] = "Enter a valid amount"
                    if (selectedType == TransactionType.TRANSFER && destinationAccountId == null) {
                        errors["destinationAccountId"] = "Destination account is required"
                    }
                    if (selectedType == TransactionType.TRANSFER && destinationAccountId == selectedAccountId) {
                        errors["destinationAccountId"] = "Cannot transfer to the same account"
                    }
                    if (selectedType != TransactionType.TRANSFER && category.isBlank()) {
                        errors["category"] = "Name is required"
                    }

                    localErrors.clear()
                    localErrors.putAll(errors)
                    if (errors.isNotEmpty() || isSubmitting) return@BcButton

                    val resolvedSnapshotId = snapshotId ?: return@BcButton
                    val resolvedAccountId = selectedAccountId ?: return@BcButton
                    val resolvedAmount = parsedAmount ?: return@BcButton
                    onAdd(
                        AddSandboxTransactionRequest(
                            snapshotId = resolvedSnapshotId,
                            accountId = resolvedAccountId,
                            destinationAccountId = if (selectedType == TransactionType.TRANSFER) destinationAccountId else null,
                            amount = resolvedAmount,
                            date = selectedDateMillis,
                            type = selectedType,
                            category = category.trim(),
                            description = null,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.Primary,
                enabled = !isSubmitting,
            )
        },
    )

    if (showAccountPicker) {
        AccountPickerModal(
            isVisible = true,
            accounts = accounts,
            selectedAccountId = selectedAccountId,
            title = "Select Account",
            onAccountSelected = { id ->
                selectedAccountId = id
                if (destinationAccountId == id) destinationAccountId = null
                localErrors.remove("accountId")
                showAccountPicker = false
            },
            onDismiss = { showAccountPicker = false },
        )
    }

    if (showDestinationPicker) {
        AccountPickerModal(
            isVisible = true,
            accounts = destinationAccounts,
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

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.toUtcDayStartMillis(selectedDateMillis),
        )
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
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) { DatePicker(state = datePickerState) }
    }

    if (showDiscardDialog) {
        com.petergabriel.budgetcalendar.core.designsystem.component.BcConfirmDialog(
            title = "Discard transaction?",
            message = "Your changes will be lost.",
            onDismissRequest = { showDiscardDialog = false },
            onConfirm = {
                showDiscardDialog = false
                onDismiss()
            },
            confirmLabel = "Discard",
            dismissLabel = "Keep editing",
            confirmVariant = ButtonVariant.Destructive,
        )
    }
}

@Composable
private fun InlineError(message: String?) {
    if (message.isNullOrBlank()) return
    Text(
        text = message,
        color = BudgetCalendarTheme.colors.colorErrorText,
        style = BudgetCalendarTheme.typography.bodySmall,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

private fun formatDate(millis: Long): String {
    return Instant
        .fromEpochMilliseconds(millis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
        .toString()
}
