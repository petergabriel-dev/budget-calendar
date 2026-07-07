package com.petergabriel.budgetcalendar.features.accounts.presentation.components

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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

        ACCOUNT_TYPE_OPTIONS.forEach { (_, label) ->
            composeTestRule.onNodeWithText(label).assertIsDisplayed()
        }
    }

    @Test
    fun selectingAccountType_updatesSavedType_andCreditCardDisablesSpendingPoolToggle() {
        ACCOUNT_TYPE_OPTIONS.forEach { (expectedType, _) ->
            var savedType: AccountType? = null
            var savedInSpendingPool: Boolean? = null

            setFormContent(
                initialData = EDIT_ACCOUNT,
                onSave = { _, type, _, isInSpendingPool, _, _, _, _ ->
                    savedType = type
                    savedInSpendingPool = isInSpendingPool
                },
            )

            composeTestRule.onNodeWithTag(ACCOUNT_TYPE_FIELD_TAG).performClick()
            composeTestRule.onNodeWithTag(accountTypeOptionTag(expectedType)).performClick()

            if (expectedType == AccountType.CREDIT_CARD) {
                composeTestRule.onAllNodesWithTag(INCLUDE_IN_SPENDING_POOL_SWITCH_TAG).assertCountEquals(0)
            }

            composeTestRule.onNodeWithText("Save").performClick()

            composeTestRule.runOnIdle {
                assertEquals(expectedType, savedType)
                val expectedInSpendingPool = expectedType != AccountType.CREDIT_CARD
                assertEquals(expectedInSpendingPool, savedInSpendingPool)
            }
        }
    }

    @Test
    fun creatingCreditCard_requiresInlineSetupFieldsAndSavesOwedBalance() {
        var savedType: AccountType? = null
        var savedBalance: Long? = null
        var savedInSpendingPool: Boolean? = null
        var savedCreditLimit: Long? = null
        var savedClosingDay: Int? = null
        var savedDueDay: Int? = null

        setFormContent(
            initialData = null,
            onSave = { _, type, balance, isInSpendingPool, _, creditLimit, closingDay, dueDay ->
                savedType = type
                savedBalance = balance
                savedInSpendingPool = isInSpendingPool
                savedCreditLimit = creditLimit
                savedClosingDay = closingDay
                savedDueDay = dueDay
            },
        )

        composeTestRule.onNodeWithText("Name").performTextInput("Visa")
        composeTestRule.onNodeWithTag(ACCOUNT_TYPE_FIELD_TAG).performClick()
        composeTestRule.onNodeWithTag(accountTypeOptionTag(AccountType.CREDIT_CARD)).performClick()
        composeTestRule.onNodeWithText("Current balance owed").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag(INCLUDE_IN_SPENDING_POOL_SWITCH_TAG).assertCountEquals(0)

        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.onNodeWithText("Credit limit is required").assertIsDisplayed()

        composeTestRule.onNodeWithText("Current balance owed").performTextInput("123.45")
        composeTestRule.onNodeWithText("Credit Limit").performTextInput("1000")
        composeTestRule.onNodeWithText("Closing Day").performTextInput("20")
        composeTestRule.onNodeWithText("Due Day").performTextInput("5")
        composeTestRule.onNodeWithText("Save").performClick()

        composeTestRule.runOnIdle {
            assertEquals(AccountType.CREDIT_CARD, savedType)
            assertEquals(12_345L, savedBalance)
            assertEquals(false, savedInSpendingPool)
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
            isInSpendingPool: Boolean,
            description: String,
            creditLimit: Long?,
            statementClosingDay: Int?,
            paymentDueDay: Int?,
        ) -> Unit = { _, _, _, _, _, _, _, _ -> },
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
    isInSpendingPool = true,
    createdAt = 0L,
    updatedAt = 0L,
)

private const val ACCOUNT_TYPE_FIELD_TAG = "account_type_field"
private const val INCLUDE_IN_SPENDING_POOL_SWITCH_TAG = "include_in_spending_pool_switch"

private fun accountTypeOptionTag(type: AccountType): String = "account_type_option_${type.name}"
