package com.petergabriel.budgetcalendar.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme

@Composable
fun BcHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionIcon: ImageVector? = null,
    actionContentDescription: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val radius = BudgetCalendarTheme.radius

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = typography.headline,
            color = colors.textPrimary,
        )

        if (actionIcon != null && onActionClick != null) {
            Surface(
                shape = RoundedCornerShape(radius.lg),
                color = colors.bgSurface,
            ) {
                IconButton(
                    modifier = Modifier.size(44.dp),
                    onClick = onActionClick,
                ) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = actionContentDescription,
                        tint = colors.textPrimary,
                    )
                }
            }
        }
    }
}
