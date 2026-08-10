package com.petergabriel.budgetcalendar.features.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.petergabriel.budgetcalendar.core.designsystem.component.BcBadge
import com.petergabriel.budgetcalendar.core.designsystem.component.BcButton
import com.petergabriel.budgetcalendar.core.designsystem.component.BadgeVariant
import com.petergabriel.budgetcalendar.core.designsystem.component.ButtonVariant
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme


enum class AmountTone {
    Neutral,
    Positive,
    Negative,
    Info,
}

data class SelectedDayTransactionPreviewItem(
    val id: Long,
    val title: String,
    val description: String?,
    val accountLabel: String,
    val amountLabel: String,
    val amountTone: AmountTone,
    val statusLabel: String?,
    val accessibilityLabel: String,
)

@Composable
fun SelectedDayTransactionPreview(
    state: SelectedDayTransactionPreviewState,
    onTransactionClick: (Long) -> Unit,
    onViewAllClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = state.dateLabel,
                style = typography.cardTitle,
                color = colors.textPrimary,
            )
            Text(
                text = "${state.transactionCount} ${if (state.transactionCount == 1) "transaction" else "transactions"}",
                style = typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.semantics {
                    contentDescription = "${state.transactionCount} transactions on ${state.dateLabel}"
                },
            )
        }

        when {
            state.isLoading -> {
                repeat(3) {
                    TransactionPreviewSkeleton()
                }
            }

            !state.errorMessage.isNullOrBlank() -> {
                Text(
                    text = state.errorMessage,
                    style = typography.bodySmall,
                    color = colors.colorErrorText,
                )
                BcButton(
                    text = "Try again",
                    onClick = onRetry,
                    variant = ButtonVariant.Ghost,
                )
            }

            state.items.isEmpty() -> {
                Text(
                    text = "No transactions this day",
                    style = typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }

            else -> {
                state.items.take(3).forEach { item ->
                    CompactTransactionListItem(
                        item = item,
                        onTap = { onTransactionClick(item.id) },
                    )
                }
                if (state.hasMore) {
                    BcButton(
                        text = "View all transactions",
                        onClick = onViewAllClick,
                        variant = ButtonVariant.Ghost,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
fun CompactTransactionListItem(
    item: SelectedDayTransactionPreviewItem,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing
    val radius = BudgetCalendarTheme.radius
    val amountColor = when (item.amountTone) {
        AmountTone.Neutral -> colors.textPrimary
        AmountTone.Positive -> colors.colorSuccessText
        AmountTone.Negative -> colors.colorErrorText
        AmountTone.Info -> colors.colorInfoText
    }
    val statusVariant = when (item.statusLabel?.lowercase()) {
        "pending" -> BadgeVariant.Pending
        "overdue" -> BadgeVariant.Overdue
        else -> null
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = spacing.spacing12)
            .background(
                color = colors.bgSurface,
                shape = RoundedCornerShape(radius.lg),
            )
            .clickable(onClick = onTap)
            .semantics {
                contentDescription = item.accessibilityLabel
            }
            .padding(spacing.spacing3),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.spacing1),
        ) {
            Text(
                text = item.title,
                style = typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            item.description?.takeIf(String::isNotBlank)?.let { description ->
                Text(
                    text = description,
                    style = typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = item.accountLabel,
                style = typography.bodySmall,
                color = colors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Column(
            horizontalAlignment = androidx.compose.ui.Alignment.End,
            verticalArrangement = Arrangement.spacedBy(spacing.spacing1),
        ) {
            Text(
                text = item.amountLabel,
                style = typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor,
            )
            statusVariant?.let { variant ->
                BcBadge(
                    text = item.statusLabel.orEmpty(),
                    variant = variant,
                )
            }
        }
    }
}

@Composable
private fun TransactionPreviewSkeleton() {
    val colors = BudgetCalendarTheme.colors
    val spacing = BudgetCalendarTheme.spacing
    val radius = BudgetCalendarTheme.radius

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = spacing.spacing12)
            .background(
                color = colors.bgSurface,
                shape = RoundedCornerShape(radius.lg),
            )
            .padding(spacing.spacing3),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.spacing1)) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .background(colors.bgMuted, RoundedCornerShape(radius.sm))
                    .padding(horizontal = spacing.spacing8, vertical = spacing.spacing1),
            ) {}
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .background(colors.bgMuted, RoundedCornerShape(radius.sm))
                    .padding(horizontal = spacing.spacing6, vertical = spacing.spacing1),
            ) {}
        }
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .background(colors.bgMuted, RoundedCornerShape(radius.sm))
                .padding(horizontal = spacing.spacing4, vertical = spacing.spacing1),
        ) {}
    }
}
