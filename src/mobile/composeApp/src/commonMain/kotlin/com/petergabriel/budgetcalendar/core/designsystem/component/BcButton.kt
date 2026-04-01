package com.petergabriel.budgetcalendar.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.designsystem.theme.BcColors

enum class ButtonVariant {
    Primary,
    Outline,
    Ghost,
    Destructive,
    PrimaryIconLeading,
    OutlineIconTrailing,
}

@Composable
fun BcButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val radius = BudgetCalendarTheme.radius

    val token = variantToken(variant = variant, colors = colors)
    val shape: Shape = RoundedCornerShape(radius.full)
    val alpha = if (enabled) 1f else 0.6f

    Surface(
        modifier = modifier
            .height(48.dp)
            .defaultMinSize(minWidth = 96.dp),
        shape = shape,
        color = token.containerColor.copy(alpha = alpha),
        contentColor = token.contentColor.copy(alpha = alpha),
        border = token.border,
        onClick = onClick,
        enabled = enabled,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val leading = leadingIcon ?: when (variant) {
                ButtonVariant.PrimaryIconLeading -> Icons.Outlined.Add
                ButtonVariant.Destructive -> Icons.Outlined.Delete
                else -> null
            }
            val trailing = trailingIcon ?: when (variant) {
                ButtonVariant.OutlineIconTrailing -> Icons.AutoMirrored.Outlined.ArrowForward
                else -> null
            }

            if (leading != null) {
                Icon(
                    imageVector = leading,
                    contentDescription = null,
                )
            }

            Text(
                text = text,
                style = typography.bodyLarge,
                color = token.contentColor.copy(alpha = alpha),
            )

            if (trailing != null) {
                Icon(
                    imageVector = trailing,
                    contentDescription = null,
                )
            }
        }
    }
}

private data class BcButtonToken(
    val containerColor: Color,
    val contentColor: Color,
    val border: BorderStroke?,
)

private fun variantToken(
    variant: ButtonVariant,
    colors: BcColors,
): BcButtonToken {
    return when (variant) {
        ButtonVariant.Primary,
        ButtonVariant.PrimaryIconLeading,
        -> BcButtonToken(
            containerColor = colors.bgDark,
            contentColor = colors.textInverted,
            border = null,
        )

        ButtonVariant.Outline,
        ButtonVariant.OutlineIconTrailing,
        -> BcButtonToken(
            containerColor = Color.Transparent,
            contentColor = colors.textPrimary,
            border = BorderStroke(width = 2.dp, color = colors.borderStrong),
        )

        ButtonVariant.Ghost -> BcButtonToken(
            containerColor = Color.Transparent,
            contentColor = colors.textPrimary,
            border = null,
        )

        ButtonVariant.Destructive -> BcButtonToken(
            containerColor = colors.colorError,
            contentColor = colors.textInverted,
            border = null,
        )
    }
}
