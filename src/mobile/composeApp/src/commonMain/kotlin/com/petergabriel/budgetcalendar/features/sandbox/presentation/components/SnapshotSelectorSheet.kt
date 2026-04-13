package com.petergabriel.budgetcalendar.features.sandbox.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.petergabriel.budgetcalendar.core.designsystem.component.BcButton
import com.petergabriel.budgetcalendar.core.designsystem.component.ButtonVariant
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxSnapshot
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnapshotSelectorSheet(
    isVisible: Boolean,
    snapshots: List<SandboxSnapshot>,
    activeSnapshotId: Long?,
    onSelect: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onCreateNew: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!isVisible) {
        return
    }

    val colors = BudgetCalendarTheme.colors
    val spacing = BudgetCalendarTheme.spacing
    val typography = BudgetCalendarTheme.typography
    val radius = BudgetCalendarTheme.radius

    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var createName by rememberSaveable { mutableStateOf("") }
    var deleteCandidateId by remember { mutableStateOf<Long?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.spacing4, vertical = spacing.spacing2),
            verticalArrangement = Arrangement.spacedBy(spacing.spacing3),
        ) {
            Text(
                text = "Select Sandbox",
                style = typography.section,
                color = colors.textPrimary,
            )

            val orderedSnapshots = remember(snapshots) {
                snapshots.sortedByDescending { snapshot -> snapshot.lastAccessedAt }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
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
                    LazyColumn {
                        items(orderedSnapshots, key = { snapshot -> snapshot.id }) { snapshot ->
                            SnapshotRow(
                                snapshot = snapshot,
                                isActive = snapshot.id == activeSnapshotId,
                                onSelect = {
                                    onSelect(snapshot.id)
                                    onDismiss()
                                },
                                onLongPress = { deleteCandidateId = snapshot.id },
                            )
                            if (snapshot.id != orderedSnapshots.last().id) {
                                HorizontalDivider(color = colors.borderStrong)
                            }
                        }
                    }
                }
            }

            BcButton(
                text = "Create New",
                onClick = { showCreateDialog = true },
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.Ghost,
            )
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                createName = ""
            },
            title = { Text("Create Sandbox") },
            text = {
                OutlinedTextField(
                    value = createName,
                    onValueChange = { value ->
                        createName = value.take(50)
                    },
                    singleLine = true,
                    label = { Text("Snapshot Name") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateDialog = false
                        createName = ""
                    },
                ) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = createName.trim()
                        if (trimmed.isBlank()) {
                            return@TextButton
                        }
                        onCreateNew(trimmed)
                        showCreateDialog = false
                        createName = ""
                        onDismiss()
                    },
                ) {
                    Text("Create")
                }
            },
        )
    }

    deleteCandidateId?.let { snapshotId ->
        AlertDialog(
            onDismissRequest = { deleteCandidateId = null },
            title = { Text("Delete sandbox?") },
            text = { Text("This will remove the snapshot and its sandbox transactions.") },
            dismissButton = {
                TextButton(onClick = { deleteCandidateId = null }) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(snapshotId)
                        deleteCandidateId = null
                    },
                ) {
                    Text(
                        text = "Delete",
                        color = colors.colorError,
                    )
                }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SnapshotRow(
    snapshot: SandboxSnapshot,
    isActive: Boolean,
    onSelect: () -> Unit,
    onLongPress: () -> Unit,
) {
    val colors = BudgetCalendarTheme.colors
    val spacing = BudgetCalendarTheme.spacing
    val typography = BudgetCalendarTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onSelect,
                onLongClick = onLongPress,
            )
            .padding(horizontal = spacing.spacing4, vertical = spacing.spacing3),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.spacing1),
        ) {
            Text(
                text = snapshot.name,
                style = typography.bodyLarge,
                color = colors.textPrimary,
            )
            Text(
                text = formatLastAccessed(snapshot.lastAccessedAt),
                style = typography.bodySmall,
                color = colors.textSecondary,
            )
        }

        if (isActive) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = "Active snapshot",
                tint = colors.textPrimary,
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
