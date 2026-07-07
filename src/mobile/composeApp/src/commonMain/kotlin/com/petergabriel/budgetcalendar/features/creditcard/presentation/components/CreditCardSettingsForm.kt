package com.petergabriel.budgetcalendar.features.creditcard.presentation.components

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    var statementClosingDayInput by remember(initialSettings?.accountId) {
        mutableStateOf(initialSettings?.statementClosingDay?.toString().orEmpty())
    }
    var paymentDueDayInput by remember(initialSettings?.accountId) {
        mutableStateOf(initialSettings?.paymentDueDay?.toString().orEmpty())
    }
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
                value = statementClosingDayInput,
                onValueChange = {
                    statementClosingDayInput = it
                    validationErrors.remove("statementClosingDay")
                },
                label = { Text("Closing day (1-31, optional)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = validationErrors.containsKey("statementClosingDay"),
                singleLine = true,
            )
            InlineError(validationErrors["statementClosingDay"])

            OutlinedTextField(
                value = paymentDueDayInput,
                onValueChange = {
                    paymentDueDayInput = it
                    validationErrors.remove("paymentDueDay")
                },
                label = { Text("Due day (1-31, optional)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = validationErrors.containsKey("paymentDueDay"),
                singleLine = true,
            )
            InlineError(validationErrors["paymentDueDay"])

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
                        val statementClosingDay = parseOptionalDay(
                            value = statementClosingDayInput,
                            fieldName = "closing day",
                            errors = errors,
                            key = "statementClosingDay",
                        )
                        val paymentDueDay = parseOptionalDay(
                            value = paymentDueDayInput,
                            fieldName = "due day",
                            errors = errors,
                            key = "paymentDueDay",
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
                                statementBalance = initialSettings?.statementBalance,
                                dueDate = initialSettings?.dueDate,
                                statementClosingDay = statementClosingDay,
                                paymentDueDay = paymentDueDay,
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

private fun parseOptionalDay(
    value: String,
    fieldName: String,
    errors: MutableMap<String, String>,
    key: String,
): Int? {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) {
        return null
    }

    val parsed = trimmed.toIntOrNull()
    if (parsed == null || parsed !in 1..31) {
        errors[key] = "Enter a valid $fieldName from 1 to 31"
    }
    return parsed
}
