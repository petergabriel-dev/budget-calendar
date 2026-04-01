package com.petergabriel.budgetcalendar.core.designsystem.component.form

import androidx.compose.foundation.border
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme

@Composable
fun BcInputGroup(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    trailingContent: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    errorText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val radius = BudgetCalendarTheme.radius

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
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
                    width = 1.5.dp,
                    color = if (isError) colors.colorError else colors.borderStrong,
                    shape = RoundedCornerShape(radius.lg),
                )
                .defaultMinSize(minHeight = 48.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    readOnly = readOnly,
                    enabled = !readOnly,
                    singleLine = true,
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
                color = colors.colorError,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}
