package com.petergabriel.budgetcalendar.features.sandbox.presentation.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxSnapshot
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxTransaction
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SandboxComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun snapshotPill_emptyStateExposesSelectionAction() {
        composeTestRule.setContent {
            BudgetCalendarTheme {
                SnapshotSelectorPill(
                    activeSnapshot = null,
                    onTap = {},
                )
            }
        }

        composeTestRule.onNodeWithText("No Snapshot Selected").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Select sandbox snapshot, No Snapshot Selected").assertIsDisplayed()
    }

    @Test
    fun snapshotSheet_emptyStateAndHeadingUseSharedShell() {
        composeTestRule.setContent {
            BudgetCalendarTheme {
                SnapshotSelectorSheet(
                    isVisible = true,
                    snapshots = emptyList(),
                    activeSnapshotId = null,
                    onSelect = {},
                    onDelete = {},
                    onCreateNew = {},
                    onDismiss = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Select Sandbox").assertIsDisplayed()
        composeTestRule.onNodeWithText("No snapshots yet.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Create New").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Close").assertIsDisplayed()
    }

    @Test
    fun transactionRow_requiresPromoteConfirmation_andRemoveConfirmation() {
        var promoted = false
        var removed = false
        composeTestRule.setContent {
            BudgetCalendarTheme {
                SandboxTransactionItem(
                    transaction = transaction,
                    onPromote = { promoted = true },
                    onRemove = { removed = true },
                )
            }
        }

        composeTestRule.onNodeWithText("Promote").performClick()
        composeTestRule.onNodeWithText("This adds the transaction to real data and removes it from sandbox.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.runOnIdle { assertFalse(promoted) }

        composeTestRule.onNodeWithText("Remove").performClick()
        composeTestRule.onNodeWithText("This removes this transaction from the sandbox. This action cannot be undone.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.runOnIdle { assertFalse(removed) }
    }

    @Test
    fun promotedTransaction_hidesPromoteAndShowsPromotedState() {
        composeTestRule.setContent {
            BudgetCalendarTheme {
                SandboxTransactionItem(
                    transaction = transaction.copy(originalTransactionId = 99L),
                    onPromote = { error("promote must be hidden") },
                    onRemove = {},
                )
            }
        }

        composeTestRule.onNodeWithText("PROMOTED").assertIsDisplayed()
        composeTestRule.onNodeWithText("Promote").assertDoesNotExist()
        composeTestRule.runOnIdle { assertTrue(true) }
    }

    private companion object {
        val transaction = SandboxTransaction(
            id = 1L,
            snapshotId = 1L,
            accountId = 1L,
            amount = 2_500L,
            signedAmount = -2_500L,
            date = 1L,
            type = TransactionType.EXPENSE,
            status = TransactionStatus.PENDING,
            description = "Weekend trip",
            category = "Travel",
            originalTransactionId = null,
            createdAt = 1L,
            updatedAt = 1L,
        )
    }
}
