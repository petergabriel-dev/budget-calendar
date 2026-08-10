package com.petergabriel.budgetcalendar.features.calendar.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils
import com.petergabriel.budgetcalendar.features.calendar.domain.model.CalendarDay

internal const val CALENDAR_DAY_CELL_TINT_ALPHA = 0.35f

@Composable
fun CalendarDayCell(
    day: CalendarDay,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing
    val radius = BudgetCalendarTheme.radius

    val selectedBackground = colors.bgDark
    val successTintBackground = colors.colorSuccessBg.copy(alpha = CALENDAR_DAY_CELL_TINT_ALPHA)
    val errorTintBackground = colors.colorErrorBg.copy(alpha = CALENDAR_DAY_CELL_TINT_ALPHA)
    val baseBackground = colors.bgPrimary
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

    val contentColor = if (day.isSelected) colors.textInverted else colors.textPrimary
    val borderColor = resolveCalendarDayCellBorderColor(
        isToday = day.isToday,
        todayBorder = colors.focusRing,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .defaultMinSize(minHeight = spacing.spacing12)
            .clip(cellShape)
            .border(width = spacing.spacing1 / 2, color = borderColor, shape = cellShape)
            .background(background)
            .clickable(onClick = onTap)
            .alpha(if (day.isCurrentMonth) 1f else 0.55f)
            .semantics {
                role = Role.Button
                selected = day.isSelected
                contentDescription = calendarDayContentDescription(day)
            }
            .padding(spacing.spacing1),
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
                .padding(top = spacing.spacing1),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (day.dailySummary?.hasPending == true) {
                StateDot(color = colors.colorWarningText)
            }
            if (day.dailySummary?.hasOverdue == true) {
                StateDot(color = colors.colorErrorText)
            }
        }
    }
}

internal fun calendarDayContentDescription(day: CalendarDay): String = buildString {
    append(day.date.toAccessibleCalendarDate())
    if (day.isToday) append(", today")
    if (day.isSelected) append(", selected")
    day.dailySummary?.let { summary ->
        append(", ${summary.transactionCount} ${if (summary.transactionCount == 1) "transaction" else "transactions"}")
        when {
            summary.netAmount > 0L -> append(", net income ${CurrencyUtils.formatCents(summary.netAmount)}")
            summary.netAmount < 0L -> append(", net expense ${CurrencyUtils.formatCents(-summary.netAmount)}")
        }
        if (summary.hasPending) append(", pending")
        if (summary.hasOverdue) append(", overdue")
    }
}

private fun kotlinx.datetime.LocalDate.toAccessibleCalendarDate(): String {
    val monthName = month.name
        .lowercase()
        .replaceFirstChar { character -> character.titlecase() }
    val weekdayName = dayOfWeek.name
        .lowercase()
        .replaceFirstChar { character -> character.titlecase() }
    return "$weekdayName, $monthName $day"
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
    val spacing = BudgetCalendarTheme.spacing
    Box(
        modifier = Modifier
            .padding(start = spacing.spacing1 / 2)
            .size(spacing.spacing1 + spacing.spacing1 / 2)
            .clip(CircleShape)
            .background(color),
    )
}
