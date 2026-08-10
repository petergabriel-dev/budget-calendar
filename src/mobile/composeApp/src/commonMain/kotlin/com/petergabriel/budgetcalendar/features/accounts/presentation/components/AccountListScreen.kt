package com.petergabriel.budgetcalendar.features.accounts.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.petergabriel.budgetcalendar.core.designsystem.component.BcButton
import com.petergabriel.budgetcalendar.core.designsystem.component.ButtonVariant
import com.petergabriel.budgetcalendar.core.designsystem.component.HeroNetWorth
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account
import com.petergabriel.budgetcalendar.features.creditcard.domain.model.CreditCardSummary

@Composable
fun AccountListScreen(
    accounts: List<Account>,
    creditCards: List<CreditCardSummary>,
    balances: Map<Long, Long>,
    netWorth: Long?,
    isNetWorthLoading: Boolean = false,
    netWorthError: String? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    isCreditCardsLoading: Boolean = false,
    creditCardsError: String? = null,
    modifier: Modifier = Modifier,
    onRetryAccounts: () -> Unit = {},
    onRetryCreditCards: () -> Unit = {},
    onAddAccount: () -> Unit,
    onAccountTap: (Account) -> Unit,
    onCreditCardTap: (CreditCardSummary) -> Unit,
) {
    val spacing = BudgetCalendarTheme.spacing
    val spendingPool = accounts.filter(Account::isInSpendingPool)
    val wallGarden = accounts.filterNot(Account::isInSpendingPool)
    val spendingPoolTotal = spendingPool.sumOf { account -> balances[account.id] ?: account.balance }
    val wallGardenTotal = wallGarden.sumOf { account -> balances[account.id] ?: account.balance }
    val creditCardsTotal = creditCards.sumOf(CreditCardSummary::reservedAmount)
    val hasNoData = accounts.isEmpty() && creditCards.isEmpty()
    val showEmptyState = hasNoData &&
        !isLoading &&
        !isCreditCardsLoading &&
        errorMessage == null &&
        creditCardsError == null

    if (showEmptyState) {
        EmptyState(
            netWorth = netWorth,
            isNetWorthLoading = isNetWorthLoading,
            netWorthError = netWorthError,
            onAddAccount = onAddAccount,
            modifier = modifier.padding(horizontal = spacing.spacing2),
        )
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = spacing.spacing6),
        verticalArrangement = Arrangement.spacedBy(spacing.spacing4),
    ) {
        item {
            NetWorthHero(
                netWorth = netWorth,
                isLoading = isNetWorthLoading,
                errorMessage = netWorthError,
            )
        }

        errorMessage?.let { error ->
            item {
                ScopedError(
                    message = error,
                    retryContentDescription = "Retry loading accounts",
                    onRetry = onRetryAccounts,
                )
            }
        }

        if (isLoading && accounts.isEmpty()) {
            item { AccountSkeleton(label = "Loading accounts…") }
        }

        if (spendingPool.isNotEmpty()) {
            item {
                AccountSection(title = "Spending Pool", total = spendingPoolTotal) {
                    spendingPool.forEach { account ->
                        SpendingPoolCard(
                            account = account,
                            balance = balances[account.id] ?: account.balance,
                            onClick = { onAccountTap(account) },
                        )
                    }
                }
            }
        }

        if (wallGarden.isNotEmpty()) {
            item {
                AccountSection(title = "Savings & Investments", total = wallGardenTotal) {
                    SavingsAndInvestmentsList(
                        accounts = wallGarden,
                        balances = balances,
                        onAccountTap = onAccountTap,
                    )
                }
            }
        }

        if (creditCardsError != null) {
            item {
                ScopedError(
                    message = creditCardsError,
                    retryContentDescription = "Retry loading credit cards",
                    onRetry = onRetryCreditCards,
                )
            }
        }

        if (isCreditCardsLoading && creditCards.isEmpty()) {
            item { AccountSkeleton(label = "Loading credit cards…") }
        }

        if (creditCards.isNotEmpty()) {
            item {
                AccountSection(title = "Credit Cards", total = creditCardsTotal) {
                    creditCards.forEach { summary ->
                        CreditCardTile(
                            summary = summary,
                            onClick = { onCreditCardTap(summary) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        if (hasNoData && (isLoading || isCreditCardsLoading || errorMessage != null || creditCardsError != null)) {
            item {
                Text(
                    text = "No account data available yet.",
                    style = BudgetCalendarTheme.typography.bodyMedium,
                    color = BudgetCalendarTheme.colors.textSecondary,
                )
            }
        }

        item {
            BcButton(
                text = "Add account",
                onClick = onAddAccount,
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.PrimaryIconLeading,
            )
        }
    }
}

@Composable
private fun AccountSection(
    title: String,
    total: Long,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.spacing4)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = title,
                style = typography.section,
                fontWeight = FontWeight.ExtraBold,
                color = colors.textPrimary,
            )
            Text(
                text = total.formatCurrency(),
                style = typography.bodyMedium,
                color = colors.textSecondary,
            )
        }
        content()
    }
}

@Composable
private fun NetWorthHero(
    netWorth: Long?,
    isLoading: Boolean,
    errorMessage: String?,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = spacing.spacing1),
        verticalArrangement = Arrangement.spacedBy(spacing.spacing1),
        horizontalAlignment = horizontalAlignment,
    ) {
        Text(
            text = "TOTAL NET WORTH",
            style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textSecondary,
        )
        if (isLoading && netWorth == null) {
            Box(
                modifier = Modifier
                    .defaultMinSize(minHeight = spacing.spacing8)
                    .fillMaxWidth()
                    .background(colors.bgSurface)
                    .semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate },
            )
        } else if (netWorth != null) {
            HeroNetWorth(amount = netWorth)
        } else {
            Text(
                text = "Not available",
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = typography.bodyLarge,
                color = colors.textSecondary,
            )
        }
        errorMessage?.let { error ->
            Text(
                text = error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = typography.bodySmall,
                color = colors.colorErrorText,
            )
        }
    }
}

