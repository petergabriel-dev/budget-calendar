package com.petergabriel.budgetcalendar.features.accounts.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.petergabriel.budgetcalendar.core.designsystem.component.BadgeVariant
import com.petergabriel.budgetcalendar.core.designsystem.component.BcBadge
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils
import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account
import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType

@Composable
fun AccountCard(
    account: Account,
    balance: Long,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing
    val radius = BudgetCalendarTheme.radius
    val isCreditCard = account.type == AccountType.CREDIT_CARD

    Card(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = spacing.spacing12)
            .clickable(onClick = onTap)
            .semantics {
                role = Role.Button
                contentDescription = "${account.name}, ${account.type.name}, ${CurrencyUtils.formatCents(balance)}"
            },
        shape = RoundedCornerShape(radius.xl),
        colors = CardDefaults.cardColors(
            containerColor = if (isCreditCard) colors.colorWarningBg else colors.bgSurface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.spacing4),
            verticalArrangement = Arrangement.spacedBy(spacing.spacing3),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.name,
                        style = typography.cardTitle,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = account.type.name.replace('_', ' '),
                        style = typography.bodySmall,
                        color = if (isCreditCard) colors.colorWarningText else colors.colorInfoText,
                    )
                }
                Text(
                    text = CurrencyUtils.formatCents(balance),
                    style = typography.cardTitle,
                    fontWeight = FontWeight.Bold,
                    color = if (isCreditCard) colors.colorErrorText else colors.textPrimary,
                )
            }
            BcBadge(
                text = if (account.isInSpendingPool) "Safe to Spend" else "Excluded",
                variant = if (account.isInSpendingPool) BadgeVariant.Success else BadgeVariant.Info,
            )
        }
    }
}
