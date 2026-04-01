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
import com.petergabriel.budgetcalendar.features.accounts.presentation.AccountScreen
import com.petergabriel.budgetcalendar.features.calendar.presentation.CalendarScreen
import com.petergabriel.budgetcalendar.features.home.presentation.HomeScreen
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import com.petergabriel.budgetcalendar.features.transactions.domain.usecase.MarkOverdueTransactionsUseCase
import com.petergabriel.budgetcalendar.features.transactions.presentation.TransactionFormViewModel
import com.petergabriel.budgetcalendar.features.transactions.presentation.components.TransactionFormSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

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
        var hasSkippedFirstResume by remember { mutableStateOf(false) }
        val transactionFormViewModel: TransactionFormViewModel = koinViewModel()
        val markOverdueTransactionsUseCase: MarkOverdueTransactionsUseCase = koinInject()
        val formState by transactionFormViewModel.uiState.collectAsStateWithLifecycle()
        val lifecycleOwner = LocalLifecycleOwner.current
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            withContext(Dispatchers.Default) {
                markOverdueTransactionsUseCase(DateUtils.nowMillis())
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
                    markOverdueTransactionsUseCase(DateUtils.nowMillis())
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
                )

                AppTab.CALENDAR -> CalendarScreen(
                    modifier = Modifier.padding(innerPadding),
                )

                AppTab.ACCOUNTS -> AccountScreen(
                    modifier = Modifier.padding(innerPadding),
                )

                AppTab.ME -> Box(modifier = Modifier.padding(innerPadding))
            }
        }

        TransactionFormSheet(
            isVisible = showAddForm,
            uiState = formState,
            initialDateMillis = remember { DateUtils.nowMillis() },
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
    }
}
