package com.petergabriel.budgetcalendar.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme

data class BcWeekDay(
    val id: String,
    val weekdayLabel: String,
    val dayOfMonth: Int,
    val accessibilityLabel: String,
    val isSelected: Boolean,
    val isToday: Boolean = false,
    val hasTransactions: Boolean = false,
    val enabled: Boolean = true,
)

@Composable
fun BcWeekPicker(
    days: List<BcWeekDay>,
    onDaySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing
    val radius = BudgetCalendarTheme.radius

    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { selectableGroup() },
        horizontalArrangement = Arrangement.spacedBy(spacing.spacing1),
    ) {
        days.forEach { day ->
            val containerModifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = spacing.spacing12)
                .then(
                    if (day.isSelected) {
                        Modifier.background(
                            color = colors.bgDark,
                            shape = RoundedCornerShape(radius.lg),
                        )
                    } else if (day.isToday) {
                        Modifier.border(
                            width = spacing.spacing1 / 4,
                            color = colors.borderStrong,
                            shape = RoundedCornerShape(radius.lg),
                        )
                    } else {
                        Modifier
                    },
                )
                .semantics {
                    contentDescription = day.accessibilityLabel
                    selected = day.isSelected
                }
                .selectable(
                    selected = day.isSelected,
                    enabled = day.enabled,
                    role = Role.Tab,
                    onClick = { onDaySelected(day.id) },
                )

            Column(
                modifier = containerModifier.padding(vertical = spacing.spacing2),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.spacing1),
            ) {
                Text(
                    text = day.weekdayLabel,
                    style = typography.caption,
                    color = when {
                        day.isSelected -> colors.textInverted
                        day.enabled -> colors.textSecondary
                        else -> colors.textDisabled
                    },
                )
                Text(
                    text = day.dayOfMonth.toString(),
                    style = typography.bodyLarge,
                    color = when {
                        day.isSelected -> colors.textInverted
                        day.enabled -> colors.textPrimary
                        else -> colors.textDisabled
                    },
                )
                if (day.hasTransactions && day.enabled) {
                    Box(
                        modifier = Modifier
                            .size(spacing.spacing1)
                            .background(
                                color = if (day.isSelected) colors.textInverted else colors.colorInfoText,
                                shape = RoundedCornerShape(radius.full),
                            ),
                    )
                }
            }
        }
    }
}
