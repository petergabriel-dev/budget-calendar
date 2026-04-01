package com.petergabriel.budgetcalendar.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils

@Composable
fun HeroSafeToSpend(
    amount: Long,
    dailyRate: Long? = null,
    label: String = "SAFE TO SPEND",
    modifier: Modifier = Modifier,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
            ),
            color = colors.textSecondary,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = CurrencyUtils.formatCents(amount),
                style = typography.displayMedium,
                color = colors.textPrimary,
            )

            if (dailyRate != null) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "${CurrencyUtils.formatCents(dailyRate)} / day",
                    style = typography.section.copy(fontWeight = FontWeight.Light),
                    color = colors.textPrimary,
                )
            }
        }
    }
}
