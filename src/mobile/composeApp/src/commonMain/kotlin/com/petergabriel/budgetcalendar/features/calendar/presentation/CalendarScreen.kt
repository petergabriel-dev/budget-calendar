package com.petergabriel.budgetcalendar.features.calendar.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.petergabriel.budgetcalendar.core.designsystem.component.BcButton
import com.petergabriel.budgetcalendar.core.designsystem.component.BcHeader
import com.petergabriel.budgetcalendar.core.designsystem.component.BcNotificationBell
import com.petergabriel.budgetcalendar.core.designsystem.component.BcSheetShell
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.calendar.presentation.components.CalendarGrid
import com.petergabriel.budgetcalendar.features.calendar.presentation.components.DayTransactionSectionHeader
import com.petergabriel.budgetcalendar.features.calendar.presentation.components.DayTransactionList
import com.petergabriel.budgetcalendar.features.calendar.presentation.components.EndOfMonthProjectionPill
import com.petergabriel.budgetcalendar.features.calendar.presentation.components.MonthNavigationRow
import com.petergabriel.budgetcalendar.features.calendar.presentation.components.formatDayTransactionHeaderLabel
import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import com.petergabriel.budgetcalendar.features.transactions.presentation.TransactionFormEffect
import com.petergabriel.budgetcalendar.features.transactions.presentation.TransactionFormViewModel
import com.petergabriel.budgetcalendar.features.transactions.presentation.components.TransactionDetailsSheet
import com.petergabriel.budgetcalendar.features.transactions.presentation.components.TransactionFormSheet
import com.petergabriel.budgetcalendar.features.transactions.presentation.components.TransactionItemDensity
import kotlinx.coroutines.flow.collect
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    unreadCount: Int,
    onBellClick: () -> Unit,
    pendingSelectedDate: LocalDate? = null,
    onPendingSelectedDateConsumed: () -> Unit = {},
    viewModel: CalendarViewModel = koinViewModel(),
    transactionFormViewModel: TransactionFormViewModel = koinViewModel(),
) {
    val spacing = BudgetCalendarTheme.spacing
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val transactionFormUiState by transactionFormViewModel.uiState.collectAsStateWithLifecycle()

    var horizontalDragTotal by remember { mutableFloatStateOf(0f) }
    var showTransactionForm by remember { mutableStateOf(false) }
    var showExpandSheet by remember { mutableStateOf(false) }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }

    LaunchedEffect(pendingSelectedDate) {
        pendingSelectedDate?.let { date ->
            viewModel.selectDate(date)
            onPendingSelectedDateConsumed()
        }
    }

    LaunchedEffect(transactionFormViewModel) {
        transactionFormViewModel.effects.collect { effect ->
            if (effect == TransactionFormEffect.MutationSucceeded) {
                showTransactionForm = false
                editingTransaction = null
                selectedTransaction = null
            }
        }
    }

    val selectedDateMillis = remember(uiState.selectedDate) {
        val localDayStart = uiState.selectedDate
            .atStartOfDayIn(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()
        DateUtils.toUtcDayStartMillis(localDayStart)
    }

    val openTransactionDetails: (Transaction) -> Unit = { transaction ->
        showExpandSheet = false
        showTransactionForm = false
        editingTransaction = null
        selectedTransaction = transaction
    }

    val openTransactionForm: (Transaction) -> Unit = { transaction ->
        selectedTransaction = null
        editingTransaction = transaction
        transactionFormViewModel.setType(transaction.type)
        showTransactionForm = true
    }

    val swipeModifier = if (showExpandSheet) {
        Modifier
    } else {
        Modifier.pointerInput(uiState.currentMonth) {
            detectHorizontalDragGestures(
                onHorizontalDrag = { _, dragAmount ->
                    horizontalDragTotal += dragAmount
                },
                onDragEnd = {
                    when {
                        horizontalDragTotal > 70f -> viewModel.navigateMonth(-1)
                        horizontalDragTotal < -70f -> viewModel.navigateMonth(1)
                    }
                    horizontalDragTotal = 0f
                },
                onDragCancel = { horizontalDragTotal = 0f },
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgPrimary)
            .then(swipeModifier)
            .padding(spacing.spacing4),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            BcHeader(
                title = "Calendar",
                modifier = Modifier.fillMaxWidth(),
                actionContent = {
                    BcNotificationBell(
                        unreadCount = unreadCount,
                        onClick = onBellClick,
                    )
                },
            )

            MonthNavigationRow(
                currentMonth = uiState.currentMonth,
                onPreviousMonth = { viewModel.navigateMonth(-1) },
                onNextMonth = { viewModel.navigateMonth(1) },
            )

            EndOfMonthProjectionPill(
                projectionAmount = uiState.endOfMonthProjection,
                isLoading = uiState.isProjectionLoading,
                errorMessage = uiState.projectionError,
                onRetry = viewModel::retryProjection,
                modifier = Modifier.padding(top = spacing.spacing2),
            )

            CalendarGrid(
                days = uiState.calendarMonth?.days.orEmpty(),
                onDateSelected = viewModel::selectDate,
                modifier = Modifier.weight(1f),
            )

            uiState.monthError?.let { errorMessage ->
                CalendarInlineError(
                    message = errorMessage,
                    retryContentDescription = "Retry loading ${uiState.currentMonth} calendar",
                    onRetry = { viewModel.loadMonth(uiState.currentMonth) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            DayTransactionSectionHeader(
                selectedDate = uiState.selectedDate,
                onExpand = {
                    showTransactionForm = false
                    editingTransaction = null
                    showExpandSheet = true
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        DayTransactionList(
            transactions = uiState.selectedDayTransactions,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = spacing.spacing12),
            accountNameProvider = { accountId ->
                uiState.accountNamesById[accountId] ?: "Account #$accountId"
            },
            maxItems = 1,
            density = TransactionItemDensity.Compact,
            isLoading = uiState.isSelectedDayLoading,
            errorMessage = uiState.selectedDayError,
            onRetry = { viewModel.selectDate(uiState.selectedDate) },
            onTransactionTap = openTransactionDetails,
        )
    }

    if (showExpandSheet) {
        BcSheetShell(
            title = formatDayTransactionHeaderLabel(selectedDate = uiState.selectedDate),
            onDismissRequest = { showExpandSheet = false },
            bodyScrollable = false,
            body = {
                DayTransactionList(
                transactions = uiState.selectedDayTransactions,
                modifier = Modifier.weight(1f),
                accountNameProvider = { accountId ->
                    uiState.accountNamesById[accountId] ?: "Account #$accountId"
                },
                density = TransactionItemDensity.Compact,
                isLoading = uiState.isSelectedDayLoading,
                errorMessage = uiState.selectedDayError,
                onRetry = { viewModel.selectDate(uiState.selectedDate) },
                    onTransactionTap = openTransactionDetails,
                )
            },
        )
    }

    TransactionFormSheet(
        isVisible = showTransactionForm,
        uiState = transactionFormUiState,
        initialDateMillis = selectedDateMillis,
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
            showTransactionForm = false
            editingTransaction = null
        },
    )

    selectedTransaction?.let { transaction ->
        TransactionDetailsSheet(
            isVisible = true,
            transaction = transaction,
            accountName = uiState.accountNamesById[transaction.accountId] ?: "Account #${transaction.accountId}",
            destinationAccountName = transaction.destinationAccountId?.let { accountId ->
                uiState.accountNamesById[accountId] ?: "Account #$accountId"
            },
            onDismiss = { selectedTransaction = null },
            onEdit = openTransactionForm,
            onDelete = { transactionToDelete ->
                transactionFormViewModel.delete(transactionToDelete.id)
            },
        )
    }
}

@Composable
private fun CalendarInlineError(
    message: String,
    retryContentDescription: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BudgetCalendarTheme.colors
    val spacing = BudgetCalendarTheme.spacing
    val typography = BudgetCalendarTheme.typography

    Column(
        modifier = modifier.padding(top = spacing.spacing2),
    ) {
        Text(
            text = message,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = typography.bodySmall,
            color = colors.colorErrorText,
        )
        BcButton(
            text = "Retry",
            onClick = onRetry,
            contentDescription = retryContentDescription,
            variant = com.petergabriel.budgetcalendar.core.designsystem.component.ButtonVariant.Outline,
        )
    }
}
