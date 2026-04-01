package com.petergabriel.budgetcalendar.features.recurring.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.petergabriel.budgetcalendar.core.designsystem.component.BcButton
import com.petergabriel.budgetcalendar.core.designsystem.component.ButtonVariant
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils
import com.petergabriel.budgetcalendar.features.recurring.domain.model.GeneratedTransaction
import com.petergabriel.budgetcalendar.features.recurring.domain.model.RecurringTransaction
import com.petergabriel.budgetcalendar.features.recurring.domain.model.RecurrenceType
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.math.min
import kotlin.time.Clock

@Composable
fun RecurringListScreen(
    recurringTransactions: List<RecurringTransaction>,
    upcomingGenerated: List<GeneratedTransaction>,
    modifier: Modifier = Modifier,
    onRecurringTap: (RecurringTransaction) -> Unit,
    onToggleActive: (RecurringTransaction, Boolean) -> Unit,
    onAddRecurring: () -> Unit,
    onGenerateMonthly: () -> Unit,
) {
    val spacing = BudgetCalendarTheme.spacing
    val monthlyIncome = recurringTransactions
        .filter { recurring -> recurring.isActive && recurring.type == RecurrenceType.INCOME }
        .sumOf { recurring -> recurring.amount }
    val monthlyExpense = recurringTransactions
        .filter { recurring -> recurring.isActive && recurring.type == RecurrenceType.EXPENSE }
        .sumOf { recurring -> recurring.amount }
    val monthlyNet = monthlyIncome - monthlyExpense

    Box(modifier = modifier.fillMaxSize()) {
        if (recurringTransactions.isEmpty()) {
            EmptyState(modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = spacing.spacing4),
                verticalArrangement = Arrangement.spacedBy(spacing.spacing3),
            ) {
                item {
                    MonthlySummary(
                        income = monthlyIncome,
                        expense = monthlyExpense,
                        net = monthlyNet,
                    )
                }

                item {
                    BcButton(
                        text = "Generate This Month",
                        onClick = onGenerateMonthly,
                        modifier = Modifier.fillMaxWidth(),
                        variant = ButtonVariant.Outline,
                    )
                }

                items(recurringTransactions, key = { recurring -> recurring.id }) { recurring ->
                    val nextDate = resolveNextDate(recurring, upcomingGenerated)
                    RecurringCard(
                        recurring = recurring,
                        nextScheduledDate = nextDate,
                        onTap = { onRecurringTap(recurring) },
                        onToggleActive = { isActive -> onToggleActive(recurring, isActive) },
                    )
                }
            }
        }

        BcButton(
            text = "Add Recurring",
            onClick = onAddRecurring,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(spacing.spacing5),
            variant = ButtonVariant.PrimaryIconLeading,
        )
    }
}

@Composable
private fun MonthlySummary(
    income: Long,
    expense: Long,
    net: Long,
) {
    val spacing = BudgetCalendarTheme.spacing
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val radius = BudgetCalendarTheme.radius

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.spacing2),
        shape = RoundedCornerShape(radius.xl),
        color = colors.bgSurface,
    ) {
        Column(
            modifier = Modifier.padding(spacing.spacing4),
            verticalArrangement = Arrangement.spacedBy(spacing.spacing1),
        ) {
            Text(
                text = "Monthly Recurring",
                style = typography.bodySmall,
                color = colors.textSecondary,
            )
            Text(
                text = "Income: ${CurrencyUtils.formatCents(income, includePlusSign = true)}",
                style = typography.bodyMedium,
                color = colors.colorSuccess,
            )
            Text(
                text = "Expense: ${CurrencyUtils.formatCents(-expense)}",
                style = typography.bodyMedium,
                color = colors.colorError,
            )
            Text(
                text = "Net: ${CurrencyUtils.formatCents(net, includePlusSign = true)}",
                style = typography.cardTitle,
                fontWeight = FontWeight.Bold,
                color = if (net >= 0L) colors.colorSuccess else colors.colorError,
            )
        }
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
) {
    val spacing = BudgetCalendarTheme.spacing
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography

    Column(
        modifier = modifier.padding(horizontal = spacing.spacing6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
    ) {
        Text(
            text = "No recurring transactions",
            style = typography.section,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
        )
        Text(
            text = "Add recurring income, expenses, or transfers.",
            style = typography.bodyMedium,
            color = colors.textSecondary,
        )
    }
}

private fun resolveNextDate(
    recurring: RecurringTransaction,
    upcomingGenerated: List<GeneratedTransaction>,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): Long {
    val upcomingDate = upcomingGenerated
        .filter { generated -> generated.recurringId == recurring.id }
        .minByOrNull(GeneratedTransaction::date)
        ?.date
    if (upcomingDate != null) {
        return upcomingDate
    }

    val now = Clock.System.now().toEpochMilliseconds()
    val nowDate = kotlin.time.Instant
        .fromEpochMilliseconds(now)
        .toLocalDateTime(timeZone)
        .date
    val thisMonthStart = LocalDate(nowDate.year, nowDate.month.number, 1)

    val thisMonthTarget = clampDay(thisMonthStart, recurring.dayOfMonth)
    val candidate = if (thisMonthTarget < nowDate) {
        val nextMonthStart = thisMonthStart.plus(DatePeriod(months = 1))
        clampDay(nextMonthStart, recurring.dayOfMonth)
    } else {
        thisMonthTarget
    }

    return candidate.atStartOfDayIn(timeZone).toEpochMilliseconds()
}

private fun clampDay(monthStart: LocalDate, dayOfMonth: Int): LocalDate {
    val lastDay = monthStart.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1)).day
    val clampedDay = min(dayOfMonth, lastDay)
    return LocalDate(monthStart.year, monthStart.month.number, clampedDay)
}
