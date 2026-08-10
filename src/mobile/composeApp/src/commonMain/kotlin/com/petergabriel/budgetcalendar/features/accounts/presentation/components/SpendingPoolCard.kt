package com.petergabriel.budgetcalendar.features.accounts.presentation.components

import androidx.compose.foundation.background
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
fun SpendingPoolCard(
    account: Account,
    balance: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing
    val radius = BudgetCalendarTheme.radius
    val description = account.description?.takeIf { it.isNotBlank() } ?: account.name

    Column(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = spacing.spacing12)
            .clip(RoundedCornerShape(radius.xl))
            .background(colors.bgDark)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = "${account.name}, ${account.type.name}, ${CurrencyUtils.formatCents(balance)}, Safe to Spend included"
            }
            .padding(spacing.spacing4),
        verticalArrangement = Arrangement.spacedBy(spacing.spacing3),
    ) {
        Text(
            text = account.type.name.replace('_', ' '),
            style = typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            modifier = Modifier
                .clip(RoundedCornerShape(radius.md))
                .background(colors.bgPrimary)
                .padding(horizontal = spacing.spacing3, vertical = spacing.spacing2),
        )
        Text(
            text = account.name,
            style = typography.cardTitle,
            fontWeight = FontWeight.SemiBold,
            color = colors.textInverted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = CurrencyUtils.formatCents(balance),
            style = typography.section,
            fontWeight = FontWeight.ExtraBold,
            color = colors.textInverted,
        )
        Text(
            text = description,
            style = typography.bodySmall,
            color = colors.textTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
