package com.petergabriel.budgetcalendar.features.calendar.presentation.components

import androidx.compose.foundation.background
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
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils
import com.petergabriel.budgetcalendar.features.calendar.domain.model.CalendarDay

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
    val todayBackground = colors.colorSuccess.copy(alpha = 0.2f)
    val baseBackground = Color.Transparent

    val background = when {
        day.isSelected -> selectedBackground
        day.isToday -> todayBackground
        else -> baseBackground
    }

    val contentColor = if (day.isSelected) {
        colors.textInverted
    } else {
        colors.textSecondary
    }

    val netAmount = day.dailySummary?.netAmount
    val netText = netAmount?.let { amount ->
        CurrencyUtils.formatCents(amount, includePlusSign = true)
    } ?: "—"

    val netColor = when {
        day.isSelected -> colors.textInverted
        netAmount == null || netAmount == 0L -> colors.textTertiary
        netAmount > 0L -> colors.colorSuccess
        else -> colors.colorError
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(radius.lg))
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

        Text(
            text = netText,
            style = typography.calendarAmount,
            fontWeight = FontWeight.SemiBold,
            color = netColor,
            maxLines = 1,
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
