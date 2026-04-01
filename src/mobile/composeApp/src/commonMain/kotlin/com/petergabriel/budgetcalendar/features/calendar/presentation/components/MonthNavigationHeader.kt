package com.petergabriel.budgetcalendar.features.calendar.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.features.calendar.domain.model.firstDay
import kotlinx.datetime.YearMonth

@Composable
fun MonthNavigationHeader(
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
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = currentMonth.toDisplayLabel(),
            style = typography.title.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
                letterSpacing = (-1).sp,
            ),
            color = colors.textPrimary,
            modifier = Modifier.padding(end = spacing.spacing4),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.spacing4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onPreviousMonth,
            ) {
                Text(
                    text = "‹",
                    style = typography.section.copy(fontWeight = FontWeight.ExtraBold),
                    color = colors.textPrimary,
                )
            }

            IconButton(
                onClick = onNextMonth,
            ) {
                Text(
                    text = "›",
                    style = typography.section.copy(fontWeight = FontWeight.ExtraBold),
                    color = colors.textPrimary,
                )
            }
        }
    }
}

private fun YearMonth.toDisplayLabel(): String {
    val monthName = firstDay().month.name
        .lowercase()
        .replaceFirstChar { character -> character.titlecase() }

    return "$monthName $year"
}
