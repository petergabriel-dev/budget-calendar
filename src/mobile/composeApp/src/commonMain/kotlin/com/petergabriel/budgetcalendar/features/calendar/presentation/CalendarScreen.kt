package com.petergabriel.budgetcalendar.features.calendar.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.petergabriel.budgetcalendar.core.designsystem.component.BcHeader
import com.petergabriel.budgetcalendar.core.designsystem.component.BcNotificationBell
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.features.calendar.presentation.components.CalendarGrid
import com.petergabriel.budgetcalendar.features.calendar.presentation.components.DayTransactionSectionHeader
import com.petergabriel.budgetcalendar.features.calendar.presentation.components.DayTransactionList
import com.petergabriel.budgetcalendar.features.calendar.presentation.components.EndOfMonthProjectionPill
import com.petergabriel.budgetcalendar.features.calendar.presentation.components.MonthNavigationRow
import com.petergabriel.budgetcalendar.features.calendar.presentation.components.formatDayTransactionHeaderLabel
import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import com.petergabriel.budgetcalendar.features.transactions.presentation.TransactionFormViewModel
import com.petergabriel.budgetcalendar.features.transactions.presentation.components.TransactionDetailsSheet
import com.petergabriel.budgetcalendar.features.transactions.presentation.components.TransactionFormSheet
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    unreadCount: Int,
    onBellClick: () -> Unit,
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

    val selectedDateMillis = remember(uiState.selectedDate) {
        uiState.selectedDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
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

            Spacer(modifier = Modifier.height(spacing.spacing2))

            EndOfMonthProjectionPill(
                projectionAmount = uiState.endOfMonthProjection,
            )

            Spacer(modifier = Modifier.height(spacing.spacing2))

            CalendarGrid(
                days = uiState.calendarMonth?.days.orEmpty(),
                onDateSelected = viewModel::selectDate,
                modifier = Modifier.weight(1f),
            )

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
                .heightIn(min = 96.dp, max = 112.dp),
            accountNameProvider = { accountId ->
                uiState.accountNamesById[accountId] ?: "Account #$accountId"
            },
            maxItems = 1,
            onTransactionTap = openTransactionDetails,
        )

        if (uiState.isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.spacing3),
            )
        }

        if (uiState.error != null) {
            Text(
                text = uiState.error ?: "Unknown error",
                style = typography.bodySmall,
                color = colors.colorError,
                modifier = Modifier.padding(top = spacing.spacing3),
            )
        }
    }

    if (showExpandSheet) {
        val expandSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showExpandSheet = false },
            sheetState = expandSheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = spacing.spacing4, vertical = spacing.spacing2),
            ) {
                Text(
                    text = formatDayTransactionHeaderLabel(selectedDate = uiState.selectedDate),
                    style = typography.cardTitle,
                    color = colors.textPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = spacing.spacing2),
                )

                DayTransactionList(
                    transactions = uiState.selectedDayTransactions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    accountNameProvider = { accountId ->
                        uiState.accountNamesById[accountId] ?: "Account #$accountId"
                    },
                    onTransactionTap = openTransactionDetails,
                )
            }
        }
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
            showTransactionForm = false
            editingTransaction = null
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
                selectedTransaction = null
            },
        )
    }
}
