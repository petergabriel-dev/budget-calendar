package com.petergabriel.budgetcalendar.features.calendar.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@Composable
fun DayTransactionSectionHeader(
    selectedDate: LocalDate,
    modifier: Modifier = Modifier,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val label = if (selectedDate == today) {
        "Today · ${selectedDate.toMonthDayLabel()}"
    } else {
        "${selectedDate.toWeekdayLabel()} · ${selectedDate.toMonthDayLabel()}"
    }

    Column(
        modifier = modifier.padding(top = 24.dp, bottom = 8.dp),
    ) {
        Text(
            text = label,
            style = typography.cardTitle.copy(fontWeight = FontWeight.ExtraBold),
            color = colors.textPrimary,
        )
    }
}

private fun LocalDate.toMonthDayLabel(): String {
    val monthLabel = month.name
        .lowercase()
        .replaceFirstChar { character -> character.titlecase() }
        .take(3)

    return "$monthLabel $day"
}

private fun LocalDate.toWeekdayLabel(): String {
    return dayOfWeek.name
        .lowercase()
        .replaceFirstChar { character -> character.titlecase() }
}
