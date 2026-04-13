package com.petergabriel.budgetcalendar.features.sandbox.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import com.petergabriel.budgetcalendar.core.designsystem.component.BcButton
import com.petergabriel.budgetcalendar.core.designsystem.component.BcSegmentedControl
import com.petergabriel.budgetcalendar.core.designsystem.component.ButtonVariant
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
) {
    if (!isVisible) {
        return
    }

    val colors = BudgetCalendarTheme.colors
    val spacing = BudgetCalendarTheme.spacing
    val typography = BudgetCalendarTheme.typography

    var selectedType by rememberSaveable { mutableStateOf(TransactionType.EXPENSE) }
    var amountInput by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var selectedAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedDateMillis by rememberSaveable {
        mutableStateOf(DateUtils.startOfDayMillis(DateUtils.nowMillis()))
    }
    var accountMenuExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(accounts) {
        if (selectedAccountId == null || accounts.none { account -> account.id == selectedAccountId }) {
            selectedAccountId = accounts.firstOrNull()?.id
        }
    }

    val parsedAmount = MoneyInputUtils.parseToCents(amountInput)?.takeIf { cents -> cents > 0L }
    val canAdd = snapshotId != null &&
        selectedAccountId != null &&
        parsedAmount != null &&
        category.trim().isNotBlank()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.spacing4, vertical = spacing.spacing3),
            verticalArrangement = Arrangement.spacedBy(spacing.spacing4),
        ) {
            Text(
                text = "Add Sandbox Transaction",
                style = typography.section,
                color = colors.textPrimary,
            )

            BcSegmentedControl(
                options = listOf("Income", "Expense"),
                selectedIndex = if (selectedType == TransactionType.INCOME) 0 else 1,
                onSelectedIndexChange = { index ->
                    selectedType = if (index == 0) TransactionType.INCOME else TransactionType.EXPENSE
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.spacing1),
            ) {
                Text(
                    text = "AMOUNT",
                    style = typography.bodyMedium.copy(letterSpacing = typography.caption.letterSpacing),
                    color = colors.textSecondary,
                )
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { raw ->
                        if (raw.isBlank()) {
                            amountInput = ""
                            return@OutlinedTextField
                        }
                        if (MoneyInputUtils.parseToCents(raw) != null) {
                            amountInput = raw
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    prefix = { Text("₱") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.spacing1),
            ) {
                Text(
                    text = "Name",
                    style = typography.bodySmall,
                    color = colors.textSecondary,
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { value -> category = value.take(80) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("e.g. Groceries") },
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.spacing1),
            ) {
                Text(
                    text = "Account",
                    style = typography.bodySmall,
                    color = colors.textSecondary,
                )
                ExposedDropdownMenuBox(
                    expanded = accountMenuExpanded,
                    onExpandedChange = { expanded -> accountMenuExpanded = expanded },
                ) {
                    val selectedAccountName = accounts.firstOrNull { account -> account.id == selectedAccountId }?.name
                        ?: "No account available"
                    OutlinedTextField(
                        value = selectedAccountName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountMenuExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(
                                type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                enabled = true,
                            ),
                    )
                    DropdownMenu(
                        expanded = accountMenuExpanded,
                        onDismissRequest = { accountMenuExpanded = false },
                    ) {
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.name) },
                                onClick = {
                                    selectedAccountId = account.id
                                    accountMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Date",
                    style = typography.bodyMedium,
                    color = colors.textSecondary,
                )
                Text(
                    text = formatDate(selectedDateMillis),
                    style = typography.bodyMedium,
                    color = colors.textPrimary,
                )
            }

            BcButton(
                text = "Add",
                onClick = {
                    val resolvedSnapshotId = snapshotId
                    val resolvedAccountId = selectedAccountId
                    val resolvedAmount = parsedAmount
                    if (
                        resolvedSnapshotId == null ||
                        resolvedAccountId == null ||
                        resolvedAmount == null
                    ) {
                        return@BcButton
                    }
                    onAdd(
                        AddSandboxTransactionRequest(
                            snapshotId = resolvedSnapshotId,
                            accountId = resolvedAccountId,
                            amount = resolvedAmount,
                            date = selectedDateMillis,
                            type = selectedType,
                            category = category.trim(),
                            description = null,
                        ),
                    )
                },
                enabled = canAdd,
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.Primary,
            )
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
}

private fun formatDate(millis: Long): String {
    return Instant
        .fromEpochMilliseconds(millis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
        .toString()
}
