package com.petergabriel.budgetcalendar.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BcSheetShell(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    closeContentDescription: String = "Close",
    bodyScrollable: Boolean = true,
    headerContent: (@Composable RowScope.() -> Unit)? = null,
    body: @Composable ColumnScope.() -> Unit,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
) {
    BcSheetShellContent(
        title = title,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        closeContentDescription = closeContentDescription,
        bodyScrollable = bodyScrollable,
        headerContent = headerContent,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        body = body,
        footer = footer,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BcSheetShellContent(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    closeContentDescription: String,
    bodyScrollable: Boolean,
    headerContent: (@Composable RowScope.() -> Unit)?,
    sheetState: SheetState,
    body: @Composable ColumnScope.() -> Unit,
    footer: (@Composable ColumnScope.() -> Unit)?,
) {
    val colors = BudgetCalendarTheme.colors
    val radius = BudgetCalendarTheme.radius

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = radius.xxl,
            topEnd = radius.xxl,
        ),
        containerColor = colors.bgPrimary,
        contentColor = colors.textPrimary,
        dragHandle = {},
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
        ) {
            BcSheetHeader(
                title = title,
                onDismiss = onDismissRequest,
                closeContentDescription = closeContentDescription,
                actionContent = headerContent,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(
                        if (bodyScrollable) {
                            Modifier.verticalScroll(rememberScrollState())
                        } else {
                            Modifier
                        },
                    )
                    .padding(horizontal = BudgetCalendarTheme.spacing.spacing6),
                verticalArrangement = Arrangement.spacedBy(BudgetCalendarTheme.spacing.spacing4),
                content = body,
            )

            footer?.let { footerContent ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = BudgetCalendarTheme.spacing.spacing6,
                            vertical = BudgetCalendarTheme.spacing.spacing4,
                        ),
                    content = footerContent,
                )
            }
        }
    }
}

@Composable
fun BcSheetHeader(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    closeContentDescription: String = "Close",
    actionContent: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = spacing.spacing6,
                end = spacing.spacing3,
                top = spacing.spacing4,
                bottom = spacing.spacing2,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
            style = typography.section,
            color = colors.textPrimary,
        )
        actionContent?.invoke(this)
        IconButton(
            modifier = Modifier.size(spacing.spacing12),
            onClick = onDismiss,
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = closeContentDescription,
                tint = colors.textPrimary,
            )
        }
    }
}

@Composable
fun BcSheetFooter(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val spacing = BudgetCalendarTheme.spacing

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.spacing2),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