@Composable
private fun EmptyState(
    netWorth: Long?,
    isNetWorthLoading: Boolean,
    netWorthError: String?,
    onAddAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.spacing6, vertical = spacing.spacing6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
    ) {
        NetWorthHero(
            netWorth = netWorth,
            isLoading = isNetWorthLoading,
            errorMessage = netWorthError,
            horizontalAlignment = Alignment.CenterHorizontally,
        )
        Text(
            text = "Add your first account",
            style = typography.section,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
        )
        Text(
            text = "Start by adding where your money lives.",
            style = typography.bodyMedium,
            color = colors.textSecondary,
        )
        BcButton(
            text = "Add account",
            onClick = onAddAccount,
            variant = ButtonVariant.PrimaryIconLeading,
        )
    }
}

@Composable
private fun ScopedError(
    message: String,
    retryContentDescription: String,
    onRetry: () -> Unit,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.spacing2)) {
        Text(
            text = message,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = typography.bodySmall,
            color = colors.colorErrorText,
        )
        BcButton(
            text = "Retry",
            contentDescription = retryContentDescription,
            onClick = onRetry,
            variant = ButtonVariant.Outline,
        )
    }
}

@Composable
private fun AccountSkeleton(label: String) {
    val colors = BudgetCalendarTheme.colors
    val spacing = BudgetCalendarTheme.spacing
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = spacing.spacing12)
            .background(colors.bgSurface)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                liveRegion = LiveRegionMode.Polite
            },
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(spacing.spacing4),
            style = BudgetCalendarTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun SavingsAndInvestmentsList(
    accounts: List<Account>,
    balances: Map<Long, Long>,
    onAccountTap: (Account) -> Unit,
) {
    val spacing = BudgetCalendarTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.spacing4)) {
        accounts.forEach { account ->
            WalledGardenCard(
                account = account,
                balance = balances[account.id] ?: account.balance,
                onClick = { onAccountTap(account) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
