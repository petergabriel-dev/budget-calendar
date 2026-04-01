package com.petergabriel.budgetcalendar.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils

@Composable
fun HeroNetWorth(
    amount: Long,
    modifier: Modifier = Modifier,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography

    Text(
        text = CurrencyUtils.formatCents(amount),
        modifier = modifier,
        style = typography.displayLarge.copy(
            fontWeight = FontWeight.Black,
            letterSpacing = (-3).sp,
            lineHeight = 61.2.sp,
        ),
        color = colors.textPrimary,
    )
}
