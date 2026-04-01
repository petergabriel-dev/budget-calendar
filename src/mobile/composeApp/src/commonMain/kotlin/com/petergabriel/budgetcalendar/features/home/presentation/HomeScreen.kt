package com.petergabriel.budgetcalendar.features.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.petergabriel.budgetcalendar.core.designsystem.component.BcHeader
import com.petergabriel.budgetcalendar.core.designsystem.component.BcSearchBar
import com.petergabriel.budgetcalendar.core.designsystem.component.BcSectionHeader
import com.petergabriel.budgetcalendar.core.designsystem.component.BcSegmentedControl
import com.petergabriel.budgetcalendar.core.designsystem.component.HeroSafeToSpend
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils
import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.budget.presentation.BudgetViewModel
import com.petergabriel.budgetcalendar.features.home.presentation.components.ConsequencesSection
import com.petergabriel.budgetcalendar.features.home.presentation.components.SimulationFormCard
import com.petergabriel.budgetcalendar.features.home.presentation.components.SnapshotSelectorPill
import com.petergabriel.budgetcalendar.features.home.presentation.components.SnapshotSelectorSheet
import com.petergabriel.budgetcalendar.features.sandbox.presentation.SandboxViewModel
import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import com.petergabriel.budgetcalendar.features.transactions.presentation.TransactionFormViewModel
import com.petergabriel.budgetcalendar.features.transactions.presentation.TransactionViewModel
import com.petergabriel.budgetcalendar.features.transactions.presentation.components.TransactionFormSheet
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock
import kotlin.time.Instant

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    budgetViewModel: BudgetViewModel = koinViewModel(),
    transactionViewModel: TransactionViewModel = koinViewModel(),
    transactionFormViewModel: TransactionFormViewModel = koinViewModel(),
    sandboxViewModel: SandboxViewModel = koinViewModel(),
) {
    val colors = BudgetCalendarTheme.colors
    val spacing = BudgetCalendarTheme.spacing

    val budgetState by budgetViewModel.uiState.collectAsStateWithLifecycle()
    val transactionState by transactionViewModel.uiState.collectAsStateWithLifecycle()
    val formState by transactionFormViewModel.uiState.collectAsStateWithLifecycle()
    val sandboxState by sandboxViewModel.uiState.collectAsStateWithLifecycle()

    val tz = TimeZone.currentSystemDefault()
    val today = remember {
        Clock.System.now().toLocalDateTime(tz).date
    }
    val weekStart = remember(today) {
        today - DatePeriod(days = today.dayOfWeek.ordinal)
    }
    val weekDays = remember(weekStart) {
        (0..6).map { weekStart + DatePeriod(days = it) }
    }
    val weekStartMillis = remember(weekStart) {
        weekStart.atStartOfDayIn(tz).toEpochMilliseconds()
    }
    val weekEndMillis = remember(weekStart) {
        (weekStart + DatePeriod(days = 7)).atStartOfDayIn(tz).toEpochMilliseconds()
    }

    var selectedDate by remember { mutableStateOf(today) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddForm by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }

    val overdueTransactions = remember(transactionState.transactions) {
        transactionState.transactions.filter { it.status == TransactionStatus.OVERDUE && !it.isSandbox }
    }
    val pendingCount = remember(transactionState.transactions) {
        transactionState.transactions.count { it.status == TransactionStatus.PENDING && !it.isSandbox }
    }
    val weeklySpent = remember(transactionState.transactions, weekStartMillis, weekEndMillis) {
        transactionState.transactions
            .filter { txn ->
                txn.date in weekStartMillis until weekEndMillis &&
                    txn.type == TransactionType.EXPENSE &&
                    txn.status != TransactionStatus.CANCELLED &&
                    !txn.isSandbox
            }
            .sumOf { it.amount }
    }

    DisposableEffect(Unit) {
        onDispose { sandboxViewModel.setSandboxMode(false) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgPrimary),
        contentPadding = PaddingValues(
            start = spacing.spacing4,
            end = spacing.spacing4,
            top = spacing.spacing4,
            bottom = 100.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.spacing4),
    ) {
        item {
            BcHeader(
                title = if (sandboxState.isSandboxMode) "Sandbox" else "Budget",
                modifier = Modifier.fillMaxWidth(),
                actionIcon = Icons.Outlined.Notifications,
                actionContentDescription = "Notifications",
                onActionClick = {},
            )
        }

        item {
            BcSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Search...",
            )
        }

        item {
            BcSegmentedControl(
                options = listOf("Live Budget", "Sandbox"),
                selectedIndex = if (sandboxState.isSandboxMode) 1 else 0,
                onSelectedIndexChange = { index ->
                    sandboxViewModel.setSandboxMode(index == 1)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (!sandboxState.isSandboxMode) {
            item {
                HeroSafeToSpend(
                    amount = budgetState.budgetSummary.availableToSpend,
                    dailyRate = if (budgetState.budgetSummary.availableToSpend > 0L) {
                        budgetState.budgetSummary.availableToSpend / 30
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                BcSectionHeader(
                    title = "This Week",
                    actionText = "${CurrencyUtils.formatCents(weeklySpent)} spent",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(spacing.spacing2))
                WeekStrip(
                    weekDays = weekDays,
                    selectedDate = selectedDate,
                    onDateSelected = { selectedDate = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                BcSectionHeader(
                    title = "Overdue",
                    actionText = if (pendingCount > 0) "$pendingCount Pending" else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = spacing.spacing2),
                )
            }

            if (overdueTransactions.isEmpty()) {
                item {
                    Text(
                        text = "No overdue transactions",
                        style = BudgetCalendarTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(vertical = spacing.spacing2),
                    )
                }
            } else {
                items(overdueTransactions, key = { it.id }) { transaction ->
                    ScheduleTransactionRow(
                        transaction = transaction,
                        today = today,
                        tz = tz,
                        onClick = {
                            editingTransaction = transaction
                            transactionFormViewModel.setType(transaction.type)
                            showAddForm = true
                        },
                    )
                    HorizontalDivider(color = colors.borderSubtle)
                }
            }
        } else {
            item {
                SnapshotSelectorPill(
                    activeSnapshot = sandboxState.activeSnapshot,
                    onSnapshotPillTap = sandboxViewModel::showSnapshotSheet,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                HeroSafeToSpend(
                    amount = sandboxState.projectedSafeToSpend,
                    dailyRate = sandboxState.currentDailyRate,
                    label = "PROJECTED SPEND",
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                SimulationFormCard(
                    simulationInput = sandboxState.simulationInput,
                    onSimulationInputChange = sandboxViewModel::updateSimulationInput,
                    onRunSimulation = sandboxViewModel::runSimulation,
                    onClearSimulation = sandboxViewModel::clearSimulation,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                ConsequencesSection(
                    result = sandboxState.consequencesResult,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            val sandboxError = sandboxState.error
            if (!sandboxError.isNullOrBlank()) {
                item {
                    Text(
                        text = sandboxError,
                        style = BudgetCalendarTheme.typography.bodyMedium,
                        color = colors.colorError,
                    )
                }
            }
        }
    }

    SnapshotSelectorSheet(
        isVisible = sandboxState.isSnapshotSheetVisible,
        snapshots = sandboxState.availableSnapshots,
        activeSnapshotId = sandboxState.activeSnapshot?.id,
        onSelect = sandboxViewModel::selectSnapshot,
        onCreateNew = sandboxViewModel::createSandbox,
        onDismiss = sandboxViewModel::hideSnapshotSheet,
    )

    TransactionFormSheet(
        isVisible = showAddForm,
        uiState = formState,
        initialDateMillis = remember { DateUtils.nowMillis() },
        initialData = editingTransaction,
        onSetType = transactionFormViewModel::setType,
        onSave = { request ->
            val editing = editingTransaction
            if (editing == null) {
                transactionFormViewModel.submit(request)
            } else {
                transactionFormViewModel.replace(existingTransactionId = editing.id, request = request)
            }
            showAddForm = false
            editingTransaction = null
        },
        onCancel = {
            transactionFormViewModel.clearError()
            showAddForm = false
            editingTransaction = null
        },
        onDelete = { transaction ->
            transactionFormViewModel.delete(transaction.id)
            showAddForm = false
            editingTransaction = null
        },
    )
}

@Composable
private fun WeekStrip(
    weekDays: List<LocalDate>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        weekDays.forEach { date ->
            val letter = date.dayOfWeek.name.first().toString()
            val isSelected = date == selectedDate
            DayPill(
                letter = letter,
                isSelected = isSelected,
                onClick = { onDateSelected(date) },
            )
        }
    }
}

@Composable
private fun DayPill(
    letter: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val radius = BudgetCalendarTheme.radius

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(radius.lg))
            .background(if (isSelected) colors.bgDark else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = letter,
            style = typography.calendarDay,
            color = if (isSelected) colors.textInverted else colors.textSecondary,
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (isSelected) colors.textInverted else colors.textSecondary),
        )
    }
}

@Composable
private fun ScheduleTransactionRow(
    transaction: Transaction,
    today: LocalDate,
    tz: TimeZone,
    onClick: () -> Unit,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing

    val txnDate = remember(transaction.date) {
        Instant.fromEpochMilliseconds(transaction.date).toLocalDateTime(tz).date
    }
    val relativeLabel = remember(txnDate, today) {
        val daysDiff = today.toEpochDays() - txnDate.toEpochDays()
        when (daysDiff) {
            0L -> "Today"
            1L -> "Yesterday"
            else -> "$daysDiff Days Ago"
        }
    }
    val title = transaction.description?.takeIf { it.isNotBlank() }
        ?: transaction.category?.takeIf { it.isNotBlank() }
        ?: "Transaction"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = spacing.spacing3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.spacing3),
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .border(1.5.dp, colors.borderStrong, CircleShape),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = typography.bodyLarge,
                color = colors.textPrimary,
            )
            Text(
                text = "$relativeLabel · ${CurrencyUtils.formatCents(transaction.amount)}",
                style = typography.bodySmall,
                color = colors.textSecondary,
            )
        }

        Text(
            text = "-${CurrencyUtils.formatCents(transaction.amount)}",
            style = typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary,
        )
    }
}
