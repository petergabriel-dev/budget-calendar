package com.petergabriel.budgetcalendar.features.sandbox.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.petergabriel.budgetcalendar.core.designsystem.component.BcButton
import com.petergabriel.budgetcalendar.core.designsystem.component.ButtonVariant
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxSnapshot
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@Composable
fun SandboxListScreen(
    snapshots: List<SandboxSnapshot>,
    modifier: Modifier = Modifier,
    onSnapshotTap: (SandboxSnapshot) -> Unit,
    onSnapshotLongPress: (SandboxSnapshot) -> Unit,
    onAddSandbox: () -> Unit,
) {
    val spacing = BudgetCalendarTheme.spacing

    Box(modifier = modifier.fillMaxSize()) {
        if (snapshots.isEmpty()) {
            EmptyState(
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = spacing.spacing4),
                verticalArrangement = Arrangement.spacedBy(spacing.spacing3),
            ) {
                items(snapshots, key = { snapshot -> snapshot.id }) { snapshot ->
                    SandboxSnapshotCard(
                        snapshot = snapshot,
                        onTap = { onSnapshotTap(snapshot) },
                        onLongPress = { onSnapshotLongPress(snapshot) },
                    )
                }
            }
        }

        BcButton(
            text = "New Sandbox",
            onClick = onAddSandbox,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(spacing.spacing5),
            variant = ButtonVariant.PrimaryIconLeading,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SandboxSnapshotCard(
    snapshot: SandboxSnapshot,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing
    val radius = BudgetCalendarTheme.radius

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = colors.bgSurface,
                shape = RoundedCornerShape(radius.xl),
            )
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress,
            )
            .padding(spacing.spacing4),
        verticalArrangement = Arrangement.spacedBy(spacing.spacing1),
    ) {
        Text(
            text = snapshot.name,
            style = typography.cardTitle,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (!snapshot.description.isNullOrBlank()) {
            Text(
                text = snapshot.description,
                style = typography.bodyMedium,
                color = colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Text(
            text = "Created ${formatDate(snapshot.createdAt)}",
            style = typography.bodySmall,
            color = colors.textTertiary,
        )

        Text(
            text = "Initial Safe to Spend: ${CurrencyUtils.formatCents(snapshot.initialSafeToSpend)}",
            style = typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary,
        )
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BudgetCalendarTheme.spacing.spacing2),
    ) {
        Text(
            text = "No sandboxes - create one to simulate scenarios",
            style = typography.section,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
        )
    }
}

private fun formatDate(dateMillis: Long): String {
    return Instant
        .fromEpochMilliseconds(dateMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
        .toString()
}
