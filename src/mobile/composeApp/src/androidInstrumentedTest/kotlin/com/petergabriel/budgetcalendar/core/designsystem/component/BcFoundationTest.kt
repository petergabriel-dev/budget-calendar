package com.petergabriel.budgetcalendar.core.designsystem.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BcFoundationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun metricHero_exposesMetricAndWarningSemantics() {
        composeTestRule.setContent {
            BudgetCalendarTheme {
                BcMetricHero(
                    label = "SAFE TO SPEND",
                    amount = 12_500L,
                    supportingText = "₱125.00 / day · 1 day left",
                    warningMessage = "Last day of month",
                )
            }
        }

        composeTestRule.onNodeWithText("SAFE TO SPEND").assertIsDisplayed()
        composeTestRule.onNodeWithText("₱125.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("Last day of month").assertIsDisplayed()
    }

    @Test
    fun sheetShell_exposesHeadingCloseAndStickyFooter() {
        var dismissed = false
        composeTestRule.setContent {
            BudgetCalendarTheme {
                BcSheetShell(
                    title = "Transaction details",
                    onDismissRequest = { dismissed = true },
                    body = { androidx.compose.material3.Text("Scrollable body") },
                    footer = {
                        BcSheetFooter {
                            BcButton(
                                text = "Save",
                                onClick = {},
                                modifier = androidx.compose.ui.Modifier.weight(1f),
                            )
                        }
                    },
                )
            }
        }

        composeTestRule.onNodeWithText("Transaction details").assertIsDisplayed()
        composeTestRule.onNodeWithText("Scrollable body").assertIsDisplayed()
        composeTestRule.onNodeWithText("Save").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Close").performClick()

        composeTestRule.runOnIdle {
            assertEquals(true, dismissed)
        }
    }

    @Test
    fun confirmDialog_cancelIsSafeDefault() {
        var confirmed = false
        composeTestRule.setContent {
            BudgetCalendarTheme {
                BcConfirmDialog(
                    title = "Delete transaction?",
                    message = "This action cannot be undone.",
                    onDismissRequest = {},
                    onConfirm = { confirmed = true },
                    confirmLabel = "Delete",
                )
            }
        }

        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.runOnIdle {
            assertFalse(confirmed)
        }
    }

    @Test
    fun confirmDialog_busyDisablesMutation() {
        var confirmed = false
        composeTestRule.setContent {
            BudgetCalendarTheme {
                BcConfirmDialog(
                    title = "Delete transaction?",
                    message = "Saving…",
                    onDismissRequest = {},
                    onConfirm = { confirmed = true },
                    isBusy = true,
                )
            }
        }

        composeTestRule.onNodeWithText("Working…").assertIsDisplayed()
        composeTestRule.runOnIdle {
            assertFalse(confirmed)
        }
    }
}
