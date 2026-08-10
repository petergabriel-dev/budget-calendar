package com.petergabriel.budgetcalendar.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.semantics
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme

@Composable
fun BcSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val radius = BudgetCalendarTheme.radius
    val spacing = BudgetCalendarTheme.spacing

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(spacing.spacing12)
            .clip(RoundedCornerShape(radius.full))
            .background(colors.bgSurface)
            .padding(spacing.spacing1)
            .semantics { selectableGroup() },
        horizontalArrangement = Arrangement.spacedBy(spacing.spacing1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, option ->
            val isActive = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(spacing.spacing10)
                    .clip(RoundedCornerShape(radius.lg))
                    .background(if (isActive) colors.bgDark else androidx.compose.ui.graphics.Color.Transparent)
                    .selectable(
                        selected = isActive,
                        onClick = { onSelectedIndexChange(index) },
                        role = Role.Tab,
                    )
                    .semantics(mergeDescendants = true) {},
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option,
                    style = typography.bodyMedium,
                    color = if (isActive) colors.textInverted else colors.textSecondary,
                )
            }
        }
    }
}
