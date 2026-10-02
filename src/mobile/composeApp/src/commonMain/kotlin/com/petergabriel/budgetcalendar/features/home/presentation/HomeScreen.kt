package com.petergabriel.budgetcalendar.features.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.petergabriel.budgetcalendar.core.designsystem.component.BcHeader
import com.petergabriel.budgetcalendar.core.designsystem.component.BcMetricHero
import com.petergabriel.budgetcalendar.core.designsystem.component.BcMetricHeroHierarchy
import com.petergabriel.budgetcalendar.core.designsystem.component.BcWeekDay
import com.petergabriel.budgetcalendar.core.designsystem.component.BcNotificationBell
import com.petergabriel.budgetcalendar.core.designsystem.component.BcSectionHeader
import com.petergabriel.budgetcalendar.core.designsystem.component.BcSegmentedControl
import com.petergabriel.budgetcalendar.core.designsystem.component.HeroSafeToSpend
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.budget.presentation.BudgetViewModel
import com.petergabriel.budgetcalendar.features.sandbox.presentation.components.AddSandboxTransactionSheet
import com.petergabriel.budgetcalendar.features.sandbox.presentation.components.ComparisonDeltaRow
import com.petergabriel.budgetcalendar.features.sandbox.presentation.components.HeroProjectedSpend
import com.petergabriel.budgetcalendar.features.sandbox.presentation.components.SandboxEmptySnapshotState
import com.petergabriel.budgetcalendar.features.sandbox.presentation.components.SandboxScopedError
import com.petergabriel.budgetcalendar.features.sandbox.presentation.components.SandboxTransactionList
import com.petergabriel.budgetcalendar.features.sandbox.presentation.SandboxViewModel
import com.petergabriel.budgetcalendar.features.sandbox.presentation.components.SnapshotSelectorPill
import com.petergabriel.budgetcalendar.features.sandbox.presentation.components.SnapshotSelectorSheet
import com.petergabriel.budgetcalendar.features.home.presentation.components.HomeWeeklyActivity
import com.petergabriel.budgetcalendar.features.home.presentation.components.OverdueTransactionRow
import com.petergabriel.budgetcalendar.features.home.presentation.components.SelectedDayTransactionPreviewState
import com.petergabriel.budgetcalendar.features.home.presentation.components.WeeklySpendSummaryState
import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.transactions.presentation.TransactionFormEffect
import com.petergabriel.budgetcalendar.features.transactions.presentation.TransactionFormViewModel
import com.petergabriel.budgetcalendar.features.transactions.presentation.TransactionViewModel
import com.petergabriel.budgetcalendar.features.transactions.presentation.components.TransactionDetailsSheet
import com.petergabriel.budgetcalendar.features.transactions.presentation.components.TransactionFormSheet
import kotlinx.coroutines.flow.collect
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    unreadCount: Int,
    onBellClick: () -> Unit,
    onViewAllSelected: (LocalDate) -> Unit = {},
    onSandboxModeChanged: (Boolean) -> Unit = {},
    homeViewModel: HomeViewModel = koinViewModel(),
    budgetViewModel: BudgetViewModel = koinViewModel(),
    transactionViewModel: TransactionViewModel = koinViewModel(),
    transactionFormViewModel: TransactionFormViewModel = koinViewModel(),
    sandboxViewModel: SandboxViewModel = koinViewModel(),
) {
    val colors = BudgetCalendarTheme.colors
    val spacing = BudgetCalendarTheme.spacing

    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val budgetState by budgetViewModel.uiState.collectAsStateWithLifecycle()
    val transactionState by transactionViewModel.uiState.collectAsStateWithLifecycle()
    val formState by transactionFormViewModel.uiState.collectAsStateWithLifecycle()
    val sandboxState by sandboxViewModel.uiState.collectAsStateWithLifecycle()

    val tz = TimeZone.currentSystemDefault()
    val today = Clock.System.now().toLocalDateTime(tz).date
    val daysRemainingInMonth = DateUtils.daysRemainingInMonth(today = today, tz = tz)
    val isLastDayOfMonth = daysRemainingInMonth == 1
    val weekDates = remember(today) { homeWeekDates(today) }

    var selectedDate by remember(today) { mutableStateOf(today) }
    var showAddForm by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    var currentDayMillis by remember { mutableLongStateOf(DateUtils.startOfDayMillis(DateUtils.nowMillis())) }

    val accountNames = remember(formState.availableAccounts) {
        formState.availableAccounts.associate { account -> account.id to account.name }
    }
    val weekTransactions = remember(transactionState.transactions, weekDates, tz) {
        transactionState.transactions.filter { transaction ->
            transactionLocalDate(transaction, tz) in weekDates.first()..weekDates.last() &&
                transaction.status != TransactionStatus.CANCELLED
        }
    }
    val weekDays = remember(weekDates, selectedDate, weekTransactions) {
        weekDates.map { date ->
            val dayCount = weekTransactions.count { transaction -> transactionLocalDate(transaction, tz) == date }
            val dateLabel = homeDateLabel(date)
            BcWeekDay(
                id = date.toString(),
                weekdayLabel = date.dayOfWeek.name.take(3),
                dayOfMonth = date.dayOfMonth,
                accessibilityLabel = buildString {
                    append(dateLabel)
                    if (date == selectedDate) append(", selected")
                    append(", $dayCount ${if (dayCount == 1) "transaction" else "transactions"}")
                },
                isSelected = date == selectedDate,
                isToday = date == today,
                hasTransactions = dayCount > 0,
            )
        }
    }
    val selectedTransactions = remember(transactionState.transactions, selectedDate, tz) {
        selectedDayTransactions(transactionState.transactions, selectedDate, tz)
    }
    val overdueTransactions = remember(transactionState.transactions) {
        orderedOverdueTransactions(transactionState.transactions)
    }
    val pendingCount = remember(transactionState.transactions) {
        transactionState.transactions.count { it.status == TransactionStatus.PENDING }
    }
    val weeklySpent = remember(transactionState.transactions, weekDates, tz) {
        weeklyExpenseTotal(transactionState.transactions, weekDates.first(), tz)
    }
    val selectedPreviewItems = remember(selectedTransactions, accountNames, selectedDate) {
        selectedTransactions.take(3).map { transaction ->
            previewItem(
                transaction = transaction,
                selectedDate = selectedDate,
                accountNames = accountNames,
            )
        }
    }
    val selectedPreviewState = SelectedDayTransactionPreviewState(
        dateLabel = homeDateLabel(selectedDate),
        transactionCount = selectedTransactions.size,
        items = selectedPreviewItems,
        hasMore = selectedTransactions.size > selectedPreviewItems.size,
        isLoading = transactionState.isLoading,
        errorMessage = transactionState.error,
    )

    fun closeTransactionForm() {
        showAddForm = false
        editingTransaction = null
        selectedTransaction = null
    }

    fun openTransactionDetails(transaction: Transaction) {
        showAddForm = false
        editingTransaction = null
        selectedTransaction = transaction
    }

    LaunchedEffect(transactionFormViewModel) {
        transactionFormViewModel.effects.collect { effect ->
            if (effect == TransactionFormEffect.MutationSucceeded) {
                closeTransactionForm()
            }
        }
    }

    LaunchedEffect(today) {
        selectedDate = today
    }

    LaunchedEffect(Unit) {
        while (true) {
            val delay = DateUtils.millisUntilNextDay()
            kotlinx.coroutines.delay(delay)
            currentDayMillis = DateUtils.startOfDayMillis(DateUtils.nowMillis())
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            sandboxViewModel.setSandboxMode(false)
            onSandboxModeChanged(false)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgPrimary),
        contentPadding = PaddingValues(
            start = spacing.spacing4,
            end = spacing.spacing4,
            top = spacing.spacing4,
            bottom = spacing.spacing12 * 2,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.spacing4),
    ) {
        item {
            BcHeader(
                title = if (sandboxState.isSandboxMode) "Sandbox" else "Budget",
                modifier = Modifier.fillMaxWidth(),
                actionContent = {
                    BcNotificationBell(
                        unreadCount = unreadCount,
                        onClick = onBellClick,
                    )
                },
            )
        }

        item {
            BcSegmentedControl(
                options = listOf("Live Budget", "Sandbox"),
                selectedIndex = if (sandboxState.isSandboxMode) 1 else 0,
                onSelectedIndexChange = { index ->
                    val isSandboxMode = index == 1
                    sandboxViewModel.setSandboxMode(isSandboxMode)
                    onSandboxModeChanged(isSandboxMode)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (!sandboxState.isSandboxMode) {
            item {
                HeroSafeToSpend(
                    amount = budgetState.budgetSummary.availableToSpend,
                    dailyRate = if (budgetState.budgetSummary.availableToSpend > 0L) {
                        budgetState.budgetSummary.availableToSpend / daysRemainingInMonth
                    } else {
                        null
                    },
                    daysRemaining = daysRemainingInMonth,
                    isLastDayOfMonth = isLastDayOfMonth,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            homeState.availableCreditForSpending?.let { availableCredit ->
                item {
                    BcMetricHero(
                        label = "AVAILABLE CREDIT",
                        amount = availableCredit,
                        hierarchy = BcMetricHeroHierarchy.Secondary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item {
                HomeWeeklyActivity(
                    weeklySpend = WeeklySpendSummaryState(
                        amount = weeklySpent,
                        isLoading = transactionState.isLoading,
                        errorMessage = transactionState.error,
                    ),
                    weekDays = weekDays,
                    selectedDayPreview = selectedPreviewState,
                    onDaySelected = { id ->
                        weekDates.firstOrNull { it.toString() == id }?.let { selectedDate = it }
                    },
                    onTransactionClick = { id ->
                        transactionState.transactions.firstOrNull { it.id == id }?.let(::openTransactionDetails)
                    },
                    onViewAllClick = { id ->
                        weekDates.firstOrNull { it.toString() == id }?.let(onViewAllSelected)
                    },
                    onRetryWeeklySpend = transactionViewModel::retry,
                    onRetryPreview = transactionViewModel::retry,
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
                    OverdueTransactionRow(
                        transaction = transaction,
                        accountName = accountNames[transaction.accountId] ?: "Account #${transaction.accountId}",
                        destinationAccountName = transaction.destinationAccountId?.let { id -> accountNames[id] },
                        relativeDateLabel = relativeHomeDateLabel(
                            transactionDate = transactionLocalDate(transaction, tz),
                            today = today,
                        ),
                        onTap = { openTransactionDetails(transaction) },
                    )
                }
            }
        } else {
            item {
                SnapshotSelectorPill(
                    activeSnapshot = sandboxState.activeSnapshot,
                    onTap = sandboxViewModel::showSnapshotSheet,
                    isLoading = sandboxState.isLoadingSnapshots,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            val snapshotError = sandboxState.snapshotError
            if (!snapshotError.isNullOrBlank()) {
                item {
                    SandboxScopedError(
                        message = snapshotError,
                        onRetry = { sandboxViewModel.loadAvailableSnapshots(autoSelect = true) },
                    )
                }
            }

            val activeSnapshot = sandboxState.activeSnapshot
            if (activeSnapshot == null) {
                if (!sandboxState.isLoadingSnapshots) {
                    item {
                        SandboxEmptySnapshotState(
                            onSelectSnapshot = sandboxViewModel::showSnapshotSheet,
                        )
                    }
                }
            } else {
                item {
                    HeroProjectedSpend(
                        projectedSafeToSpend = sandboxState.projectedSafeToSpend,
                        dailyRate = sandboxState.currentDailyRate,
                        isLoading = sandboxState.isLoadingProjection,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                val projectionError = sandboxState.projectionError
                if (!projectionError.isNullOrBlank()) {
                    item {
                        SandboxScopedError(
                            message = projectionError,
                            onRetry = sandboxViewModel::retrySelectedSnapshot,
                        )
                    }
                }

                val comparison = sandboxState.comparison
                if (comparison != null) {
                    item {
                        ComparisonDeltaRow(
                            comparison = comparison,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                item {
                    SandboxTransactionList(
                        transactions = sandboxState.sandboxTransactions,
                        onAddTap = sandboxViewModel::showAddTransactionSheet,
                        onPromote = sandboxViewModel::promoteTransaction,
                        onRemove = sandboxViewModel::removeTransaction,
                        isLoading = sandboxState.isLoadingTransactions,
                        errorMessage = sandboxState.transactionError,
                        onRetry = sandboxViewModel::retrySelectedSnapshot,
                        removingTransactionIds = sandboxState.removingTransactionIds,
                        promotingTransactionIds = sandboxState.promotingTransactionIds,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            val sandboxMutationError = sandboxState.mutationError
            if (!sandboxMutationError.isNullOrBlank()) {
                item {
                    Text(
                        text = sandboxMutationError,
                        style = BudgetCalendarTheme.typography.bodyMedium,
                        color = colors.colorErrorText,
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
        onDelete = sandboxViewModel::deleteSandbox,
        onCreateNew = sandboxViewModel::createSandbox,
        onDismiss = sandboxViewModel::hideSnapshotSheet,
        isBusy = sandboxState.isSnapshotMutationBusy || sandboxState.isLoadingProjection,
        errorMessage = sandboxState.snapshotError,
    )

    AddSandboxTransactionSheet(
        isVisible = sandboxState.isAddTransactionSheetVisible,
        snapshotId = sandboxState.activeSnapshot?.id,
        accounts = formState.availableAccounts,
        onAdd = sandboxViewModel::addTransaction,
        onDismiss = sandboxViewModel::hideAddTransactionSheet,
        isSubmitting = sandboxState.isAddingTransaction,
        errorMessage = sandboxState.mutationError,
    )

    TransactionFormSheet(
        isVisible = showAddForm,
        uiState = formState,
        initialDateMillis = currentDayMillis,
        initialData = editingTransaction,
        onSetType = transactionFormViewModel::setType,
        onSave = { request ->
            val editing = editingTransaction
            if (editing == null) {
                transactionFormViewModel.submit(request)
            } else {
                transactionFormViewModel.replace(existingTransactionId = editing.id, request = request)
            }
        },
        onCancel = {
            transactionFormViewModel.clearError()
            closeTransactionForm()
        },
        onDelete = { transaction ->
            transactionFormViewModel.delete(transaction.id)
        },
    )

    selectedTransaction?.let { transaction ->
        TransactionDetailsSheet(
            isVisible = true,
            transaction = transaction,
            accountName = accountNames[transaction.accountId] ?: "Account #${transaction.accountId}",
            destinationAccountName = transaction.destinationAccountId?.let { id -> accountNames[id] },
            onDismiss = { selectedTransaction = null },
            onEdit = { editable ->
                selectedTransaction = null
                editingTransaction = editable
                transactionFormViewModel.setType(editable.type)
                showAddForm = true
            },
            onDelete = { deletable ->
                transactionFormViewModel.delete(deletable.id)
            },
        )
    }

}
