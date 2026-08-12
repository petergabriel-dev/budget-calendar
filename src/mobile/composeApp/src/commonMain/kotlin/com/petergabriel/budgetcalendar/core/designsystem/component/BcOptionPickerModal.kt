package com.petergabriel.budgetcalendar.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.semantics
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme

@Composable
fun BcOptionPickerModal(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
    optionTestTag: ((Int) -> String)? = null,
) {
    val colors = BudgetCalendarTheme.colors
    val spacing = BudgetCalendarTheme.spacing
    val typography = BudgetCalendarTheme.typography

    BcSheetShell(
        title = title,
        onDismissRequest = onDismiss,
        closeContentDescription = "Close option picker",
        bodyScrollable = false,
        body = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { selectableGroup() },
            ) {
                itemsIndexed(
                    items = options,
                    key = { index, _ -> index },
                ) { index, option ->
                    val isSelected = index == selectedIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = spacing.spacing12)
                            .selectable(
                                selected = isSelected,
                                onClick = { onSelect(index) },
                                role = Role.RadioButton,
                            )
                            .then(
                                optionTestTag?.invoke(index)?.let { tag ->
                                    Modifier.testTag(tag)
                                } ?: Modifier,
                            )
                            .semantics(mergeDescendants = true) {
                                contentDescription = if (isSelected) {
                                    "$option, selected"
                                } else {
                                    option
                                }
                            }
                            .padding(
                                horizontal = spacing.spacing4,
                                vertical = spacing.spacing3,
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = option,
                            style = typography.bodyMedium,
                            color = colors.textPrimary,
                        )

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = colors.colorSuccessText,
                                modifier = Modifier.size(spacing.spacing6),
                            )
                        }
                    }
                }
            }
        },
    )
}
