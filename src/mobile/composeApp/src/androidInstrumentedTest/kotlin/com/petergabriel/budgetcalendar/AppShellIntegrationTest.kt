package com.petergabriel.budgetcalendar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppShellIntegrationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun shell_navigatesAllTabsAndKeepsSharedNavigationAvailable() {
        composeTestRule.onNodeWithContentDescription("Home").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Calendar").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Accounts").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Me").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Add").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Calendar").performClick()
        composeTestRule.onNodeWithContentDescription("Previous month").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Accounts").performClick()
        composeTestRule.onNodeWithText("TOTAL NET WORTH").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Me").performClick()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Home").performClick()
        composeTestRule.onNodeWithText("Live Budget").assertIsDisplayed()
    }
}
