package com.petergabriel.budgetcalendar.features.calendar.presentation

import com.petergabriel.budgetcalendar.features.accounts.domain.usecase.GetAccountsUseCase
import com.petergabriel.budgetcalendar.features.accounts.testutil.FakeAccountRepository
import com.petergabriel.budgetcalendar.features.calendar.domain.usecase.BuildCalendarMonthUseCase
import com.petergabriel.budgetcalendar.features.calendar.domain.usecase.CalculateDaySummaryUseCase
import com.petergabriel.budgetcalendar.features.calendar.domain.usecase.CalculateMonthProjectionUseCase
import com.petergabriel.budgetcalendar.features.calendar.domain.usecase.GetMonthTransactionsUseCase
import com.petergabriel.budgetcalendar.features.transactions.domain.model.CreateTransactionRequest
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import com.petergabriel.budgetcalendar.features.transactions.testutil.FakeTransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.YearMonth
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var transactionRepository: FakeTransactionRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        accountRepository = FakeAccountRepository()
        transactionRepository = FakeTransactionRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadMonth_exposesStable42CellPlaceholderBeforeDataArrives() = runTest(dispatcher) {
        val viewModel = calendarViewModel()

        assertEquals(42, viewModel.uiState.value.calendarMonth?.days?.size)
        assertTrue(viewModel.uiState.value.isMonthLoading)
    }

    @Test
    fun loadMonth_keepsSelectedDateOutsideDisplayedMonth() = runTest(dispatcher) {
        val viewModel = calendarViewModel()
        val selectedDate = LocalDate(2026, 8, 1)

        viewModel.selectDate(selectedDate)
        advanceUntilIdle()
        viewModel.loadMonth(YearMonth(2026, 9))
        advanceUntilIdle()

        assertEquals(selectedDate, viewModel.uiState.value.selectedDate)
    }

    @Test
    fun loadMonth_showsOffMonthSelectedDateTransactions() = runTest(dispatcher) {
        val selectedDate = LocalDate(2026, 8, 1)
        val transaction = createTransaction(selectedDate)
        val viewModel = calendarViewModel()

        viewModel.selectDate(selectedDate)
        advanceUntilIdle()
        viewModel.loadMonth(YearMonth(2026, 9))
        advanceUntilIdle()

        assertEquals(listOf(transaction), viewModel.uiState.value.selectedDayTransactions)
    }

    @Test
    fun offMonthSelectedDate_reactsToTransactionWrites() = runTest(dispatcher) {
        val selectedDate = LocalDate(2026, 8, 1)
        val viewModel = calendarViewModel()

        viewModel.selectDate(selectedDate)
        advanceUntilIdle()
        viewModel.loadMonth(YearMonth(2026, 9))
        advanceUntilIdle()
        val transaction = createTransaction(selectedDate)
        advanceUntilIdle()

        assertEquals(listOf(transaction), viewModel.uiState.value.selectedDayTransactions)
    }

    @Test
    fun offMonthSelectedDate_excludesCancelledTransactions() = runTest(dispatcher) {
        val selectedDate = LocalDate(2026, 8, 1)
        createTransaction(selectedDate, TransactionStatus.CANCELLED)
        val viewModel = calendarViewModel()

        viewModel.selectDate(selectedDate)
        advanceUntilIdle()
        viewModel.loadMonth(YearMonth(2026, 9))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.selectedDayTransactions.isEmpty())
    }

    @Test
    fun loadMonth_doesNotClampEndOfMonthSelection() = runTest(dispatcher) {
        val viewModel = calendarViewModel()
        val selectedDate = LocalDate(2026, 1, 31)

        viewModel.selectDate(selectedDate)
        advanceUntilIdle()
        viewModel.loadMonth(YearMonth(2026, 2))
        advanceUntilIdle()

        assertEquals(selectedDate, viewModel.uiState.value.selectedDate)
    }

    private fun calendarViewModel() = CalendarViewModel(
        getMonthTransactionsUseCase = GetMonthTransactionsUseCase(transactionRepository),
        buildCalendarMonthUseCase = BuildCalendarMonthUseCase(CalculateDaySummaryUseCase()),
        getAccountsUseCase = GetAccountsUseCase(accountRepository),
        calculateMonthProjectionUseCase = CalculateMonthProjectionUseCase(
            transactionRepository,
            accountRepository,
        ),
    )

    private suspend fun createTransaction(
        date: LocalDate,
        status: TransactionStatus = TransactionStatus.CONFIRMED,
    ) = transactionRepository.createTransaction(
        CreateTransactionRequest(
            accountId = 1L,
            amount = 1_000L,
            date = date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
            type = TransactionType.EXPENSE,
            status = status,
        ),
    )
}
