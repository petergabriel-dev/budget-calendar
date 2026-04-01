package com.petergabriel.budgetcalendar.features.transactions.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petergabriel.budgetcalendar.core.designsystem.component.BcButton
import com.petergabriel.budgetcalendar.core.designsystem.component.BcSegmentedControl
import com.petergabriel.budgetcalendar.core.designsystem.component.ButtonVariant
import com.petergabriel.budgetcalendar.core.designsystem.component.form.BcCheckboxRow
import com.petergabriel.budgetcalendar.core.designsystem.component.form.BcInputGroup
import com.petergabriel.budgetcalendar.core.designsystem.component.form.LargeAmountInput
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.core.utils.MoneyInputUtils
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
) {
    if (!isVisible) {
        return
    }

    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val stateKey = initialData?.id ?: initialDateMillis
    val initialType = when (initialData?.type ?: uiState.selectedType) {
        TransactionType.INCOME -> TransactionType.INCOME
        else -> TransactionType.EXPENSE
    }
    var amountInput by remember(stateKey) {
        mutableStateOf(initialData?.amount?.let(MoneyInputUtils::centsToInput).orEmpty())
    }
    var selectedDateMillis by remember(stateKey) {
        mutableStateOf(DateUtils.startOfDayMillis(initialData?.date ?: initialDateMillis))
    }
    var selectedAccountId by remember(stateKey) { mutableStateOf(initialData?.accountId) }
    var categoryInput by remember(stateKey) { mutableStateOf(initialData?.category.orEmpty()) }
    var statusInput by remember(stateKey) { mutableStateOf(initialData?.status ?: TransactionStatus.PENDING) }

    val localErrors = remember(stateKey) { mutableStateMapOf<String, String>() }
    var accountMenuExpanded by remember(stateKey) { mutableStateOf(false) }
    var showDatePicker by remember(stateKey) { mutableStateOf(false) }

    LaunchedEffect(stateKey) {
        onSetType(initialType)
        localErrors.clear()
    }

    LaunchedEffect(uiState.availableAccounts, selectedAccountId) {
        if (selectedAccountId == null && uiState.availableAccounts.isNotEmpty()) {
            selectedAccountId = uiState.availableAccounts.first().id
        }
    }

    val selectedType = if (uiState.selectedType == TransactionType.INCOME) {
        TransactionType.INCOME
    } else {
        TransactionType.EXPENSE
    }
    val selectedAccount = uiState.availableAccounts.firstOrNull { account -> account.id == selectedAccountId }
    val parsedAmount = MoneyInputUtils.parseToCents(amountInput)
    val showInsufficientFunds = selectedType == TransactionType.EXPENSE &&
        parsedAmount != null &&
        selectedAccount != null &&
        parsedAmount > selectedAccount.balance
    val ctaLabel = if (initialData == null) "Add ${selectedType.displayName}" else "Save"
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = sheetState,
        dragHandle = {},
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (initialData == null) "New Transaction" else "Edit Transaction",
                        style = typography.section,
                        color = colors.textPrimary,
                    )

                    IconButton(
                        onClick = onCancel,
                        enabled = !uiState.isSubmitting,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close",
                            tint = colors.textPrimary,
                        )
                    }
                }

                if (!uiState.error.isNullOrBlank()) {
                    InlineError(uiState.error)
                }

                BcSegmentedControl(
                    options = listOf("Expense", "Income"),
                    selectedIndex = if (selectedType == TransactionType.INCOME) 1 else 0,
                    onSelectedIndexChange = { selectedIndex ->
                        onSetType(if (selectedIndex == 1) TransactionType.INCOME else TransactionType.EXPENSE)
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
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
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

                    Box(modifier = Modifier.weight(1f)) {
                        BcInputGroup(
                            label = "Account",
                            value = selectedAccount?.name.orEmpty(),
                            onValueChange = {},
                            placeholder = if (uiState.availableAccounts.isEmpty()) "No accounts" else "Select account",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    enabled = !uiState.isSubmitting && uiState.availableAccounts.isNotEmpty(),
                                ) {
                                    accountMenuExpanded = true
                                },
                            readOnly = true,
                            isError = hasFieldError("accountId", localErrors, uiState),
                            errorText = resolveFieldError("accountId", localErrors, uiState),
                        )

                        DropdownMenu(
                            expanded = accountMenuExpanded,
                            onDismissRequest = { accountMenuExpanded = false },
                        ) {
                            uiState.availableAccounts.forEach { account ->
                                DropdownMenuItem(
                                    text = { Text(account.name) },
                                    onClick = {
                                        selectedAccountId = account.id
                                        accountMenuExpanded = false
                                        localErrors.remove("accountId")
                                    },
                                )
                            }
                        }
                    }
                }

                if (showDatePicker) {
                    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    selectedDateMillis = DateUtils.startOfDayMillis(
                                        datePickerState.selectedDateMillis ?: selectedDateMillis,
                                    )
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
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "STATUS",
                        style = typography.caption.copy(letterSpacing = 1.sp),
                        color = colors.textSecondary,
                    )

                    BcCheckboxRow(
                        label = "Scheduled (Future)",
                        checked = statusInput == TransactionStatus.PENDING,
                        onCheckedChange = { statusInput = TransactionStatus.PENDING },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    BcCheckboxRow(
                        label = "Paid / Cleared",
                        checked = statusInput == TransactionStatus.CONFIRMED,
                        onCheckedChange = { statusInput = TransactionStatus.CONFIRMED },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (showInsufficientFunds) {
                    Text(
                        text = "Insufficient funds warning: this expense exceeds the selected account balance.",
                        color = colors.colorWarning,
                        style = typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
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

                        if (categoryInput.isBlank()) {
                            errors["category"] = "Category is required"
                        }

                        localErrors.clear()
                        localErrors.putAll(errors)
                        if (errors.isNotEmpty()) {
                            return@BcButton
                        }

                        onSave(
                            CreateTransactionRequest(
                                accountId = checkNotNull(accountId),
                                destinationAccountId = null,
                                amount = checkNotNull(amount),
                                date = selectedDateMillis,
                                type = selectedType,
                                status = statusInput,
                                description = null,
                                category = categoryInput.trim().ifBlank { null },
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    variant = ButtonVariant.Primary,
                    enabled = !uiState.isSubmitting,
                )

                if (initialData != null && onDelete != null) {
                    TextButton(
                        onClick = { onDelete(initialData) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isSubmitting,
                    ) {
                        Text(
                            text = "Delete",
                            color = colors.colorError,
                        )
                    }
                }
            }
        }
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

    Text(
        text = message,
        color = colors.colorError,
        style = typography.bodySmall,
        modifier = Modifier.padding(start = 4.dp),
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
