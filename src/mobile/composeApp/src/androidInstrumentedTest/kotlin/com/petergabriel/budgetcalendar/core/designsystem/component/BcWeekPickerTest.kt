package com.petergabriel.budgetcalendar.core.designsystem.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BcWeekPickerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun weekDaysExposeSelectionAndInvokeDateId() {
        var selectedId: String? = null
        val days = listOf(
            BcWeekDay("monday", "MON", 10, "Monday, August 10, selected, 1 transaction", true),
            BcWeekDay("tuesday", "TUE", 11, "Tuesday, August 11, 0 transactions", false),
        )

        composeTestRule.setContent {
            BudgetCalendarTheme {
                BcWeekPicker(days = days, onDaySelected = { selectedId = it })
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Monday, August 10, selected, 1 transaction")
            .assertIsDisplayed()
            .assertIsSelected()
        composeTestRule
            .onNodeWithContentDescription("Tuesday, August 11, 0 transactions")
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals("tuesday", selectedId)
        }
    }
}
