package com.petergabriel.budgetcalendar.features.budget.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@Composable
fun SafeToSpendHeader(
    amount: Long,
    lastUpdated: Long,
    isCalculating: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pullDistance = remember { mutableFloatStateOf(0f) }
    val colors = BudgetCalendarTheme.colors
    val spacing = BudgetCalendarTheme.spacing
    val radius = BudgetCalendarTheme.radius
    val typography = BudgetCalendarTheme.typography

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 122.dp)
            .background(
                color = colors.colorSuccessBg,
                shape = RoundedCornerShape(radius.xxl),
            )
            .pointerInput(onRefresh) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        if (dragAmount > 0f) {
                            pullDistance.floatValue += dragAmount
                        }
                    },
                    onDragEnd = {
                        if (pullDistance.floatValue >= 72f) {
                            onRefresh()
                        }
                        pullDistance.floatValue = 0f
                    },
                    onDragCancel = {
                        pullDistance.floatValue = 0f
                    },
                )
            }
            .padding(horizontal = spacing.spacing4, vertical = spacing.spacing3),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isCalculating) {
                ShimmerAmountPlaceholder(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .height(42.dp)
                        .fillMaxWidth(0.52f),
                )
            } else {
                Text(
                    text = CurrencyUtils.formatCents(amount),
                    style = typography.displayMedium,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
            }

            Text(
                text = "Safe to Spend",
                style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textSecondary,
                modifier = Modifier.padding(top = spacing.spacing1),
            )

            Text(
                text = "Pull down to refresh",
                style = typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        Text(
            text = formatLastUpdated(lastUpdated),
            style = typography.bodySmall,
            color = colors.textSecondary,
            modifier = Modifier.align(Alignment.BottomEnd),
        )
    }
}

@Composable
private fun ShimmerAmountPlaceholder(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "safe_to_spend_shimmer")
    val alpha = transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "safe_to_spend_shimmer_alpha",
    )

    Box(
        modifier = modifier
            .alpha(alpha.value)
            .background(
                color = BudgetCalendarTheme.colors.textPrimary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(BudgetCalendarTheme.radius.lg),
            ),
    )
}

private fun formatLastUpdated(lastUpdated: Long): String {
    val localDateTime = Instant.fromEpochMilliseconds(lastUpdated)
        .toLocalDateTime(TimeZone.currentSystemDefault())

    val hour = localDateTime.hour.toString().padStart(2, '0')
    val minute = localDateTime.minute.toString().padStart(2, '0')
    return "Updated $hour:$minute"
}
