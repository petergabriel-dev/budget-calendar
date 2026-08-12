package com.petergabriel.budgetcalendar.features.accounts.presentation.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account
import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType
import com.petergabriel.budgetcalendar.features.creditcard.domain.model.CreditCardSettings
import com.petergabriel.budgetcalendar.features.creditcard.domain.model.CreditCardSummary
import com.petergabriel.budgetcalendar.features.creditcard.presentation.components.CreditCardPaymentSheet
import com.petergabriel.budgetcalendar.features.creditcard.presentation.components.CreditCardSettingsForm
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountOverlaySheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun accountDetails_deleteRequiresConfirmationAndCancelKeepsSheet() {
        var deleted = false
        composeTestRule.setContent {
            BudgetCalendarTheme {
                AccountDetailsSheet(
                    isVisible = true,
                    account = account,
                    onEdit = {},
                    onDelete = { deleted = true },
                    onDismiss = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Delete").performClick()
        composeTestRule.onNodeWithText("Delete Primary?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Keep account").performClick()

        composeTestRule.runOnIdle { assertFalse(deleted) }
        composeTestRule.onNodeWithText("Primary").assertIsDisplayed()
    }

    @Test
    fun paymentSheet_failureMessageKeepsPaymentContext() {
        composeTestRule.setContent {
            BudgetCalendarTheme {
                CreditCardPaymentSheet(
                    isVisible = true,
                    ccSummary = cardSummary,
                    suggestedAmount = 5_000L,
                    spendingPoolAccounts = listOf(account),
                    errorMessage = "write failed",
                    onConfirmPayment = { _, _ -> },
                    onCancel = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Pay Visa").assertIsDisplayed()
        composeTestRule.onNodeWithText("write failed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Payment Amount").assertIsDisplayed()
    }

    @Test
    fun settingsSheet_failureMessageKeepsFields() {
        composeTestRule.setContent {
            BudgetCalendarTheme {
                CreditCardSettingsForm(
                    isVisible = true,
                    initialSettings = settings,
                    errorMessage = "write failed",
                    onSave = {},
                    onCancel = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Credit Card Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("write failed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Credit Limit").assertIsDisplayed()
        composeTestRule.onNodeWithText("Closing day (1-31)").assertIsDisplayed()
    }

    private companion object {
        val account = Account(
            id = 1L,
            name = "Primary",
            type = AccountType.CHECKING,
            balance = 25_000L,
            createdAt = 0L,
            updatedAt = 0L,
        )
        val cardSummary = CreditCardSummary(
            accountId = 2L,
            accountName = "Visa",
            currentBalance = -5_000L,
            reservedAmount = 5_000L,
            statementBalance = null,
            creditLimit = 20_000L,
            availableCredit = 15_000L,
            dueDate = null,
            statementClosingDay = 20,
            paymentDueDay = 5,
        )
        val settings = CreditCardSettings(
            id = 1L,
            accountId = 2L,
            creditLimit = 20_000L,
            statementBalance = null,
            dueDate = null,
            statementClosingDay = 20,
            paymentDueDay = 5,
            createdAt = 0L,
            updatedAt = 0L,
        )
    }
}
