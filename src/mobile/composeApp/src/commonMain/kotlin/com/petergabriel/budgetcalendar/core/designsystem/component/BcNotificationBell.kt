package com.petergabriel.budgetcalendar.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme

@Composable
fun BcNotificationBell(
    unreadCount: Int,
    onClick: () -> Unit,
) {
    val colors = BudgetCalendarTheme.colors
    val radius = BudgetCalendarTheme.radius
    val spacing = BudgetCalendarTheme.spacing
    val touchTarget = spacing.spacing12 - spacing.spacing1

    Surface(
        shape = RoundedCornerShape(radius.lg),
        color = colors.bgSurface,
    ) {
        Box {
            IconButton(
                modifier = Modifier.size(touchTarget),
                onClick = onClick,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint = colors.textPrimary,
                )
            }

            if (unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = spacing.spacing1, end = spacing.spacing1),
                ) {
                    BcBadge(
                        text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                        variant = BadgeVariant.Error,
                    )
                }
            }
        }
    }
}
