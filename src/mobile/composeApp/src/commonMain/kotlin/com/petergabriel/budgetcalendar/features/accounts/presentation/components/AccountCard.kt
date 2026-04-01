package com.petergabriel.budgetcalendar.features.accounts.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.petergabriel.budgetcalendar.core.designsystem.component.BadgeVariant
import com.petergabriel.budgetcalendar.core.designsystem.component.BcBadge
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils
import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account
import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountCard(
    account: Account,
    balance: Long,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing
    val radius = BudgetCalendarTheme.radius

    val isCreditCard = account.type == AccountType.CREDIT_CARD
    val containerColor = if (isCreditCard) {
        colors.colorWarningBg
    } else {
        colors.bgSurface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress,
            ),
        shape = RoundedCornerShape(radius.xl),
        colors = CardDefaults.cardColors(containerColor = containerColor),
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
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.name,
                        style = typography.cardTitle,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                    )

                    Text(
                        text = account.type.displayName(),
                        style = typography.bodySmall,
                        color = if (isCreditCard) colors.colorWarning else colors.colorInfo,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }

                Text(
                    text = CurrencyUtils.formatCents(balance),
                    style = typography.cardTitle,
                    fontWeight = FontWeight.Bold,
                    color = if (isCreditCard) colors.colorError else colors.textPrimary,
                )
            }

            SpendingPoolChip(isInSpendingPool = account.isInSpendingPool)
        }
    }
}

@Composable
private fun SpendingPoolChip(isInSpendingPool: Boolean) {
    BcBadge(
        text = if (isInSpendingPool) "Safe to Spend" else "Excluded",
        variant = if (isInSpendingPool) BadgeVariant.Success else BadgeVariant.Info,
    )
}

private fun AccountType.displayName(): String {
    return name
        .lowercase()
        .replace('_', ' ')
        .split(' ')
        .joinToString(" ") { chunk ->
            chunk.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase() else char.toString()
            }
        }
}
