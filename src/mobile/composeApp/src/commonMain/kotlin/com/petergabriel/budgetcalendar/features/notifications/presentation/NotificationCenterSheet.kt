package com.petergabriel.budgetcalendar.features.notifications.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.petergabriel.budgetcalendar.core.designsystem.component.BcBadge
import com.petergabriel.budgetcalendar.core.designsystem.component.BcButton
import com.petergabriel.budgetcalendar.core.designsystem.component.BcSheetShell
import com.petergabriel.budgetcalendar.core.designsystem.component.BadgeVariant
import com.petergabriel.budgetcalendar.core.designsystem.component.ButtonVariant
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils
import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.notifications.domain.model.Notification
import com.petergabriel.budgetcalendar.features.notifications.domain.model.NotificationType
import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import com.petergabriel.budgetcalendar.features.transactions.domain.usecase.GetOverdueTransactionsUseCase
import org.koin.compose.koinInject

@Composable
fun NotificationCenterSheet(
    isVisible: Boolean,
    viewModel: NotificationCenterViewModel,
    onDismiss: () -> Unit,
    getOverdueTransactionsUseCase: GetOverdueTransactionsUseCase = koinInject(),
) {
    if (!isVisible) {
        return
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val overdueTransactions by getOverdueTransactionsUseCase().collectAsStateWithLifecycle(emptyList())

    NotificationCenterSheetContent(
        uiState = uiState,
        overdueTransactions = overdueTransactions,
        onDismiss = onDismiss,
        onRetry = viewModel::retry,
        onToggleExpand = viewModel::toggleExpand,
        onDismissNotification = viewModel::dismiss,
        onDismissAll = viewModel::dismissAll,
    )
}

@Composable
fun NotificationCenterSheetContent(
    uiState: NotificationCenterUiState,
    overdueTransactions: List<Transaction>,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onToggleExpand: (Long) -> Unit,
    onDismissNotification: (Long) -> Unit,
    onDismissAll: () -> Unit,
) {
    val colors = BudgetCalendarTheme.colors
    val spacing = BudgetCalendarTheme.spacing
    val typography = BudgetCalendarTheme.typography

    BcSheetShell(
        title = "Notifications",
        onDismissRequest = onDismiss,
        bodyScrollable = false,
        headerContent = {
            if (uiState.notifications.isNotEmpty()) {
                BcButton(
                    text = if (uiState.isDismissingAll) "Clearing..." else "Clear all",
                    onClick = onDismissAll,
                    variant = ButtonVariant.Ghost,
                    enabled = !uiState.isDismissingAll && uiState.dismissingNotificationIds.isEmpty(),
                    contentDescription = "Clear all notifications",
                )
            }
        },
        body = {
            NotificationCenterBody(
                uiState = uiState,
                overdueTransactions = overdueTransactions,
                onRetry = onRetry,
                onToggleExpand = onToggleExpand,
                onDismissNotification = onDismissNotification,
                spacing = spacing,
            )
        },
    )
}

@Composable
private fun ColumnScope.NotificationCenterBody(
    uiState: NotificationCenterUiState,
    overdueTransactions: List<Transaction>,
    onRetry: () -> Unit,
    onToggleExpand: (Long) -> Unit,
    onDismissNotification: (Long) -> Unit,
    spacing: com.petergabriel.budgetcalendar.core.designsystem.theme.BcSpacing,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val hasLoadError = !uiState.loadError.isNullOrBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(spacing.spacing3),
    ) {
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Loading notifications...",
                        style = typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                }
            }

            uiState.notifications.isEmpty() && !hasLoadError -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No new notifications",
                        style = typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
                ) {
                    items(uiState.notifications, key = { notification -> notification.id }) { notification ->
                        NotificationAccordionItem(
                            notification = notification,
                            isExpanded = notification.id in uiState.expandedNotificationIds,
                            isBusy = notification.id in uiState.dismissingNotificationIds || uiState.isDismissingAll,
                            overdueTransactions = overdueTransactions,
                            onToggleExpand = { onToggleExpand(notification.id) },
                            onDismiss = { onDismissNotification(notification.id) },
                        )
                    }
                }
            }
        }

        if (hasLoadError) {
            FeedbackText(
                text = uiState.loadError.orEmpty(),
                color = colors.colorErrorText,
            )
            BcButton(
                text = "Retry",
                onClick = onRetry,
                variant = ButtonVariant.Outline,
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (!uiState.error.isNullOrBlank()) {
            FeedbackText(
                text = uiState.error.orEmpty(),
                color = colors.colorErrorText,
            )
        }
    }
}

