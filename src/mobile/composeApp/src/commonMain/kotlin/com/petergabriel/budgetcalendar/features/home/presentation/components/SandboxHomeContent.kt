package com.petergabriel.budgetcalendar.features.home.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.petergabriel.budgetcalendar.core.designsystem.component.BcButton
import com.petergabriel.budgetcalendar.core.designsystem.component.BcSectionHeader
import com.petergabriel.budgetcalendar.core.designsystem.component.ButtonVariant
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils
import com.petergabriel.budgetcalendar.core.utils.MoneyInputUtils
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.ConsequencesResult
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxSnapshot
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SimulationInput
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@Composable
fun SnapshotSelectorPill(
    activeSnapshot: SandboxSnapshot?,
    onSnapshotPillTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BudgetCalendarTheme.colors
    val spacing = BudgetCalendarTheme.spacing
    val typography = BudgetCalendarTheme.typography
    val radius = BudgetCalendarTheme.radius

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bgDark, RoundedCornerShape(radius.full))
            .clickable(onClick = onSnapshotPillTap)
            .padding(horizontal = spacing.spacing4, vertical = spacing.spacing3),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = activeSnapshot?.name ?: "No Snapshot Selected",
            style = typography.bodyLarge,
            color = colors.textInverted,
        )

        Icon(
            imageVector = Icons.Outlined.Science,
            contentDescription = "Select sandbox snapshot",
            tint = colors.textInverted,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnapshotSelectorSheet(
    isVisible: Boolean,
    snapshots: List<SandboxSnapshot>,
    activeSnapshotId: Long?,
    onSelect: (Long) -> Unit,
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

    val orderedSnapshots = remember(snapshots) {
        snapshots.sortedByDescending { snapshot -> snapshot.lastAccessedAt }
    }

    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var newName by rememberSaveable { mutableStateOf("") }

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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bgSurface, RoundedCornerShape(radius.xl)),
            ) {
                orderedSnapshots.forEachIndexed { index, snapshot ->
                    val isActive = snapshot.id == activeSnapshotId
                    SnapshotRow(
                        snapshot = snapshot,
                        isActive = isActive,
                        onClick = {
                            onSelect(snapshot.id)
                            onDismiss()
                        },
                    )
                    if (index < orderedSnapshots.lastIndex) {
                        HorizontalDivider(color = colors.borderStrong)
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
                newName = ""
            },
            title = { Text("Create Sandbox") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { value ->
                        newName = value.take(50)
                    },
                    singleLine = true,
                    label = { Text("Snapshot Name") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = newName.trim()
                        if (trimmed.isNotEmpty()) {
                            onCreateNew(trimmed)
                            showCreateDialog = false
                            newName = ""
                            onDismiss()
                        }
                    },
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateDialog = false
                        newName = ""
                    },
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun SnapshotRow(
    snapshot: SandboxSnapshot,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val colors = BudgetCalendarTheme.colors
    val spacing = BudgetCalendarTheme.spacing
    val typography = BudgetCalendarTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(spacing.spacing4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.spacing3),
    ) {
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(24.dp)
                .border(1.5.dp, colors.borderStrong, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (isActive) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = colors.textPrimary,
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = snapshot.name,
                style = typography.bodyLarge,
                color = colors.textPrimary,
            )
            Text(
                text = "Created ${formatDate(snapshot.createdAt)}",
                style = typography.bodySmall,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
fun SimulationFormCard(
    simulationInput: SimulationInput,
    onSimulationInputChange: (SimulationInput) -> Unit,
    onRunSimulation: () -> Unit,
    onClearSimulation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BudgetCalendarTheme.colors
    val spacing = BudgetCalendarTheme.spacing
    val radius = BudgetCalendarTheme.radius

    val amountText = remember(simulationInput.amount) {
        if (simulationInput.amount <= 0L) "" else MoneyInputUtils.centsToInput(simulationInput.amount)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.spacing3),
    ) {
        BcSectionHeader(
            title = "Simulate Expense",
            actionText = "Clear",
            onActionClick = onClearSimulation,
            modifier = Modifier.fillMaxWidth(),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bgSurface, RoundedCornerShape(radius.xl))
                .padding(spacing.spacing6),
            verticalArrangement = Arrangement.spacedBy(spacing.spacing4),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.spacing3),
                verticalAlignment = Alignment.Top,
            ) {
                LabeledInputField(
                    label = "Purchase Name",
                    value = simulationInput.purchaseName,
                    placeholder = "e.g. MacBook Pro",
                    onValueChange = { name ->
                        onSimulationInputChange(
                            simulationInput.copy(purchaseName = name),
                        )
                    },
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Text,
                )

                LabeledInputField(
                    label = "Amount",
                    value = amountText,
                    placeholder = "0.00",
                    prefix = "₱",
                    onValueChange = { raw ->
                        val parsed = if (raw.isBlank()) {
                            0L
                        } else {
                            MoneyInputUtils.parseToCents(raw)?.takeIf { value -> value >= 0L } ?: return@LabeledInputField
                        }
                        onSimulationInputChange(
                            simulationInput.copy(amount = parsed),
                        )
                    },
                    modifier = Modifier.width(120.dp),
                    keyboardType = KeyboardType.Decimal,
                )
            }

            BcButton(
                text = "Run Simulation",
                onClick = onRunSimulation,
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.Primary,
            )
        }
    }
}

