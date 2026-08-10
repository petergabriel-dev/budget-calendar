package com.petergabriel.budgetcalendar.features.sandbox.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.petergabriel.budgetcalendar.core.designsystem.component.BcBadge
import com.petergabriel.budgetcalendar.core.designsystem.component.BcButton
import com.petergabriel.budgetcalendar.core.designsystem.component.BcConfirmDialog
import com.petergabriel.budgetcalendar.core.designsystem.component.BcSheetShell
import com.petergabriel.budgetcalendar.core.designsystem.component.BadgeVariant
import com.petergabriel.budgetcalendar.core.designsystem.component.ButtonVariant
import com.petergabriel.budgetcalendar.core.designsystem.component.form.BcInputGroup
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxSnapshot
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@Composable
fun SnapshotSelectorSheet(
    isVisible: Boolean,
    snapshots: List<SandboxSnapshot>,
    activeSnapshotId: Long?,
    onSelect: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onCreateNew: (String) -> Unit,
    onDismiss: () -> Unit,
    isBusy: Boolean = false,
    errorMessage: String? = null,
) {
    if (!isVisible) {
        return
    }

    val colors = BudgetCalendarTheme.colors
    val spacing = BudgetCalendarTheme.spacing
    val typography = BudgetCalendarTheme.typography
    val radius = BudgetCalendarTheme.radius
    val orderedSnapshots = snapshots.sortedByDescending { snapshot -> snapshot.lastAccessedAt }

    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var createName by rememberSaveable { mutableStateOf("") }
    var deleteCandidateId by rememberSaveable { mutableStateOf<Long?>(null) }

    BcSheetShell(
        title = "Select Sandbox",
        onDismissRequest = { if (!isBusy) onDismiss() },
        bodyScrollable = false,
        body = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(colors.bgSurface, RoundedCornerShape(radius.xl)),
            ) {
                if (orderedSnapshots.isEmpty()) {
                    Text(
                        text = "No snapshots yet.",
                        style = typography.bodyMedium,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(spacing.spacing4),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        items(orderedSnapshots, key = { snapshot -> snapshot.id }) { snapshot ->
                            SnapshotRow(
                                snapshot = snapshot,
                                isActive = snapshot.id == activeSnapshotId,
                                isBusy = isBusy,
                                onSelect = { onSelect(snapshot.id) },
                                onDelete = { deleteCandidateId = snapshot.id },
                            )
                            if (snapshot.id != orderedSnapshots.lastOrNull()?.id) {
                                HorizontalDivider(color = colors.borderStrong)
                            }
                        }
                    }
                }

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        style = typography.bodySmall,
                        color = colors.colorErrorText,
                        modifier = Modifier.padding(spacing.spacing4),
                    )
                }
            }
        },
        footer = {
            BcButton(
                text = "Create New",
                onClick = { showCreateDialog = true },
                variant = ButtonVariant.Outline,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                createName = ""
            },
            title = { Text("Create Sandbox") },
            text = {
                BcInputGroup(
                    label = "Snapshot Name",
                    value = createName,
                    onValueChange = { value -> createName = value.take(50) },
                    placeholder = "e.g. Summer plan",
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            dismissButton = {
                BcButton(
                    text = "Cancel",
                    onClick = {
                        showCreateDialog = false
                        createName = ""
                    },
                    variant = ButtonVariant.Ghost,
                )
            },
            confirmButton = {
                BcButton(
                    text = "Create",
                    onClick = {
                        val trimmed = createName.trim()
                        if (trimmed.isBlank() || isBusy) return@BcButton
                        onCreateNew(trimmed)
                        showCreateDialog = false
                        createName = ""
                    },
                    enabled = createName.trim().isNotBlank() && !isBusy,
                )
            },
        )
    }

    deleteCandidateId?.let { snapshotId ->
        val snapshotName = orderedSnapshots.firstOrNull { snapshot -> snapshot.id == snapshotId }?.name
            ?: "this snapshot"
        BcConfirmDialog(
            title = "Delete sandbox?",
            message = "Delete $snapshotName and its sandbox transactions? This action cannot be undone.",
            onDismissRequest = { if (!isBusy) deleteCandidateId = null },
            onConfirm = {
                onDelete(snapshotId)
                deleteCandidateId = null
            },
            confirmLabel = "Delete",
            isBusy = isBusy,
        )
    }
}

@Composable
private fun SnapshotRow(
    snapshot: SandboxSnapshot,
    isActive: Boolean,
    isBusy: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = BudgetCalendarTheme.colors
    val spacing = BudgetCalendarTheme.spacing
    val typography = BudgetCalendarTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "${snapshot.name}${if (isActive) ", Active snapshot" else ""}"
            }
            .padding(horizontal = spacing.spacing3, vertical = spacing.spacing2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.spacing2),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(enabled = !isBusy, onClick = onSelect)
                .padding(vertical = spacing.spacing2),
            verticalArrangement = Arrangement.spacedBy(spacing.spacing1),
        ) {
            Text(
                text = snapshot.name,
                style = typography.bodyLarge,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Last accessed ${formatLastAccessed(snapshot.lastAccessedAt)}",
                style = typography.bodySmall,
                color = colors.textSecondary,
            )
        }

        if (isActive) {
            BcBadge(
                text = "Active",
                variant = BadgeVariant.Info,
            )
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = "Active snapshot",
                tint = colors.textPrimary,
                modifier = Modifier.size(spacing.spacing5),
            )
        }

        IconButton(
            onClick = onDelete,
            enabled = !isBusy,
            modifier = Modifier
                .size(spacing.spacing12)
                .semantics { contentDescription = "Delete ${snapshot.name}" },
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = null,
                tint = colors.colorErrorText,
            )
        }
    }
}

private fun formatLastAccessed(millis: Long): String {
    val localDateTime = Instant
        .fromEpochMilliseconds(millis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    val hour = localDateTime.hour.toString().padStart(2, '0')
    val minute = localDateTime.minute.toString().padStart(2, '0')
    return "${localDateTime.date} $hour:$minute"
}
