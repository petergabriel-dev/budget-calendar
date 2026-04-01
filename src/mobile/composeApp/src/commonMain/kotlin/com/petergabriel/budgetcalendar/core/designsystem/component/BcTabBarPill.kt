package com.petergabriel.budgetcalendar.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme

data class BcTabItem(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit,
    val icon: ImageVector? = null,
)

@Composable
fun BcTabBarPill(
    tabs: List<BcTabItem>,
    modifier: Modifier = Modifier,
    onAddClick: (() -> Unit)? = null,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val radius = BudgetCalendarTheme.radius
    val splitIndex = tabs.size / 2
    val leadingTabs = if (onAddClick != null) tabs.take(splitIndex) else tabs
    val trailingTabs = if (onAddClick != null) tabs.drop(splitIndex) else emptyList()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(62.dp)
            .clip(RoundedCornerShape(radius.full))
            .background(colors.bgSurface)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leadingTabs.forEach { tab ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(radius.full))
                    .clickable(onClick = tab.onClick),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (tab.icon != null) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (tab.selected) colors.textPrimary else colors.textTertiary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Text(
                        text = tab.label,
                        style = typography.caption,
                        color = if (tab.selected) colors.textPrimary else colors.textTertiary,
                    )
                }
            }
        }

        if (onAddClick != null) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(radius.full))
                    .background(colors.bgDark)
                    .clickable(onClick = onAddClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Add",
                    tint = colors.textInverted,
                )
            }
        }

        trailingTabs.forEach { tab ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(radius.full))
                    .clickable(onClick = tab.onClick),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (tab.icon != null) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (tab.selected) colors.textPrimary else colors.textTertiary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Text(
                        text = tab.label,
                        style = typography.caption,
                        color = if (tab.selected) colors.textPrimary else colors.textTertiary,
                    )
                }
            }
        }
    }
}