@Composable
private fun LabeledInputField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    prefix: String? = null,
    keyboardType: KeyboardType,
) {
    val colors = BudgetCalendarTheme.colors
    val spacing = BudgetCalendarTheme.spacing
    val typography = BudgetCalendarTheme.typography
    val radius = BudgetCalendarTheme.radius

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
    ) {
        Text(
            text = label,
            style = typography.bodyMedium,
            color = colors.textSecondary,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(1.5.dp, colors.borderStrong, RoundedCornerShape(radius.lg))
                .padding(horizontal = spacing.spacing3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.spacing2),
        ) {
            if (prefix != null) {
                Text(
                    text = prefix,
                    style = typography.bodyLarge,
                    color = colors.textPrimary,
                )
            }

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
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
        }
    }
}

@Composable
fun ConsequencesSection(
    result: ConsequencesResult?,
    modifier: Modifier = Modifier,
) {
    val colors = BudgetCalendarTheme.colors
    val radius = BudgetCalendarTheme.radius

    AnimatedVisibility(visible = result != null) {
        if (result == null) {
            return@AnimatedVisibility
        }

        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BcSectionHeader(
                title = "Consequences",
                modifier = Modifier.fillMaxWidth(),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bgSurface, RoundedCornerShape(radius.xl)),
            ) {
                ConsequenceRow(
                    label = "New Safe to Spend",
                    value = CurrencyUtils.formatCents(result.newSafeToSpend),
                    subtitle = "After simulated expense",
                    valueColor = if (result.newSafeToSpend < 0L) colors.colorError else colors.textPrimary,
                )
                HorizontalDivider(color = colors.borderStrong)
                ConsequenceRow(
                    label = "Daily Velocity Impact",
                    value = "${CurrencyUtils.formatCents(result.dailyVelocityImpact, includePlusSign = true)}/day",
                    subtitle = "Safe to spend change per day",
                    valueColor = if (result.dailyVelocityImpact < 0L) colors.colorError else colors.textPrimary,
                )
                HorizontalDivider(color = colors.borderStrong)
                ConsequenceRow(
                    label = "Days of Runway",
                    value = "${result.daysOfRunway} days",
                    subtitle = "Until Safe to Spend reaches zero",
                    valueColor = if (result.daysOfRunway == 0) colors.colorError else colors.textPrimary,
                )
                HorizontalDivider(color = colors.borderStrong)
                ConsequenceRow(
                    label = "Affordability",
                    value = if (result.isAffordable) "Affordable" else "Cannot afford",
                    subtitle = null,
                    valueColor = if (result.isAffordable) colors.colorSuccess else colors.colorError,
                )
            }
        }
    }
}

@Composable
private fun ConsequenceRow(
    label: String,
    value: String,
    subtitle: String?,
    valueColor: Color,
) {
    val colors = BudgetCalendarTheme.colors
    val spacing = BudgetCalendarTheme.spacing
    val typography = BudgetCalendarTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(spacing.spacing4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.spacing3),
    ) {
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(24.dp)
                .border(1.5.dp, colors.borderStrong, CircleShape),
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }

        Text(
            text = value,
            style = typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = valueColor,
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
