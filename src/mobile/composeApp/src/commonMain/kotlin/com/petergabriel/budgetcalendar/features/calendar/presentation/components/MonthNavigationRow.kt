package com.petergabriel.budgetcalendar.features.calendar.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.features.calendar.domain.model.firstDay
import kotlinx.datetime.YearMonth

@Composable
fun MonthNavigationRow(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BudgetCalendarTheme.colors
    val spacing = BudgetCalendarTheme.spacing
    val typography = BudgetCalendarTheme.typography

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = currentMonth.toDisplayLabel(),
            style = typography.title,
            color = colors.textPrimary,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.spacing4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                modifier = Modifier.semantics { contentDescription = "Previous month" },
                onClick = onPreviousMonth,
            ) {
                Text(
                    text = "‹",
                    style = typography.section,
                    color = colors.textPrimary,
                )
            }

            IconButton(
                modifier = Modifier.semantics { contentDescription = "Next month" },
                onClick = onNextMonth,
            ) {
                Text(
                    text = "›",
                    style = typography.section,
                    color = colors.textPrimary,
                )
            }
        }
    }
}

internal fun YearMonth.toDisplayLabel(): String {
    val monthName = firstDay().month.name
        .lowercase()
        .replaceFirstChar { character -> character.titlecase() }
        .take(3)

    return "$monthName $year"
}
