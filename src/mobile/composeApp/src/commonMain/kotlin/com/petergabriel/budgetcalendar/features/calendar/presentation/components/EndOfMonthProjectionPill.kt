package com.petergabriel.budgetcalendar.features.calendar.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils

@Composable
fun EndOfMonthProjectionPill(
    projectionAmount: Long,
    modifier: Modifier = Modifier,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing
    val radius = BudgetCalendarTheme.radius

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius.lg))
            .background(
                color = colors.bgSurface,
                shape = RoundedCornerShape(radius.lg),
            )
            .padding(horizontal = spacing.spacing4, vertical = spacing.spacing3),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "End of Month Projection",
            style = typography.bodyMedium.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = colors.textSecondary,
        )
        Text(
            text = projectionAmount.formatCurrency(),
            style = typography.bodyLarge.copy(
                fontFamily = typography.title.fontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
            ),
            color = if (projectionAmount <= 0L) colors.colorError else colors.textPrimary,
        )
    }
}

private fun Long.formatCurrency(): String = CurrencyUtils.formatCents(this)
