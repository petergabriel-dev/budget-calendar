package com.petergabriel.budgetcalendar.features.notifications.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.petergabriel.budgetcalendar.core.designsystem.component.BcBadge
import com.petergabriel.budgetcalendar.core.designsystem.component.BadgeVariant
import com.petergabriel.budgetcalendar.core.designsystem.component.BcButton
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

@OptIn(ExperimentalMaterial3Api::class)
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

    val colors = BudgetCalendarTheme.colors
    val spacing = BudgetCalendarTheme.spacing
    val typography = BudgetCalendarTheme.typography

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val overdueTransactions by getOverdueTransactionsUseCase().collectAsStateWithLifecycle(emptyList())

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.spacing4, vertical = spacing.spacing3),
            verticalArrangement = Arrangement.spacedBy(spacing.spacing3),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Notifications",
                    style = typography.section,
                    color = colors.textPrimary,
                )

                if (uiState.notifications.isNotEmpty()) {
                    Box(
                        modifier = Modifier.semantics {
                            contentDescription = "Clear all notifications"
                        },
                    ) {
                        BcButton(
                            text = "Clear all",
                            onClick = viewModel::dismissAll,
                            variant = ButtonVariant.Ghost,
                        )
                    }
                }
            }

            if (uiState.notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = spacing.spacing6),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No new notifications",
                        style = typography.bodyMedium,
                        color = colors.textTertiary,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
                ) {
                    items(uiState.notifications, key = { notification -> notification.id }) { notification ->
                        NotificationAccordionItem(
                            notification = notification,
                            isExpanded = uiState.expandedNotificationId == notification.id,
                            overdueTransactions = overdueTransactions,
                            onToggleExpand = { viewModel.toggleExpand(notification.id) },
                            onDismiss = { viewModel.dismiss(notification.id) },
                        )
                    }
                }
            }

            if (!uiState.error.isNullOrBlank()) {
                Text(
                    text = uiState.error.orEmpty(),
                    style = typography.bodySmall,
                    color = colors.colorError,
                )
            }
        }
    }
}

@Composable
private fun NotificationAccordionItem(
    notification: Notification,
    isExpanded: Boolean,
    overdueTransactions: List<Transaction>,
    onToggleExpand: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = BudgetCalendarTheme.colors
    val spacing = BudgetCalendarTheme.spacing
    val typography = BudgetCalendarTheme.typography
    val radius = BudgetCalendarTheme.radius
    val minTouchTarget = spacing.spacing12 - spacing.spacing1

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
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onToggleExpand),
                verticalArrangement = Arrangement.spacedBy(spacing.spacing1),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.spacing2),
                ) {
                    Text(
                        text = titleFor(notification),
                        style = typography.bodyLarge,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )

                    if (notification.type == NotificationType.OVERDUE_TRANSACTIONS) {
                        val countText = (notification.overdueCount ?: 0).toString()
                        BcBadge(
                            text = countText,
                            variant = BadgeVariant.Overdue,
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
                onClick = onDismiss,
                modifier = Modifier
                    .size(minTouchTarget)
                    .semantics { contentDescription = "Dismiss notification" },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(spacing.spacing5),
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

                        Text(
                            text = CurrencyUtils.formatCents(
                                amountInCents = signedAmount(transaction),
                                includePlusSign = signedAmount(transaction) > 0L,
                            ),
                            style = typography.bodyMedium,
                            color = when (transaction.type) {
                                TransactionType.INCOME -> colors.colorSuccess
                                TransactionType.EXPENSE -> colors.colorError
                                TransactionType.TRANSFER -> colors.colorInfo
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
                    color = colors.colorWarning,
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
                    color = colors.colorError,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
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

private fun signedAmount(transaction: Transaction): Long {
    return transaction.signedAmount
}
