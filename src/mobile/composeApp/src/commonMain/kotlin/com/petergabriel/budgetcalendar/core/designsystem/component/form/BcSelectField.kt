package com.petergabriel.budgetcalendar.core.designsystem.component.form

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme

@Composable
fun BcSelectField(
    label: String,
    value: String,
    onClick: () -> Unit,
    placeholder: String = "Select an option",
    modifier: Modifier = Modifier,
    fieldModifier: Modifier = Modifier,
) {
    val colors = BudgetCalendarTheme.colors
    val spacing = BudgetCalendarTheme.spacing

    BcInputGroup(
        label = label,
        value = value,
        onValueChange = {},
        placeholder = placeholder,
        modifier = modifier,
        fieldModifier = fieldModifier
            .clickable(onClick = onClick)
            .semantics { role = Role.DropdownList },
        readOnly = true,
        onClick = onClick,
        trailingContent = {
            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(spacing.spacing6),
            )
        },
    )
}