@Composable
private fun NotificationAccordionItem(
    notification: Notification,
    isExpanded: Boolean,
    isBusy: Boolean,
    overdueTransactions: List<Transaction>,
    onToggleExpand: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = BudgetCalendarTheme.colors
    val spacing = BudgetCalendarTheme.spacing
    val typography = BudgetCalendarTheme.typography
    val radius = BudgetCalendarTheme.radius
    val title = titleFor(notification)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = colors.bgSurface,
                shape = RoundedCornerShape(radius.lg),
            )
            .padding(horizontal = spacing.spacing3, vertical = spacing.spacing2),
        verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.spacing2),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.spacing1),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.spacing2),
                ) {
                    Text(
                        text = title,
                        style = typography.bodyLarge,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )

                    if (notification.type == NotificationType.OVERDUE_TRANSACTIONS) {
                        val count = notification.overdueCount ?: 0
                        BcBadge(
                            text = count.toString(),
                            variant = BadgeVariant.Overdue,
                            modifier = Modifier.semantics {
                                contentDescription = "$count overdue transactions"
                            },
                        )
                    }
                }

                Text(
                    text = formatRelativeTimestamp(notification.createdAt),
                    style = typography.bodySmall,
                    color = colors.textTertiary,
                )
            }

            IconButton(
                onClick = onToggleExpand,
                modifier = Modifier
                    .size(spacing.spacing12)
                    .semantics {
                        contentDescription = if (isExpanded) "Collapse $title" else "Expand $title"
                        stateDescription = if (isExpanded) "Expanded" else "Collapsed"
                    },
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = colors.textPrimary,
                )
            }

            IconButton(
                onClick = onDismiss,
                enabled = !isBusy,
                modifier = Modifier
                    .size(spacing.spacing12)
                    .semantics {
                        contentDescription = "Dismiss $title"
                    },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = null,
                    tint = colors.textSecondary,
                )
            }
        }

        if (isExpanded) {
            HorizontalDivider(color = colors.borderSubtle)
            NotificationExpandedContent(
                notification = notification,
                overdueTransactions = overdueTransactions,
            )
        }
    }
}

@Composable
private fun NotificationExpandedContent(
    notification: Notification,
    overdueTransactions: List<Transaction>,
) {
    val colors = BudgetCalendarTheme.colors
    val spacing = BudgetCalendarTheme.spacing
    val typography = BudgetCalendarTheme.typography
    val radius = BudgetCalendarTheme.radius

    when (notification.type) {
        NotificationType.OVERDUE_TRANSACTIONS -> {
            if (overdueTransactions.isEmpty()) {
                Text(
                    text = "No overdue transactions",
                    style = typography.bodySmall,
                    color = colors.textSecondary,
                )
                return
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
            ) {
                overdueTransactions.forEach { transaction ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = transaction.description ?: transaction.category ?: "Transaction #${transaction.id}",
                            style = typography.bodyMedium,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f),
                        )

                        val signedAmount = transaction.signedAmount
                        Text(
                            text = CurrencyUtils.formatCents(
                                amountInCents = signedAmount,
                                includePlusSign = signedAmount > 0L,
                            ),
                            style = typography.bodyMedium,
                            color = when (transaction.type) {
                                TransactionType.INCOME -> colors.colorSuccessText
                                TransactionType.EXPENSE -> colors.colorErrorText
                                TransactionType.TRANSFER -> colors.colorInfoText
                            },
                        )
                    }
                }
            }
        }

        NotificationType.ROLLOVER_REMINDER_7,
        NotificationType.ROLLOVER_REMINDER_5,
        NotificationType.ROLLOVER_REMINDER_3,
        -> {
            Text(
                text = "Projected rollover: ${CurrencyUtils.formatCents(notification.projectedRollover ?: 0L)} · ${notification.daysRemaining ?: 0} days left",
                style = typography.bodyMedium,
                color = colors.textSecondary,
            )
        }

        NotificationType.STS_LOW -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = colors.colorWarningBg,
                        shape = RoundedCornerShape(radius.lg),
                    )
                    .padding(spacing.spacing3),
            ) {
                Text(
                    text = "${CurrencyUtils.formatCents(notification.currentSts ?: 0L)} remaining · ${lowPercent(notification)}% of pool",
                    style = typography.bodyMedium,
                    color = colors.colorWarningText,
                )
            }
        }

        NotificationType.STS_NEGATIVE -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = colors.colorErrorBg,
                        shape = RoundedCornerShape(radius.lg),
                    )
                    .padding(spacing.spacing3),
            ) {
                Text(
                    text = "${CurrencyUtils.formatCents(notification.currentSts ?: 0L)} below zero",
                    style = typography.bodyMedium,
                    color = colors.colorErrorText,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun FeedbackText(
    text: String,
    color: androidx.compose.ui.graphics.Color,
) {
    val typography = BudgetCalendarTheme.typography

    Text(
        text = text,
        style = typography.bodySmall,
        color = color,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

private fun titleFor(notification: Notification): String {
    return when (notification.type) {
        NotificationType.OVERDUE_TRANSACTIONS -> "Overdue transactions"
        NotificationType.ROLLOVER_REMINDER_7 -> "Rollover reminder (7 days)"
        NotificationType.ROLLOVER_REMINDER_5 -> "Rollover reminder (5 days)"
        NotificationType.ROLLOVER_REMINDER_3 -> "Rollover reminder (3 days)"
        NotificationType.STS_LOW -> "Safe to Spend is low"
        NotificationType.STS_NEGATIVE -> "Safe to Spend is negative"
    }
}

private fun formatRelativeTimestamp(createdAt: Long, nowMillis: Long = DateUtils.nowMillis()): String {
    val delta = (nowMillis - createdAt).coerceAtLeast(0L)
    val minuteMs = 60_000L
    val hourMs = 60L * minuteMs
    val dayMs = 24L * hourMs

    return when {
        delta < minuteMs -> "Just now"
        delta < hourMs -> "${delta / minuteMs}m ago"
        delta < dayMs -> "${delta / hourMs}h ago"
        else -> "${delta / dayMs}d ago"
    }
}

private fun lowPercent(notification: Notification): Int {
    val current = notification.currentSts ?: return 0
    val pool = notification.poolBalance ?: return 0
    if (pool <= 0L) {
        return 0
    }

    return ((current * 100L) / pool).toInt()
}
