package com.petergabriel.budgetcalendar.features.sandbox.presentation.components

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
class AddSandboxTransactionSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun transferValidation_staysInSheetAndNamesMissingDestination() {
        composeTestRule.setContent {
            BudgetCalendarTheme {
                AddSandboxTransactionSheet(
                    isVisible = true,
                    snapshotId = 1L,
                    accounts = listOf(account, secondAccount),
                    onAdd = {},
                    onDismiss = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Transfer").performClick()
        composeTestRule.onNodeWithText("Add Transfer").performClick()

        composeTestRule.onNodeWithText("Destination account is required").assertIsDisplayed()
    }

    @Test
    fun dirtyDismissal_requiresDiscardConfirmation() {
        var dismissed = false
        composeTestRule.setContent {
            BudgetCalendarTheme {
                AddSandboxTransactionSheet(
                    isVisible = true,
                    snapshotId = 1L,
                    accounts = listOf(account),
                    onAdd = {},
                    onDismiss = { dismissed = true },
                )
            }
        }

        composeTestRule.onNodeWithText("Transfer").performClick()
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.onNodeWithText("Keep editing").assertIsDisplayed()
        composeTestRule.onNodeWithText("Discard").performClick()
        composeTestRule.runOnIdle { assertEquals(true, dismissed) }
    }

    private companion object {
        val account = Account(
            id = 1L,
            name = "Primary",
            type = AccountType.CHECKING,
            balance = 10_000L,
            createdAt = 0L,
            updatedAt = 0L,
        )
        val secondAccount = account.copy(id = 2L, name = "Savings")
    }
}
