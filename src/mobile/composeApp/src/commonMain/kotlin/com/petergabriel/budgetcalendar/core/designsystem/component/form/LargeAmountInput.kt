package com.petergabriel.budgetcalendar.core.designsystem.component.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme

@Composable
fun LargeAmountInput(
    label: String,
    amount: String,
    onAmountChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val amountStyle = typography.displayLarge.copy(lineHeight = 61.2.sp)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BudgetCalendarTheme.spacing.spacing2),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label.uppercase(),
            style = typography.caption.copy(letterSpacing = 2.sp),
            color = colors.textSecondary,
        )

        BasicTextField(
            value = amount,
            onValueChange = onAmountChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = amountStyle.copy(color = colors.textPrimary),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.Center) {
                    if (amount.isBlank()) {
                        Text(
                            text = "₱0.00",
                            style = amountStyle,
                            color = colors.textTertiary,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}
