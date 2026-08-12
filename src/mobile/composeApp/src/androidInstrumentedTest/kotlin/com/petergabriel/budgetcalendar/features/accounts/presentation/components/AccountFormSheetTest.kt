package com.petergabriel.budgetcalendar.features.accounts.presentation.components

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account
import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountFormSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun typeDropdown_opensAndShowsAllFiveAccountTypes() {
        setFormContent(initialData = null)

        composeTestRule.onNodeWithTag(ACCOUNT_TYPE_FIELD_TAG).performClick()

        ACCOUNT_TYPE_OPTIONS.forEach { (type, _) ->
            composeTestRule.onNodeWithTag(accountTypeOptionTag(type)).assertIsDisplayed()
        }
    }

    @Test
    fun creatingCreditCard_requiresInlineSetupFieldsAndSavesOwedBalance() {
        var savedType: AccountType? = null
        var savedBalance: Long? = null
        var savedCreditLimit: Long? = null
        var savedClosingDay: Int? = null
        var savedDueDay: Int? = null

        setFormContent(
            initialData = null,
            onSave = { _, type, balance, _, creditLimit, closingDay, dueDay ->
                savedType = type
                savedBalance = balance
                savedCreditLimit = creditLimit
                savedClosingDay = closingDay
                savedDueDay = dueDay
            },
        )

        composeTestRule.onNodeWithTag(ACCOUNT_NAME_FIELD_TAG).performTextReplacement("Visa")
        composeTestRule.onNodeWithTag(ACCOUNT_TYPE_FIELD_TAG).performClick()
        composeTestRule.onNodeWithTag(accountTypeOptionTag(AccountType.CREDIT_CARD)).performClick()
        composeTestRule.onNodeWithText("Current balance owed").assertIsDisplayed()

        composeTestRule.onAllNodesWithText("Add Account")[1].performClick()
        composeTestRule.onNodeWithText("Credit limit is required").assertIsDisplayed()

        composeTestRule.onNodeWithTag(ACCOUNT_BALANCE_FIELD_TAG).performTextReplacement("123.45")
        composeTestRule.onNodeWithTag(ACCOUNT_CREDIT_LIMIT_FIELD_TAG).performTextReplacement("1000")
        composeTestRule.onNodeWithTag(ACCOUNT_CLOSING_DAY_FIELD_TAG).performTextReplacement("20")
        composeTestRule.onNodeWithTag(ACCOUNT_DUE_DAY_FIELD_TAG).performTextReplacement("5")
        composeTestRule.onAllNodesWithText("Add Account")[1].performClick()
        composeTestRule.onAllNodesWithText("Name is required").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Enter a valid amount").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Credit limit is required").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Closing day must be 1-31").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Due day must be 1-31").assertCountEquals(0)

        composeTestRule.runOnIdle {
            assertEquals(AccountType.CREDIT_CARD, savedType)
            assertEquals(12_345L, savedBalance)
            assertEquals(100_000L, savedCreditLimit)
            assertEquals(20, savedClosingDay)
            assertEquals(5, savedDueDay)
        }
    }

    private fun setFormContent(
        initialData: Account?,
        onSave: (
            name: String,
            type: AccountType,
            balance: Long,
            description: String,
            creditLimit: Long?,
            statementClosingDay: Int?,
            paymentDueDay: Int?,
        ) -> Unit = { _, _, _, _, _, _, _ -> },
    ) {
        composeTestRule.setContent {
            BudgetCalendarTheme {
                AccountFormSheet(
                    isVisible = true,
                    initialData = initialData,
                    isSubmitting = false,
                    onSave = onSave,
                    onCancel = {},
                )
            }
        }
    }
}

private val ACCOUNT_TYPE_OPTIONS = listOf(
    AccountType.CHECKING to "Checking",
    AccountType.SAVINGS to "Savings",
    AccountType.CREDIT_CARD to "Credit Card",
    AccountType.CASH to "Cash",
    AccountType.INVESTMENT to "Investment",
)

private val EDIT_ACCOUNT = Account(
    id = 1L,
    name = "Primary",
    type = AccountType.CHECKING,
    balance = 500_00L,
    createdAt = 0L,
    updatedAt = 0L,
)

private const val ACCOUNT_NAME_FIELD_TAG = "account_name_field"
private const val ACCOUNT_TYPE_FIELD_TAG = "account_type_field"
private const val ACCOUNT_BALANCE_FIELD_TAG = "account_balance_field"
private const val ACCOUNT_CREDIT_LIMIT_FIELD_TAG = "account_credit_limit_field"
private const val ACCOUNT_CLOSING_DAY_FIELD_TAG = "account_closing_day_field"
private const val ACCOUNT_DUE_DAY_FIELD_TAG = "account_due_day_field"

private fun accountTypeOptionTag(type: AccountType): String = "account_type_option_${type.name}"
