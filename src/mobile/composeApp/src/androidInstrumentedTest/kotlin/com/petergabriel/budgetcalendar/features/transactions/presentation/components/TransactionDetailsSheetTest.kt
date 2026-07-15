package com.petergabriel.budgetcalendar.features.transactions.presentation.components

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionDetailsSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun expenseDetails_showRequiredFieldsAndAccessibleActions() {
        setDetailsContent(transaction = EXPENSE)

        composeTestRule.onNodeWithText("Transaction Details").assertIsDisplayed()
        composeTestRule.onNodeWithText("-₱45.67").assertIsDisplayed()
        composeTestRule.onNodeWithText("Expense").assertIsDisplayed()
        composeTestRule.onNodeWithText("Groceries").assertIsDisplayed()
        composeTestRule.onNodeWithText("2026-07-15").assertIsDisplayed()
        composeTestRule.onNodeWithText("CONFIRMED").assertIsDisplayed()
        composeTestRule.onNodeWithText("Savings").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("DESTINATION ACCOUNT").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("DESCRIPTION").assertCountEquals(0)
        composeTestRule.onNodeWithContentDescription("Close transaction details").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Transaction actions").assertIsDisplayed()
    }

    @Test
    fun transferDetails_showDestinationAndDescription() {
        setDetailsContent(
            transaction = TRANSFER,
            destinationAccountName = "MariBank",
        )

        composeTestRule.onNodeWithText("₱75.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("DESTINATION ACCOUNT").assertIsDisplayed()
        composeTestRule.onNodeWithText("MariBank").assertIsDisplayed()
        composeTestRule.onNodeWithText("DESCRIPTION").assertIsDisplayed()
        composeTestRule.onNodeWithText("Credit card payment").assertIsDisplayed()
    }

    @Test
    fun overflowEdit_callsEditCallback() {
        var editedTransaction: Transaction? = null
        setDetailsContent(onEdit = { editedTransaction = it })

        composeTestRule.onNodeWithContentDescription("Transaction actions").performClick()
        composeTestRule.onNodeWithText("Edit").performClick()

        composeTestRule.runOnIdle {
            assertEquals(EXPENSE, editedTransaction)
        }
    }

    @Test
    fun deleteCancelDoesNotDelete_andConfirmDeletesOnce() {
        var deletedTransaction: Transaction? = null
        var deleteCalls = 0
        setDetailsContent(
            onDelete = {
                deletedTransaction = it
                deleteCalls += 1
            },
        )

        composeTestRule.onNodeWithContentDescription("Transaction actions").performClick()
        composeTestRule.onNodeWithText("Delete").performClick()
        composeTestRule.onNodeWithText("Delete transaction?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").performClick()

        composeTestRule.runOnIdle {
            assertNull(deletedTransaction)
            assertEquals(0, deleteCalls)
        }

        composeTestRule.onNodeWithContentDescription("Transaction actions").performClick()
        composeTestRule.onNodeWithText("Delete").performClick()
        composeTestRule.onNodeWithText("Delete").performClick()

        composeTestRule.runOnIdle {
            assertEquals(EXPENSE, deletedTransaction)
            assertEquals(1, deleteCalls)
        }
    }

    private fun setDetailsContent(
        transaction: Transaction = EXPENSE,
        destinationAccountName: String? = null,
        onEdit: (Transaction) -> Unit = {},
        onDelete: (Transaction) -> Unit = {},
    ) {
        composeTestRule.setContent {
            BudgetCalendarTheme {
                TransactionDetailsSheet(
                    isVisible = true,
                    transaction = transaction,
                    accountName = "Savings",
                    destinationAccountName = destinationAccountName,
                    onDismiss = {},
                    onEdit = onEdit,
                    onDelete = onDelete,
                )
            }
        }
    }
}

private val TRANSACTION_DATE = LocalDate(2026, 7, 15)
    .atStartOfDayIn(TimeZone.currentSystemDefault())
    .toEpochMilliseconds()

private val EXPENSE = Transaction(
    id = 1L,
    accountId = 10L,
    destinationAccountId = null,
    amount = 4_567L,
    signedAmount = -4_567L,
    date = TRANSACTION_DATE,
    type = TransactionType.EXPENSE,
    status = TransactionStatus.CONFIRMED,
    description = null,
    category = "Groceries",
    linkedTransactionId = null,
    createdAt = 0L,
    updatedAt = 0L,
)

private val TRANSFER = Transaction(
    id = 2L,
    accountId = 10L,
    destinationAccountId = 20L,
    amount = 7_500L,
    signedAmount = -7_500L,
    date = TRANSACTION_DATE,
    type = TransactionType.TRANSFER,
    status = TransactionStatus.CONFIRMED,
    description = "Credit card payment",
    category = null,
    linkedTransactionId = 3L,
    createdAt = 0L,
    updatedAt = 0L,
)
