package com.petergabriel.budgetcalendar.features.creditcard.presentation.components

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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.petergabriel.budgetcalendar.core.utils.MoneyInputUtils
import com.petergabriel.budgetcalendar.features.creditcard.domain.model.CreditCardSettings
import com.petergabriel.budgetcalendar.features.creditcard.domain.model.UpdateCreditCardSettingsRequest
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardSettingsForm(
    isVisible: Boolean,
    initialSettings: CreditCardSettings?,
    onSave: (UpdateCreditCardSettingsRequest) -> Unit,
    onCancel: () -> Unit,
) {
    if (!isVisible) {
        return
    }

    var creditLimitInput by remember(initialSettings?.accountId) {
        mutableStateOf(initialSettings?.creditLimit?.let(MoneyInputUtils::centsToInput).orEmpty())
    }
    var statementBalanceInput by remember(initialSettings?.accountId) {
        mutableStateOf(initialSettings?.statementBalance?.let(MoneyInputUtils::centsToInput).orEmpty())
    }
    var dueDateMillis by remember(initialSettings?.accountId) { mutableStateOf(initialSettings?.dueDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    val validationErrors = remember(initialSettings?.accountId) { mutableStateMapOf<String, String>() }

    ModalBottomSheet(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Credit Card Settings",
                style = MaterialTheme.typography.titleLarge,
            )

            OutlinedTextField(
                value = creditLimitInput,
                onValueChange = {
                    creditLimitInput = it
                    validationErrors.remove("creditLimit")
                },
                label = { Text("Credit Limit (optional)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = validationErrors.containsKey("creditLimit"),
                singleLine = true,
            )
            InlineError(validationErrors["creditLimit"])

            OutlinedTextField(
                value = statementBalanceInput,
                onValueChange = {
                    statementBalanceInput = it
                    validationErrors.remove("statementBalance")
                },
                label = { Text("Statement Balance (optional)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = validationErrors.containsKey("statementBalance"),
                singleLine = true,
            )
            InlineError(validationErrors["statementBalance"])

            OutlinedTextField(
                value = dueDateMillis?.let(::formatDate) ?: "",
                onValueChange = {},
                label = { Text("Due Date (optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                readOnly = true,
                trailingIcon = { Text("v") },
            )

            if (dueDateMillis != null) {
                TextButton(
                    onClick = { dueDateMillis = null },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Clear due date")
                }
            }

            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDateMillis)
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                dueDateMillis = datePickerState.selectedDateMillis
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
                        val errors = mutableMapOf<String, String>()
                        val creditLimit = parseOptionalMoney(
                            value = creditLimitInput,
                            fieldName = "credit limit",
                            errors = errors,
                            key = "creditLimit",
                        )
                        val statementBalance = parseOptionalMoney(
                            value = statementBalanceInput,
                            fieldName = "statement balance",
                            errors = errors,
                            key = "statementBalance",
                        )

                        if (creditLimit != null && creditLimit < 0L) {
                            errors["creditLimit"] = "Credit limit must be non-negative"
                        }

                        validationErrors.clear()
                        validationErrors.putAll(errors)
                        if (errors.isNotEmpty()) {
                            return@Button
                        }

                        onSave(
                            UpdateCreditCardSettingsRequest(
                                creditLimit = creditLimit,
                                statementBalance = statementBalance,
                                dueDate = dueDateMillis,
                            ),
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Save")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private fun parseOptionalMoney(
    value: String,
    fieldName: String,
    errors: MutableMap<String, String>,
    key: String,
): Long? {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) {
        return null
    }

    val parsed = MoneyInputUtils.parseToCents(trimmed)
    if (parsed == null) {
        errors[key] = "Enter a valid $fieldName amount"
    }
    return parsed
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

private fun formatDate(dateMillis: Long): String {
    return Instant
        .fromEpochMilliseconds(dateMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
        .toString()
}
