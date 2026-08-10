package com.petergabriel.budgetcalendar.features.sandbox.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.petergabriel.budgetcalendar.core.designsystem.component.BcButton
import com.petergabriel.budgetcalendar.core.designsystem.component.ButtonVariant
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme

@Composable
fun SandboxEmptySnapshotState(
    onSelectSnapshot: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BudgetCalendarTheme.colors
    val spacing = BudgetCalendarTheme.spacing
    val typography = BudgetCalendarTheme.typography

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = spacing.spacing6)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.spacing3),
    ) {
        Text(
            text = "No Snapshot Selected",
            style = typography.section,
            color = colors.textPrimary,
        )
        Text(
            text = "Choose or create a sandbox snapshot to start planning.",
            style = typography.bodyMedium,
            color = colors.textSecondary,
        )
        BcButton(
            text = "Select snapshot",
            onClick = onSelectSnapshot,
            variant = ButtonVariant.Primary,
        )
    }
}

@Composable
fun SandboxScopedError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BudgetCalendarTheme.colors
    val spacing = BudgetCalendarTheme.spacing

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
    ) {
        Text(
            text = message,
            style = BudgetCalendarTheme.typography.bodySmall,
            color = colors.colorErrorText,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        BcButton(
            text = "Retry",
            onClick = onRetry,
            variant = ButtonVariant.Outline,
        )
    }
}
