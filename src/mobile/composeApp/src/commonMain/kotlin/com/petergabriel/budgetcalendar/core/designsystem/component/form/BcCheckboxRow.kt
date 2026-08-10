package com.petergabriel.budgetcalendar.core.designsystem.component.form

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme

@Composable
fun BcCheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val radius = BudgetCalendarTheme.radius
    val spacing = BudgetCalendarTheme.spacing

    Row(
        modifier = modifier
            .semantics { role = Role.Checkbox }
            .defaultMinSize(minHeight = spacing.spacing12)
            .clickable { onCheckedChange(!checked) },
        horizontalArrangement = Arrangement.spacedBy(spacing.spacing3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(spacing.spacing3 * 2)
                .clip(RoundedCornerShape(radius.md))
                .then(
                    if (checked) {
                        Modifier.background(colors.bgDark)
                    } else {
                        Modifier.border(
                            width = spacing.spacing1 / 2,
                            color = colors.borderStrong,
                            shape = RoundedCornerShape(radius.md),
                        )
                    },
                ),
        )

        Text(
            text = label,
            style = typography.bodyMedium,
            color = colors.textPrimary,
        )
    }
}
