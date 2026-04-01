package com.petergabriel.budgetcalendar.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme

@Composable
fun BcSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val radius = BudgetCalendarTheme.radius

    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(radius.full),
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Search",
                tint = colors.textTertiary,
            )
        },
        textStyle = typography.bodyMedium.copy(color = colors.textPrimary),
        placeholder = {
            Text(
                text = placeholder,
                style = typography.bodyMedium,
                color = colors.textTertiary,
            )
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = colors.bgSurface,
            unfocusedContainerColor = colors.bgSurface,
            disabledContainerColor = colors.bgSurface,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            focusedLeadingIconColor = colors.textTertiary,
            unfocusedLeadingIconColor = colors.textTertiary,
            focusedPlaceholderColor = colors.textTertiary,
            unfocusedPlaceholderColor = colors.textTertiary,
            focusedIndicatorColor = colors.borderStrong,
            unfocusedIndicatorColor = colors.borderStrong,
            cursorColor = colors.textPrimary,
        ),
    )
}
