package com.petergabriel.budgetcalendar.core.designsystem.component.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme

@Composable
fun BcSwitchRow(
    label: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = spacing.spacing12)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .semantics(mergeDescendants = true) {
                contentDescription = listOfNotNull(label, subtitle).joinToString(", ")
                stateDescription = if (checked) "On" else "Off"
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.spacing1),
        ) {
            Text(
                text = label,
                style = typography.bodyMedium,
                color = colors.textPrimary,
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
