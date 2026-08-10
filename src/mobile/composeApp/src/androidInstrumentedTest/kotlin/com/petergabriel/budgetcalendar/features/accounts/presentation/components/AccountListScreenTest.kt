package com.petergabriel.budgetcalendar.features.accounts.presentation.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account
import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType
import com.petergabriel.budgetcalendar.features.creditcard.domain.model.CreditCardSummary
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun populatedAccountList_showsStackedSectionsAndUtilizationText() {
        composeTestRule.setContent {
            BudgetCalendarTheme {
                AccountListScreen(
                    accounts = listOf(account, savings),
                    creditCards = listOf(card),
                    balances = mapOf(account.id to account.balance, savings.id to savings.balance),
                    netWorth = 45_000L,
                    onAddAccount = {},
                    onAccountTap = {},
                    onCreditCardTap = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Spending Pool").assertIsDisplayed()
        composeTestRule.onNodeWithText("Savings & Investments").assertIsDisplayed()
        composeTestRule.onNodeWithText("Credit Cards").assertIsDisplayed()
        composeTestRule.onNodeWithText("Utilization 50%").assertIsDisplayed()
    }

    @Test
    fun emptyAccountList_showsAddFirstAccountState() {
        composeTestRule.setContent {
            BudgetCalendarTheme {
                AccountListScreen(
                    accounts = emptyList(),
                    creditCards = emptyList(),
                    balances = emptyMap(),
                    netWorth = null,
                    isNetWorthLoading = false,
                    onAddAccount = {},
                    onAccountTap = {},
                    onCreditCardTap = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Add your first account").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add account").assertIsDisplayed()
    }

    @Test
    fun creditCardError_keepsOrdinaryAccountsAndRetriesScopedArea() {
        var retryCount = 0
        composeTestRule.setContent {
            BudgetCalendarTheme {
                AccountListScreen(
                    accounts = listOf(account),
                    creditCards = emptyList(),
                    balances = mapOf(account.id to account.balance),
                    netWorth = 45_000L,
                    creditCardsError = "card load failed",
                    onRetryCreditCards = { retryCount++ },
                    onAddAccount = {},
                    onAccountTap = {},
                    onCreditCardTap = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Primary").assertIsDisplayed()
        composeTestRule.onNodeWithText("card load failed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").performClick()
        composeTestRule.runOnIdle { assertEquals(1, retryCount) }
    }

    private companion object {
        val account = Account(
            id = 1L,
            name = "Primary",
            type = AccountType.CHECKING,
            balance = 45_000L,
            isInSpendingPool = true,
            createdAt = 0L,
            updatedAt = 0L,
        )
        val savings = Account(
            id = 3L,
            name = "Emergency",
            type = AccountType.SAVINGS,
            balance = 12_000L,
            isInSpendingPool = false,
            createdAt = 0L,
            updatedAt = 0L,
        )
        val card = CreditCardSummary(
            accountId = 2L,
            accountName = "Visa",
            currentBalance = -10_000L,
            reservedAmount = 10_000L,
            statementBalance = null,
            creditLimit = 20_000L,
            availableCredit = 10_000L,
            dueDate = null,
            statementClosingDay = 20,
            paymentDueDay = 5,
        )
    }
}
