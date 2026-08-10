package com.petergabriel.budgetcalendar.features.notifications.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.features.notifications.domain.model.Notification
import com.petergabriel.budgetcalendar.features.notifications.domain.model.NotificationType
import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationCenterSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyState_showsHeadingEmptyTextAndClose() {
        var dismissed = false
        composeTestRule.setContent {
            BudgetCalendarTheme {
                NotificationCenterSheetContent(
                    uiState = NotificationCenterUiState(isLoading = false),
                    overdueTransactions = emptyList(),
                    onDismiss = { dismissed = true },
                    onRetry = {},
                    onToggleExpand = {},
                    onDismissNotification = {},
                    onDismissAll = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Notifications").assertIsDisplayed()
        composeTestRule.onNodeWithText("No new notifications").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.runOnIdle { assertEquals(true, dismissed) }
    }

    @Test
    fun overdueRow_expandsIndependentlyAndShowsCount() {
        val overdue = notification(NotificationType.OVERDUE_TRANSACTIONS, 1L)
        composeTestRule.setContent {
            var expanded by remember { mutableStateOf(false) }
            BudgetCalendarTheme {
                NotificationCenterSheetContent(
                    uiState = NotificationCenterUiState(
                        isLoading = false,
                        notifications = listOf(overdue),
                        expandedNotificationIds = if (expanded) setOf(overdue.id) else emptySet(),
                    ),
                    overdueTransactions = listOf(overdueTransaction),
                    onDismiss = {},
                    onRetry = {},
                    onToggleExpand = { expanded = true },
                    onDismissNotification = {},
                    onDismissAll = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("2 overdue transactions").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Expand Overdue transactions").performClick()
        composeTestRule.onNodeWithText("Overdue coffee").assertIsDisplayed()
    }

    @Test
    fun dismissAndClearAll_actionsExposeNotificationNames() {
        val first = notification(NotificationType.STS_LOW, 1L)
        val second = notification(NotificationType.STS_NEGATIVE, 2L)
        var dismissedId = 0L
        var clearAllCount = 0
        composeTestRule.setContent {
            BudgetCalendarTheme {
                NotificationCenterSheetContent(
                    uiState = NotificationCenterUiState(
                        isLoading = false,
                        notifications = listOf(first, second),
                    ),
                    overdueTransactions = emptyList(),
                    onDismiss = {},
                    onRetry = {},
                    onToggleExpand = {},
                    onDismissNotification = { dismissedId = it },
                    onDismissAll = { clearAllCount++ },
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Dismiss Safe to Spend is low").performClick()
        composeTestRule.onNodeWithContentDescription("Clear all notifications").performClick()
        composeTestRule.runOnIdle {
            assertEquals(first.id, dismissedId)
            assertEquals(1, clearAllCount)
        }
    }

    private companion object {
        val overdueTransaction = Transaction(
            id = 10L,
            accountId = 1L,
            destinationAccountId = null,
            amount = 1_500L,
            signedAmount = -1_500L,
            date = 1L,
            type = TransactionType.EXPENSE,
            status = TransactionStatus.OVERDUE,
            description = "Overdue coffee",
            category = "Food",
            linkedTransactionId = null,
            createdAt = 1L,
            updatedAt = 1L,
        )

        fun notification(type: NotificationType, id: Long): Notification {
            return Notification(
                id = id,
                type = type,
                isDismissed = false,
                triggerMonth = null,
                triggerYear = null,
                overdueCount = if (type == NotificationType.OVERDUE_TRANSACTIONS) 2 else null,
                projectedRollover = null,
                daysRemaining = null,
                currentSts = if (type == NotificationType.STS_LOW) 10L else null,
                poolBalance = if (type == NotificationType.STS_LOW) 100L else null,
                createdAt = 1L,
                updatedAt = 1L,
            )
        }
    }
}
