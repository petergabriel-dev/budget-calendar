package com.petergabriel.budgetcalendar.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils
import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account
import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountPickerModal(
    isVisible: Boolean,
    accounts: List<Account>,
    selectedAccountId: Long?,
    title: String,
    onAccountSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!isVisible) return

    val colors = BudgetCalendarTheme.colors
    val spacing = BudgetCalendarTheme.spacing
    val typography = BudgetCalendarTheme.typography

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = {},
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
        ) {
            BcHeader(
                title = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.spacing4, vertical = spacing.spacing2),
                actionIcon = Icons.AutoMirrored.Outlined.ArrowBack,
                actionContentDescription = "Back",
                onActionClick = onDismiss,
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                items(
                    items = accounts,
                    key = { account -> account.id },
                ) { account ->
                    val formattedBalance = CurrencyUtils.formatCents(account.balance)
                    val accountTypeLabel = account.type.badgeLabel()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = spacing.spacing12)
                            .clickable { onAccountSelected(account.id) }
                            .semantics(mergeDescendants = true) {
                                contentDescription = "${account.name}, $formattedBalance, $accountTypeLabel"
                            }
                            .padding(horizontal = spacing.spacing4, vertical = spacing.spacing3),
                        horizontalArrangement = Arrangement.spacedBy(spacing.spacing3),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(spacing.spacing1),
                        ) {
                            Text(
                                text = account.name,
                                style = typography.bodyLarge,
                                color = colors.textPrimary,
                            )
                            Text(
                                text = formattedBalance,
                                style = typography.bodySmall,
                                color = colors.textSecondary,
                            )
                        }

                        BcBadge(
                            text = accountTypeLabel,
                            variant = BadgeVariant.Pending,
                        )

                        if (account.id == selectedAccountId) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = "Selected",
                                tint = colors.colorSuccess,
                                modifier = Modifier.size(spacing.spacing6),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun AccountType.badgeLabel(): String {
    return when (this) {
        AccountType.CHECKING -> "Checking"
        AccountType.SAVINGS -> "Savings"
        AccountType.CREDIT_CARD -> "Credit Card"
        AccountType.CASH -> "Cash"
        AccountType.INVESTMENT -> "Investment"
    }
}
