package com.petergabriel.budgetcalendar.core.designsystem.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account
import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountPickerModalTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun picker_rowsExposeContextAndSelection() {
        var selectedId: Long? = null
        composeTestRule.setContent {
            BudgetCalendarTheme {
                AccountPickerModal(
                    isVisible = true,
                    accounts = listOf(SAVINGS),
                    selectedAccountId = null,
                    title = "Select Account",
                    onAccountSelected = { selectedId = it },
                    onDismiss = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Select Account").assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Savings, ₱100.00, Savings")
            .performClick()
        composeTestRule.runOnIdle {
            assertEquals(SAVINGS.id, selectedId)
        }
    }

    @Test
    fun picker_emptyStateExplainsMissingAccount() {
        composeTestRule.setContent {
            BudgetCalendarTheme {
                AccountPickerModal(
                    isVisible = true,
                    accounts = emptyList(),
                    selectedAccountId = null,
                    title = "Select Destination",
                    onAccountSelected = {},
                    onDismiss = {},
                )
            }
        }

        composeTestRule
            .onNodeWithText("No accounts available. Add an account first.")
            .assertIsDisplayed()
    }
}

private val SAVINGS = Account(
    id = 2L,
    name = "Savings",
    type = AccountType.SAVINGS,
    balance = 10_000L,
    isInSpendingPool = true,
    createdAt = 0L,
    updatedAt = 0L,
)
