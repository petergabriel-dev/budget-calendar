package com.petergabriel.budgetcalendar.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme

enum class BadgeVariant {
    Pending,
    Overdue,
    Success,
    Warning,
    Error,
    Info,
}

@Composable
fun BcBadge(
    text: String,
    modifier: Modifier = Modifier,
    variant: BadgeVariant = BadgeVariant.Pending,
) {
    val token = tokenForVariant(variant)
    val typography = BudgetCalendarTheme.typography
    val radius = BudgetCalendarTheme.radius

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(radius.lg),
        color = token.containerColor,
        contentColor = token.contentColor,
        border = token.border,
    ) {
        Text(
            text = text.uppercase(),
            style = typography.caption,
            color = token.contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

private data class BadgeToken(
    val containerColor: Color,
    val contentColor: Color,
    val border: BorderStroke? = null,
)

@Composable
private fun tokenForVariant(variant: BadgeVariant): BadgeToken {
    val colors = BudgetCalendarTheme.colors
    return when (variant) {
        BadgeVariant.Pending -> BadgeToken(
            containerColor = colors.bgSurface,
            contentColor = colors.textSecondary,
            border = BorderStroke(width = 1.5.dp, color = colors.borderStrong),
        )

        BadgeVariant.Overdue -> BadgeToken(
            containerColor = colors.bgDark,
            contentColor = colors.textInverted,
        )

        BadgeVariant.Success -> BadgeToken(
            containerColor = colors.colorSuccessBg,
            contentColor = colors.colorSuccess,
        )

        BadgeVariant.Warning -> BadgeToken(
            containerColor = colors.colorWarningBg,
            contentColor = colors.colorWarning,
        )

        BadgeVariant.Error -> BadgeToken(
            containerColor = colors.colorErrorBg,
            contentColor = colors.colorError,
        )

        BadgeVariant.Info -> BadgeToken(
            containerColor = colors.colorInfoBg,
            contentColor = colors.colorInfo,
        )
    }
}
