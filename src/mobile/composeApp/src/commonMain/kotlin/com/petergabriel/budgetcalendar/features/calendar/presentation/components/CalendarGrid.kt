package com.petergabriel.budgetcalendar.features.calendar.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.petergabriel.budgetcalendar.features.calendar.domain.model.CalendarDay

private val WEEK_DAYS = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

@Composable
fun CalendarGrid(
    days: List<CalendarDay>,
    onDateSelected: (kotlinx.datetime.LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowCount = (days.size / 7).coerceAtLeast(1)
    val cellHeight = 84.dp
    val gridHeight = (cellHeight * rowCount) + (2.dp * (rowCount - 1))

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            WEEK_DAYS.forEach { dayLabel ->
                Text(
                    text = dayLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 6.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            userScrollEnabled = false,
        ) {
            items(days, key = { day -> day.date.toString() }) { day ->
                CalendarDayCell(
                    day = day,
                    onTap = { onDateSelected(day.date) },
                )
            }
        }
    }
}
