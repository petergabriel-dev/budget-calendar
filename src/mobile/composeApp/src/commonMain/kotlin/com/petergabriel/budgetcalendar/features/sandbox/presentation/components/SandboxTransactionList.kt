package com.petergabriel.budgetcalendar.features.sandbox.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.petergabriel.budgetcalendar.core.designsystem.component.BcButton
import com.petergabriel.budgetcalendar.core.designsystem.component.ButtonVariant
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxTransaction

@Composable
fun SandboxTransactionList(
    transactions: List<SandboxTransaction>,
    onAddTap: () -> Unit,
    onPromote: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BudgetCalendarTheme.colors
    val spacing = BudgetCalendarTheme.spacing
    val typography = BudgetCalendarTheme.typography

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.spacing3),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Sandbox Transactions",
                style = typography.section,
                color = colors.textPrimary,
            )
            BcButton(
                text = "Add",
                onClick = onAddTap,
                variant = ButtonVariant.Ghost,
            )
        }

        if (transactions.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.spacing5),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.spacing3),
            ) {
                Text(
                    text = "No transactions yet. Tap Add to start planning.",
                    style = typography.bodyMedium,
                    color = colors.textSecondary,
                )
                BcButton(
                    text = "Add",
                    onClick = onAddTap,
                    variant = ButtonVariant.Ghost,
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
            ) {
                transactions.forEach { transaction ->
                    SandboxTransactionItem(
                        transaction = transaction,
                        onPromote = onPromote,
                        onRemove = onRemove,
                    )
                }
            }
        }
    }
}
