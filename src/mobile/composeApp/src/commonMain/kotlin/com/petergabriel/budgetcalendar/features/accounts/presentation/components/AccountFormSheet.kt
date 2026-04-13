package com.petergabriel.budgetcalendar.features.accounts.presentation.components

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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.petergabriel.budgetcalendar.core.designsystem.component.form.BcInputGroup
import com.petergabriel.budgetcalendar.core.utils.MoneyInputUtils
import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account
import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountFormSheet(
    isVisible: Boolean,
    initialData: Account?,
    isSubmitting: Boolean,
    onSave: (name: String, type: AccountType, balance: Long, isInSpendingPool: Boolean, description: String) -> Unit,
    onCancel: () -> Unit,
    onDelete: ((Account) -> Unit)? = null,
) {
    if (!isVisible) {
        return
    }

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
    val validationErrors = remember(initialData?.id) { mutableStateMapOf<String, String>() }
    var typeMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(initialData?.id) {
        validationErrors.clear()
    }

    LaunchedEffect(selectedType) {
        if (selectedType == AccountType.CREDIT_CARD) {
            includeInSpendingPool = false
        }
    }

    ModalBottomSheet(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (initialData == null) "Add Account" else "Edit Account",
                style = MaterialTheme.typography.titleLarge,
            )

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    validationErrors.remove("name")
                },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
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
                        text = if (initialData == null) "Initial Balance" else "Balance",
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = validationErrors.containsKey("balance"),
                singleLine = true,
            )
            InlineError(validationErrors["balance"])

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
                style = MaterialTheme.typography.bodySmall,
                color = if (descriptionInput.length > ACCOUNT_DESCRIPTION_SOFT_CAP) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Include in Safe to Spend",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = if (selectedType == AccountType.CREDIT_CARD) {
                            "Credit card accounts are always excluded to avoid double deductions."
                        } else {
                            "Only enabled accounts are included in available funds."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Switch(
                    modifier = Modifier.testTag(INCLUDE_IN_SPENDING_POOL_SWITCH_TAG),
                    checked = includeInSpendingPool,
                    onCheckedChange = { includeInSpendingPool = it },
                    enabled = selectedType != AccountType.CREDIT_CARD,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                        val errors = validateAccountForm(
                            name = name,
                            balanceInput = balanceInput,
                        )
                        validationErrors.clear()
                        validationErrors.putAll(errors)
                        if (errors.isNotEmpty()) {
                            return@Button
                        }

                        val balance = checkNotNull(MoneyInputUtils.parseToCents(balanceInput.trim()))
                        onSave(name.trim(), selectedType, balance, includeInSpendingPool, descriptionInput)
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
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private fun validateAccountForm(
    name: String,
    balanceInput: String,
): Map<String, String> {
    val errors = mutableMapOf<String, String>()

    if (name.trim().isEmpty()) {
        errors["name"] = "Name is required"
    }

    if (MoneyInputUtils.parseToCents(balanceInput.trim()) == null) {
        errors["balance"] = "Enter a valid amount"
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
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(start = 4.dp),
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
private const val ACCOUNT_TYPE_FIELD_TAG = "account_type_field"
private const val INCLUDE_IN_SPENDING_POOL_SWITCH_TAG = "include_in_spending_pool_switch"
