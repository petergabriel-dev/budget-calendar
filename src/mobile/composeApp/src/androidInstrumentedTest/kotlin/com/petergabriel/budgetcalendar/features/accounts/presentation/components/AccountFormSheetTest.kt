package com.petergabriel.budgetcalendar.features.accounts.presentation.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
                onSave = { _, type, _, isInSpendingPool, _ ->
                    savedType = type
                    savedInSpendingPool = isInSpendingPool
                },
            )

            composeTestRule.onNodeWithTag(ACCOUNT_TYPE_FIELD_TAG).performClick()
            composeTestRule.onNodeWithTag(accountTypeOptionTag(expectedType)).performClick()

            if (expectedType == AccountType.CREDIT_CARD) {
                composeTestRule.onNodeWithTag(INCLUDE_IN_SPENDING_POOL_SWITCH_TAG).assertIsNotEnabled()
            }

            composeTestRule.onNodeWithText("Save").performClick()

            composeTestRule.runOnIdle {
                assertEquals(expectedType, savedType)
                val expectedInSpendingPool = expectedType != AccountType.CREDIT_CARD
                assertEquals(expectedInSpendingPool, savedInSpendingPool)
            }
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
        ) -> Unit = { _, _, _, _, _ -> },
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
