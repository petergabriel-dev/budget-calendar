package com.petergabriel.budgetcalendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.petergabriel.budgetcalendar.core.designsystem.component.BcTabBarPill
import com.petergabriel.budgetcalendar.core.designsystem.component.BcTabItem
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.budget.domain.usecase.CalculateSafeToSpendUseCase
import com.petergabriel.budgetcalendar.features.accounts.presentation.AccountScreen
import com.petergabriel.budgetcalendar.features.calendar.presentation.CalendarScreen
import com.petergabriel.budgetcalendar.features.calendar.domain.usecase.CalculateMonthProjectionUseCase
import com.petergabriel.budgetcalendar.features.home.presentation.HomeScreen
import com.petergabriel.budgetcalendar.features.notifications.domain.usecase.GenerateNotificationsUseCase
import com.petergabriel.budgetcalendar.features.notifications.domain.usecase.GetUserSettingsUseCase
import com.petergabriel.budgetcalendar.features.notifications.presentation.NotificationCenterSheet
import com.petergabriel.budgetcalendar.features.notifications.presentation.NotificationCenterViewModel
import com.petergabriel.budgetcalendar.features.notifications.presentation.NotificationSettingsScreen
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import com.petergabriel.budgetcalendar.features.transactions.domain.usecase.GetOverdueTransactionsUseCase
import com.petergabriel.budgetcalendar.features.transactions.domain.usecase.MarkOverdueTransactionsUseCase
import com.petergabriel.budgetcalendar.features.transactions.presentation.TransactionFormViewModel
import com.petergabriel.budgetcalendar.features.transactions.presentation.components.TransactionFormSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Instant

private enum class AppTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Outlined.Home),
    CALENDAR("Calendar", Icons.Outlined.DateRange),
    ACCOUNTS("Accounts", Icons.Outlined.CreditCard),
    ME("Me", Icons.Outlined.Person),
}

