package com.petergabriel.budgetcalendar.features.calendar.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@Composable
fun DayTransactionSectionHeader(
    selectedDate: LocalDate,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val label = formatDayTransactionHeaderLabel(selectedDate = selectedDate)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = typography.cardTitle,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )

        IconButton(
            onClick = onExpand,
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "View all transactions",
                tint = colors.textPrimary,
            )
        }
    }
}

internal fun formatDayTransactionHeaderLabel(
    selectedDate: LocalDate,
    today: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
): String {
    return if (selectedDate == today) {
        "Today · ${selectedDate.toMonthDayLabel()}"
    } else {
        "${selectedDate.toWeekdayLabel()} · ${selectedDate.toMonthDayLabel()}"
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
