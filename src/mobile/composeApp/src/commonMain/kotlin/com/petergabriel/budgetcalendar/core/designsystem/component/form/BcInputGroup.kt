package com.petergabriel.budgetcalendar.core.designsystem.component.form

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme

@Composable
fun BcInputGroup(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    fieldModifier: Modifier = Modifier,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    errorText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    minLines: Int = 1,
    maxLines: Int = 1,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val radius = BudgetCalendarTheme.radius
    val spacing = BudgetCalendarTheme.spacing
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
    ) {
        Text(
            text = label,
            style = typography.caption,
            color = colors.textSecondary,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(radius.lg))
                .border(
                    width = spacing.spacing1 / 2,
                    color = when {
                        isError -> colors.colorError
                        isFocused -> colors.focusRing
                        else -> colors.borderStrong
                    },
                    shape = RoundedCornerShape(radius.lg),
                )
                .defaultMinSize(minHeight = spacing.spacing12)
                .then(
                    onClick?.let { clickHandler ->
                        Modifier
                            .clickable(onClick = clickHandler)
                            .semantics(mergeDescendants = true) {
                                contentDescription = "$label, ${value.ifBlank { placeholder }}"
                            }
                    } ?: Modifier,
                )
                .padding(horizontal = spacing.spacing4, vertical = spacing.spacing3),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.spacing2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = fieldModifier
                        .weight(1f)
                        .onFocusChanged { isFocused = it.isFocused },
                    readOnly = readOnly,
                    enabled = !readOnly,
                    singleLine = minLines == 1 && maxLines == 1,
                    minLines = minLines,
                    maxLines = maxLines,
                    keyboardOptions = keyboardOptions,
                    textStyle = typography.bodyMedium.copy(color = colors.textPrimary),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (value.isBlank()) {
                                Text(
                                    text = placeholder,
                                    style = typography.bodyMedium,
                                    color = colors.textTertiary,
                                )
                            }
                            innerTextField()
                        }
                    },
                )

                trailingContent?.invoke()
            }
        }

        if (isError && !errorText.isNullOrBlank()) {
            Text(
                text = errorText,
                style = typography.bodySmall,
                color = colors.colorErrorText,
                modifier = Modifier.padding(start = spacing.spacing1),
            )
        }
    }
}