@Composable
@Preview
fun App() {
    BudgetCalendarTheme {
        var selectedTab by rememberSaveable { mutableStateOf(AppTab.HOME) }
        var showAddForm by remember { mutableStateOf(false) }
        var showNotificationCenter by remember { mutableStateOf(false) }
        var hasSkippedFirstResume by remember { mutableStateOf(false) }
        val transactionFormViewModel: TransactionFormViewModel = koinViewModel()
        val notificationCenterViewModel: NotificationCenterViewModel = koinViewModel()
        val markOverdueTransactionsUseCase: MarkOverdueTransactionsUseCase = koinInject()
        val getOverdueTransactionsUseCase: GetOverdueTransactionsUseCase = koinInject()
        val calculateMonthProjectionUseCase: CalculateMonthProjectionUseCase = koinInject()
        val calculateSafeToSpendUseCase: CalculateSafeToSpendUseCase = koinInject()
        val getUserSettingsUseCase: GetUserSettingsUseCase = koinInject()
        val generateNotificationsUseCase: GenerateNotificationsUseCase = koinInject()
        val formState by transactionFormViewModel.uiState.collectAsStateWithLifecycle()
        val unreadCount by notificationCenterViewModel.unreadCount.collectAsStateWithLifecycle(initialValue = 0)
        val lifecycleOwner = LocalLifecycleOwner.current
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            withContext(Dispatchers.Default) {
                refreshNotifications(
                    markOverdueTransactionsUseCase = markOverdueTransactionsUseCase,
                    getOverdueTransactionsUseCase = getOverdueTransactionsUseCase,
                    calculateMonthProjectionUseCase = calculateMonthProjectionUseCase,
                    calculateSafeToSpendUseCase = calculateSafeToSpendUseCase,
                    getUserSettingsUseCase = getUserSettingsUseCase,
                    generateNotificationsUseCase = generateNotificationsUseCase,
                )
            }
        }

        DisposableEffect(lifecycleOwner, markOverdueTransactionsUseCase) {
            val observer = LifecycleEventObserver { _, event ->
                if (event != Lifecycle.Event.ON_RESUME) {
                    return@LifecycleEventObserver
                }

                if (!hasSkippedFirstResume) {
                    hasSkippedFirstResume = true
                    return@LifecycleEventObserver
                }

                coroutineScope.launch(Dispatchers.Default) {
                    refreshNotifications(
                        markOverdueTransactionsUseCase = markOverdueTransactionsUseCase,
                        getOverdueTransactionsUseCase = getOverdueTransactionsUseCase,
                        calculateMonthProjectionUseCase = calculateMonthProjectionUseCase,
                        calculateSafeToSpendUseCase = calculateSafeToSpendUseCase,
                        getUserSettingsUseCase = getUserSettingsUseCase,
                        generateNotificationsUseCase = generateNotificationsUseCase,
                    )
                }
            }

            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        Scaffold(
            containerColor = BudgetCalendarTheme.colors.bgPrimary,
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    BcTabBarPill(
                        tabs = AppTab.entries.map { tab ->
                            BcTabItem(
                                label = tab.label,
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = tab.icon,
                            )
                        },
                        onAddClick = {
                            transactionFormViewModel.setType(TransactionType.EXPENSE)
                            showAddForm = true
                        },
                    )
                }
            },
        ) { innerPadding ->
            when (selectedTab) {
                AppTab.HOME -> HomeScreen(
                    modifier = Modifier.padding(innerPadding),
                    unreadCount = unreadCount,
                    onBellClick = { showNotificationCenter = true },
                )

                AppTab.CALENDAR -> CalendarScreen(
                    modifier = Modifier.padding(innerPadding),
                    unreadCount = unreadCount,
                    onBellClick = { showNotificationCenter = true },
                )

                AppTab.ACCOUNTS -> AccountScreen(
                    modifier = Modifier.padding(innerPadding),
                    unreadCount = unreadCount,
                    onBellClick = { showNotificationCenter = true },
                )

                AppTab.ME -> NotificationSettingsScreen(
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }

        var currentDayMillis by remember { mutableLongStateOf(DateUtils.startOfDayMillis(DateUtils.nowMillis())) }
        LaunchedEffect(Unit) {
            while (true) {
                val delay = DateUtils.millisUntilNextDay()
                kotlinx.coroutines.delay(delay)
                currentDayMillis = DateUtils.startOfDayMillis(DateUtils.nowMillis())
            }
        }

        TransactionFormSheet(
            isVisible = showAddForm,
            uiState = formState,
            initialDateMillis = currentDayMillis,
            initialData = null,
            onSetType = transactionFormViewModel::setType,
            onSave = { request ->
                transactionFormViewModel.submit(request)
                showAddForm = false
            },
            onCancel = {
                transactionFormViewModel.clearError()
                showAddForm = false
            },
            onDelete = null,
        )

        NotificationCenterSheet(
            isVisible = showNotificationCenter,
            viewModel = notificationCenterViewModel,
            onDismiss = { showNotificationCenter = false },
        )
    }
}

private suspend fun refreshNotifications(
    markOverdueTransactionsUseCase: MarkOverdueTransactionsUseCase,
    getOverdueTransactionsUseCase: GetOverdueTransactionsUseCase,
    calculateMonthProjectionUseCase: CalculateMonthProjectionUseCase,
    calculateSafeToSpendUseCase: CalculateSafeToSpendUseCase,
    getUserSettingsUseCase: GetUserSettingsUseCase,
    generateNotificationsUseCase: GenerateNotificationsUseCase,
) {
    val nowMillis = DateUtils.nowMillis()
    val timeZone = TimeZone.currentSystemDefault()
    val today = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(timeZone).date
    val currentYearMonth = YearMonth(today.year, today.month.number)

    markOverdueTransactionsUseCase(nowMillis)

    val overdueTransactions = getOverdueTransactionsUseCase().first()
    val projectedRollover = calculateMonthProjectionUseCase(currentYearMonth).first()
    val budgetSummary = calculateSafeToSpendUseCase().first()
    val thresholdPercent = getUserSettingsUseCase().first()?.stsLowThresholdPercent ?: 15

    generateNotificationsUseCase(
        overdueTransactions = overdueTransactions,
        daysRemaining = DateUtils.daysRemainingInMonth(today = today, tz = timeZone),
        projectedRollover = projectedRollover,
        currentSts = budgetSummary.availableToSpend,
        poolBalance = budgetSummary.totalLiquidAssets,
        thresholdPercent = thresholdPercent,
        nowMillis = nowMillis,
        timeZone = timeZone,
    )
}
