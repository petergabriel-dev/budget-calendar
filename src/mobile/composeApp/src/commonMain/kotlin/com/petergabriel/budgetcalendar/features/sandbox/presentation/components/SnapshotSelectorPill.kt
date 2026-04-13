package com.petergabriel.budgetcalendar.features.sandbox.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxSnapshot

@Composable
fun SnapshotSelectorPill(
    activeSnapshot: SandboxSnapshot?,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BudgetCalendarTheme.colors
    val spacing = BudgetCalendarTheme.spacing
    val typography = BudgetCalendarTheme.typography
    val radius = BudgetCalendarTheme.radius

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .background(
                color = colors.bgDark,
                shape = RoundedCornerShape(radius.full),
            )
            .clickable(onClick = onTap)
            .padding(horizontal = spacing.spacing4, vertical = spacing.spacing3),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = activeSnapshot?.name ?: "No Snapshot Selected",
            style = typography.bodyLarge,
            color = colors.textInverted,
        )

        Icon(
            imageVector = Icons.Outlined.Science,
            contentDescription = "Select sandbox snapshot",
            tint = colors.textInverted,
        )
    }
}
