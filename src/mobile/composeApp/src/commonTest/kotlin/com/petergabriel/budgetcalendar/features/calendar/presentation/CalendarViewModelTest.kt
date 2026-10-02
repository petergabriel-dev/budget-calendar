package com.petergabriel.budgetcalendar.features.calendar.presentation

import com.petergabriel.budgetcalendar.features.accounts.domain.usecase.GetAccountsUseCase
import com.petergabriel.budgetcalendar.features.accounts.testutil.FakeAccountRepository
import com.petergabriel.budgetcalendar.features.calendar.domain.model.firstDay
import com.petergabriel.budgetcalendar.features.calendar.domain.model.plusMonths
import com.petergabriel.budgetcalendar.features.calendar.domain.model.toYearMonth
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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
    fun showMonthContaining_inMonthDoesNotReload() = runTest(dispatcher) {
        val viewModel = calendarViewModel()
        advanceUntilIdle()
        val currentMonth = viewModel.uiState.value.currentMonth

        viewModel.showMonthContaining(currentMonth.firstDay())

        assertEquals(currentMonth, viewModel.uiState.value.currentMonth)
        assertFalse(viewModel.uiState.value.isMonthLoading)
    }

    @Test
    fun showMonthContaining_adjacentDateLoadsItsMonth() = runTest(dispatcher) {
        val viewModel = calendarViewModel()
        advanceUntilIdle()
        val adjacentDate = viewModel.uiState.value.currentMonth.plusMonths(1).firstDay()

        viewModel.showMonthContaining(adjacentDate)

        assertEquals(adjacentDate.toYearMonth(), viewModel.uiState.value.currentMonth)
        assertTrue(viewModel.uiState.value.isMonthLoading)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isMonthLoading)
    }

    @Test
    fun transactionsOn_readsCurrentMonthRowsAndExcludesCancelledRows() = runTest(dispatcher) {
        val viewModel = calendarViewModel()
        advanceUntilIdle()
        val date = viewModel.uiState.value.currentMonth.firstDay()
        val cancelledTransaction = createTransaction(date, TransactionStatus.CANCELLED)
        val firstTransaction = createTransaction(date)
        advanceUntilIdle()
        assertEquals(listOf(firstTransaction), viewModel.uiState.value.transactionsOn(date))

        val laterTransaction = createTransaction(date)
        advanceUntilIdle()

        val transactions = viewModel.uiState.value.transactionsOn(date)
        assertEquals(2, transactions.size)
        assertTrue(transactions.contains(firstTransaction))
        assertTrue(transactions.contains(laterTransaction))
        assertFalse(transactions.contains(cancelledTransaction))
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
