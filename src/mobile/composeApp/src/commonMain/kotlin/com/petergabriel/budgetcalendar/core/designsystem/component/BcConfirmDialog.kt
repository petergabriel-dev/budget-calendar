package com.petergabriel.budgetcalendar.core.designsystem.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme

@Composable
fun BcConfirmDialog(
    title: String,
    message: String,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmLabel: String = "Confirm",
    dismissLabel: String = "Cancel",
    confirmVariant: ButtonVariant = ButtonVariant.Destructive,
    isBusy: Boolean = false,
    busyLabel: String = "Working…",
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val cancelFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isBusy) {
        if (!isBusy) {
            cancelFocusRequester.requestFocus()
        }
    }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = {
            if (!isBusy) {
                onDismissRequest()
            }
        },
        title = {
            Text(
                text = title,
                style = typography.cardTitle,
                color = colors.textPrimary,
            )
        },
        text = {
            Text(
                text = message,
                style = typography.bodyMedium,
                color = colors.textSecondary,
            )
        },
        dismissButton = {
            BcButton(
                text = dismissLabel,
                onClick = onDismissRequest,
                variant = ButtonVariant.Ghost,
                enabled = !isBusy,
                modifier = Modifier.focusRequester(cancelFocusRequester),
            )
        },
        confirmButton = {
            BcButton(
                text = if (isBusy) busyLabel else confirmLabel,
                onClick = onConfirm,
                variant = confirmVariant,
                enabled = !isBusy,
            )
        },
    )
}
