package com.petergabriel.budgetcalendar.features.creditcard.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.petergabriel.budgetcalendar.core.designsystem.component.BcButton
import com.petergabriel.budgetcalendar.core.designsystem.component.BcMetricHero
import com.petergabriel.budgetcalendar.core.designsystem.component.BcMetricHeroTone
import com.petergabriel.budgetcalendar.core.designsystem.component.BcSheetFooter
import com.petergabriel.budgetcalendar.core.designsystem.component.BcSheetShell
import com.petergabriel.budgetcalendar.core.designsystem.component.ButtonVariant
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils
import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.creditcard.domain.model.CreditCardSettings
import com.petergabriel.budgetcalendar.features.creditcard.domain.model.CreditCardSummary
import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import com.petergabriel.budgetcalendar.features.transactions.presentation.components.TransactionItemDensity
import com.petergabriel.budgetcalendar.features.transactions.presentation.components.TransactionListItem
import kotlin.math.abs

@Composable
fun CreditCardDetailSheet(
    isVisible: Boolean,
    summary: CreditCardSummary,
    settings: CreditCardSettings?,
    pendingTransactions: List<Transaction>,
    onMakePayment: () -> Unit,
    onEditSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!isVisible) return

    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing

    BcSheetShell(
        title = summary.accountName,
        onDismissRequest = onDismiss,
        body = {
            BcMetricHero(
                label = "CURRENT BALANCE",
                amount = summary.currentBalance,
                tone = if (summary.currentBalance < 0L) BcMetricHeroTone.Negative else BcMetricHeroTone.Positive,
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
            ) {
                SummaryRow(
                    label = "Statement Balance",
                    value = statementDueText(summary),
                )
                SummaryRow(
                    label = "Credit Limit",
                    value = settings?.creditLimit?.let(CurrencyUtils::formatCents) ?: "Not set",
                )
                SummaryRow(
                    label = "Available Credit",
                    value = summary.availableCredit?.let(CurrencyUtils::formatCents) ?: "Not set",
                )
                SummaryRow(label = "Utilization", value = utilizationText(summary))
                SummaryRow(
                    label = "Next Due Date",
                    value = nextDueDateText(
                        statementClosingDay = summary.statementClosingDay,
                        paymentDueDay = summary.paymentDueDay,
                    ) ?: "Not set",
                )
            }

            Text(
                text = "Pending & Overdue Expenses",
                style = typography.cardTitle,
                color = colors.textPrimary,
            )
            if (pendingTransactions.isEmpty()) {
                Text(
                    text = "No pending or overdue expenses for this card.",
                    style = typography.bodyMedium,
                    color = colors.textSecondary,
                )
            } else {
                pendingTransactions.forEach { transaction ->
                    TransactionListItem(
                        transaction = transaction,
                        accountName = summary.accountName,
                        density = TransactionItemDensity.Compact,
                        onTap = {},
                    )
                }
            }
        },
        footer = {
            BcSheetFooter {
                BcButton(
                    text = "Make Payment",
                    onClick = onMakePayment,
                    modifier = Modifier.weight(1f),
                    variant = ButtonVariant.Primary,
                )
                BcButton(
                    text = "Edit Settings",
                    onClick = onEditSettings,
                    modifier = Modifier.weight(1f),
                    variant = ButtonVariant.Outline,
                )
            }
        },
    )
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = typography.bodyMedium, color = colors.textSecondary)
        Text(text = value, style = typography.bodyMedium, color = colors.textPrimary)
    }
}

private fun statementDueText(summary: CreditCardSummary): String {
    val amount = summary.statementDueAmount ?: return "Not set"
    val dueDate = summary.statementDueDate?.let(DateUtils::formatMonthDay)
    return if (dueDate == null) {
        CurrencyUtils.formatCents(amount)
    } else {
        "${CurrencyUtils.formatCents(amount)} due $dueDate"
    }
}

private fun utilizationText(summary: CreditCardSummary): String {
    val limit = summary.creditLimit?.takeIf { amount -> amount > 0L } ?: return "Not set"
    val utilization = (abs(summary.currentBalance).toDouble() / limit.toDouble()).coerceIn(0.0, 1.0)
    return "${(utilization * 100).toInt()}%"
}

private fun nextDueDateText(
    statementClosingDay: Int?,
    paymentDueDay: Int?,
    nowMillis: Long = DateUtils.nowMillis(),
): String? {
    if (statementClosingDay == null || paymentDueDay == null) return null
    val lastClose = DateUtils.lastStatementClose(statementClosingDay, nowMillis)
    val dueMillis = DateUtils.paymentDueAfter(paymentDueDay, lastClose)
    return DateUtils.formatMonthDay(dueMillis)
}
