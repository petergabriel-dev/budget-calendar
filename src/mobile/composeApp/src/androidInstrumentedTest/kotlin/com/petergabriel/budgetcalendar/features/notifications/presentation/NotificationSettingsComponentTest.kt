package com.petergabriel.budgetcalendar.features.notifications.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationSettingsComponentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loadingState_keepsStructureAndDisablesSave() {
        composeTestRule.setContent {
            BudgetCalendarTheme {
                NotificationThresholdSetting(
                    value = "",
                    isLoading = true,
                    isSaving = false,
                    validationError = null,
                    saveError = null,
                    didSave = false,
                    onValueChange = {},
                    onSave = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Notification Preferences").assertIsDisplayed()
        composeTestRule.onNodeWithText("Loading...").assertIsDisplayed()
        composeTestRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun validationAndSuccessFeedback_areVisibleAndAnnounced() {
        composeTestRule.setContent {
            BudgetCalendarTheme {
                NotificationThresholdSetting(
                    value = "101",
                    isLoading = false,
                    isSaving = false,
                    validationError = "Please enter a threshold between 1 and 100",
                    saveError = null,
                    didSave = true,
                    onValueChange = {},
                    onSave = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Please enter a threshold between 1 and 100").assertIsDisplayed()
        composeTestRule.onNodeWithText("Threshold saved").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(
            "STS Low Alert Threshold percent, whole number, valid range 1 to 100",
        ).assertIsDisplayed()
    }

    @Test
    fun saveFailure_andRetry_stayInline() {
        var retryCount = 0
        composeTestRule.setContent {
            BudgetCalendarTheme {
                NotificationThresholdSetting(
                    value = "25",
                    isLoading = false,
                    isSaving = false,
                    validationError = null,
                    saveError = "settings save failed",
                    didSave = false,
                    loadError = "settings load failed",
                    onValueChange = {},
                    onSave = {},
                    onRetry = { retryCount++ },
                )
            }
        }

        composeTestRule.onNodeWithText("settings save failed").assertIsDisplayed()
        composeTestRule.onNodeWithText("settings load failed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").performClick()
        composeTestRule.runOnIdle { check(retryCount == 1) }
    }
}
