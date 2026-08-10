package com.petergabriel.budgetcalendar.features.accounts.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.petergabriel.budgetcalendar.core.designsystem.component.BcButton
import com.petergabriel.budgetcalendar.core.designsystem.component.BcConfirmDialog
import com.petergabriel.budgetcalendar.core.designsystem.component.BcMetricHero
import com.petergabriel.budgetcalendar.core.designsystem.component.BcMetricHeroTone
import com.petergabriel.budgetcalendar.core.designsystem.component.BcSheetFooter
import com.petergabriel.budgetcalendar.core.designsystem.component.BcSheetShell
import com.petergabriel.budgetcalendar.core.designsystem.component.ButtonVariant
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account

@Composable
fun AccountDetailsSheet(
    isVisible: Boolean,
    account: Account,
    isDeleting: Boolean = false,
    errorMessage: String? = null,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!isVisible) return

    var showDeleteConfirmation by remember(account.id) { mutableStateOf(false) }
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing

    BcSheetShell(
        title = account.name,
        onDismissRequest = onDismiss,
        body = {
            BcMetricHero(
                label = "CURRENT BALANCE",
                amount = account.balance,
                tone = if (account.balance < 0L) BcMetricHeroTone.Negative else BcMetricHeroTone.Neutral,
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
            ) {
                DetailRow(label = "Type", value = account.type.name.lowercase().replace('_', ' '))
                DetailRow(
                    label = "Safe to Spend",
                    value = if (account.isInSpendingPool) "Included" else "Excluded",
                )
                account.description?.takeIf { it.isNotBlank() }?.let { description ->
                    DetailRow(label = "Description", value = description)
                }
            }
            errorMessage?.let { error ->
                Text(
                    text = error,
                    style = typography.bodySmall,
                    color = colors.colorErrorText,
                )
            }
        },
        footer = {
            BcSheetFooter {
                BcButton(
                    text = "Edit",
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    variant = ButtonVariant.Outline,
                )
                BcButton(
                    text = if (isDeleting) "Deleting…" else "Delete",
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier.weight(1f),
                    variant = ButtonVariant.Destructive,
                    enabled = !isDeleting,
                )
            }
        },
    )

    if (showDeleteConfirmation) {
        BcConfirmDialog(
            title = "Delete ${account.name}?",
            message = "This action cannot be undone. Accounts with transactions cannot be deleted.",
            onDismissRequest = { if (!isDeleting) showDeleteConfirmation = false },
            onConfirm = onDelete,
            confirmLabel = "Delete",
            dismissLabel = "Keep account",
            isBusy = isDeleting,
            busyLabel = "Deleting…",
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = typography.bodyMedium, color = colors.textSecondary)
        Text(text = value, style = typography.bodyMedium, color = colors.textPrimary)
    }
}
