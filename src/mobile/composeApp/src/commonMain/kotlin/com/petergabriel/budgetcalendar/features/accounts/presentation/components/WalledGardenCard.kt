package com.petergabriel.budgetcalendar.features.accounts.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils
import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account

@Composable
fun WalledGardenCard(
    account: Account,
    balance: Long,
    isLiability: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing
    val radius = BudgetCalendarTheme.radius
    val badgeModifier = if (isLiability) {
        Modifier
            .clip(RoundedCornerShape(radius.md))
            .background(colors.bgSurface)
            .border(spacing.spacing1 / 2, colors.borderStrong, RoundedCornerShape(radius.md))
            .padding(horizontal = spacing.spacing3, vertical = spacing.spacing2)
    } else {
        Modifier
            .clip(RoundedCornerShape(radius.md))
            .background(colors.bgDark)
            .padding(horizontal = spacing.spacing3, vertical = spacing.spacing2)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = spacing.spacing12)
            .clip(RoundedCornerShape(radius.xl))
            .background(colors.bgSurface)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = "${account.name}, ${account.type.name}, ${CurrencyUtils.formatCents(balance)}, excluded from Safe to Spend"
            }
            .padding(spacing.spacing4),
        verticalArrangement = Arrangement.spacedBy(spacing.spacing3),
    ) {
        Text(
            text = if (isLiability) "LIABILITY" else account.type.name.replace('_', ' '),
            style = typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = if (isLiability) colors.textSecondary else colors.textInverted,
            modifier = badgeModifier,
        )
        Text(
            text = account.name,
            style = typography.cardTitle,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = CurrencyUtils.formatCents(balance),
            style = typography.section,
            fontWeight = FontWeight.ExtraBold,
            color = colors.textPrimary,
        )
        Text(
            text = account.description ?: account.name,
            style = typography.bodySmall,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
