package com.petergabriel.budgetcalendar.features.calendar.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.features.calendar.domain.model.CalendarDay

internal const val CALENDAR_DAY_CELL_TINT_ALPHA = 0.2f

@Composable
fun CalendarDayCell(
    day: CalendarDay,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val radius = BudgetCalendarTheme.radius

    val selectedBackground = colors.bgDark
    val successTintBackground = colors.colorSuccess.copy(alpha = CALENDAR_DAY_CELL_TINT_ALPHA)
    val errorTintBackground = colors.colorError.copy(alpha = CALENDAR_DAY_CELL_TINT_ALPHA)
    val baseBackground = Color.Transparent
    val cellShape = RoundedCornerShape(radius.lg)

    val background = resolveCalendarDayCellBackground(
        isSelected = day.isSelected,
        isToday = day.isToday,
        netAmount = day.dailySummary?.netAmount,
        selectedBackground = selectedBackground,
        successTint = successTintBackground,
        errorTint = errorTintBackground,
        baseBackground = baseBackground,
    )

    val contentColor = if (day.isSelected) {
        colors.textInverted
    } else {
        colors.textSecondary
    }

    val borderColor = resolveCalendarDayCellBorderColor(
        isToday = day.isToday,
        todayBorder = colors.bgDark,
    )

    Column(
        modifier = modifier
            .clip(cellShape)
            .border(width = 1.5.dp, color = borderColor, shape = cellShape)
            .background(background)
            .clickable(onClick = onTap)
            .alpha(if (day.isCurrentMonth) 1f else 0.4f)
            .padding(BudgetCalendarTheme.spacing.spacing1)
            .heightIn(min = 72.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = day.date.day.toString(),
            style = typography.calendarDay,
            fontWeight = if (day.isSelected) FontWeight.Black else FontWeight.Medium,
            color = contentColor,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (day.dailySummary?.hasPending == true) {
                StateDot(color = colors.colorWarning)
            }
            if (day.dailySummary?.hasOverdue == true) {
                StateDot(color = colors.colorError)
            }
            if (day.dailySummary?.hasConfirmed == true) {
                Text(
                    text = "✓",
                    color = if (day.isSelected) colors.textInverted else colors.colorSuccess,
                    style = typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

internal fun resolveCalendarDayCellBackground(
    isSelected: Boolean,
    isToday: Boolean,
    netAmount: Long?,
    selectedBackground: Color,
    successTint: Color,
    errorTint: Color,
    baseBackground: Color,
): Color {
    val resolvedNetAmount = netAmount ?: 0L

    return when {
        isSelected -> selectedBackground
        isToday && resolvedNetAmount < 0L -> errorTint
        isToday -> successTint
        !isToday && resolvedNetAmount > 0L -> successTint
        !isToday && resolvedNetAmount < 0L -> errorTint
        else -> baseBackground
    }
}

internal fun resolveCalendarDayCellBorderColor(
    isToday: Boolean,
    todayBorder: Color,
): Color {
    return if (isToday) todayBorder else Color.Transparent
}

@Composable
private fun StateDot(color: Color) {
    Box(
        modifier = Modifier
            .padding(start = 2.dp)
            .size(7.dp)
            .clip(CircleShape)
            .background(color),
    )
}
