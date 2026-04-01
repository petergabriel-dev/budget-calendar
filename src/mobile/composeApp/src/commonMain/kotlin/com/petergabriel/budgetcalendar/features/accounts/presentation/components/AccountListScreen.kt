package com.petergabriel.budgetcalendar.features.accounts.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petergabriel.budgetcalendar.core.designsystem.component.HeroNetWorth
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account
import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType
import com.petergabriel.budgetcalendar.features.creditcard.domain.model.CreditCardSummary

@Composable
fun AccountListScreen(
    accounts: List<Account>,
    creditCards: List<CreditCardSummary>,
    balances: Map<Long, Long>,
    netWorth: Long,
    modifier: Modifier = Modifier,
    onAccountTap: (Account) -> Unit,
    onCreditCardTap: (CreditCardSummary) -> Unit,
) {
    val spacing = BudgetCalendarTheme.spacing
    val spendingPool = accounts.filter { account -> account.isInSpendingPool }
    val wallGarden = accounts.filter { account -> !account.isInSpendingPool }

    val spendingPoolTotal = spendingPool.sumOf { account -> balances[account.id] ?: account.balance }
    val wallGardenTotal = wallGarden.sumOf { account -> balances[account.id] ?: account.balance }
    val creditCardsTotal = creditCards.sumOf { summary -> summary.currentBalance }

    if (accounts.isEmpty() && creditCards.isEmpty()) {
        EmptyState(
            netWorth = netWorth,
            modifier = modifier.padding(horizontal = spacing.spacing2),
        )
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = spacing.spacing6),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            NetWorthHero(netWorth = netWorth)
        }

        if (spendingPool.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SectionHeader(
                        title = "Spending Pool",
                        total = spendingPoolTotal,
                    )

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
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SectionHeader(
                        title = "Savings & Investments",
                        total = wallGardenTotal,
                    )

                    SavingsAndInvestmentsGrid(
                        accounts = wallGarden,
                        balances = balances,
                        onAccountTap = onAccountTap,
                    )
                }
            }
        }

        if (creditCards.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SectionHeader(
                        title = "Credit Cards",
                        total = creditCardsTotal,
                    )

                    creditCards.forEach { summary ->
                        val account = Account(
                            id = summary.accountId,
                            name = summary.accountName,
                            type = AccountType.CREDIT_CARD,
                            balance = summary.currentBalance,
                            isInSpendingPool = false,
                            createdAt = 0L,
                            updatedAt = 0L,
                            description = summary.accountName,
                        )
                        WalledGardenCard(
                            account = account,
                            balance = summary.currentBalance,
                            isLiability = true,
                            onClick = { onCreditCardTap(summary) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    total: Long,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = title,
            style = typography.section.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                letterSpacing = (-0.5).sp,
            ),
            color = colors.textPrimary,
        )

        Text(
            text = total.formatCurrency(),
            style = typography.bodyMedium.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun NetWorthHero(netWorth: Long) {
    val spacing = BudgetCalendarTheme.spacing
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = spacing.spacing1),
    ) {
        Text(
            text = "TOTAL NET WORTH",
            style = typography.bodyMedium.copy(
                fontFamily = typography.headline.fontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
            ),
            color = colors.textSecondary,
        )
        HeroNetWorth(amount = netWorth)
    }
}

@Composable
private fun EmptyState(
    netWorth: Long,
    modifier: Modifier = Modifier,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing

    Column(
        modifier = modifier.padding(horizontal = spacing.spacing6, vertical = spacing.spacing6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
    ) {
        NetWorthHero(netWorth = netWorth)

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
    }
}

@Composable
private fun SavingsAndInvestmentsGrid(
    accounts: List<Account>,
    balances: Map<Long, Long>,
    onAccountTap: (Account) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth < 600.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                accounts.forEach { account ->
                    WalledGardenCard(
                        account = account,
                        balance = balances[account.id] ?: account.balance,
                        onClick = { onAccountTap(account) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                accounts.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        pair.forEach { account ->
                            WalledGardenCard(
                                account = account,
                                balance = balances[account.id] ?: account.balance,
                                onClick = { onAccountTap(account) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
