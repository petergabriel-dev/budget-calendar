package com.petergabriel.budgetcalendar.features.calendar.domain.usecase

import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account
import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType
import com.petergabriel.budgetcalendar.features.accounts.testutil.FakeAccountRepository
import com.petergabriel.budgetcalendar.features.transactions.domain.model.CreateTransactionRequest
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import com.petergabriel.budgetcalendar.features.transactions.testutil.FakeTransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CalendarProjectionUseCasesTest {
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var calculateMonthProjectionUseCase: CalculateMonthProjectionUseCase

    @BeforeTest
    fun setUp() {
        accountRepository = FakeAccountRepository()
        transactionRepository = FakeTransactionRepository()
        calculateMonthProjectionUseCase = CalculateMonthProjectionUseCase(
            transactionRepository,
            accountRepository,
        )
    }

    private fun currentYearMonth(): kotlinx.datetime.YearMonth {
        val nowMillis = System.currentTimeMillis()
        val localDate = Instant.fromEpochMilliseconds(nowMillis)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
        return kotlinx.datetime.YearMonth(localDate.year, localDate.month.number)
    }

    private fun localDateMillis(year: Int, month: Int, day: Int): Long {
        return LocalDate(year, month, day)
            .atStartOfDayIn(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()
    }

    @Test
    fun calculateMonthProjection_withPendingExpense_returnsCorrectProjection() = runBlocking {
        // Arrange
        val spendingPoolAccount = Account(
            id = 1L,
            name = "Test Checking",
            type = AccountType.CHECKING,
            balance = 500_00L, // $500
            isInSpendingPool = true,
            description = null,
            createdAt = 0L,
            updatedAt = 0L,
        )
        accountRepository.seedAccounts(spendingPoolAccount)

        val yearMonth = currentYearMonth()
        val startMillis = localDateMillis(yearMonth.year, yearMonth.month.number, 1)
        val endMillis = localDateMillis(yearMonth.year, yearMonth.month.number, 28)

        val pendingExpense = CreateTransactionRequest(
            accountId = 1L,
            amount = 75_00L, // $75
            type = TransactionType.EXPENSE,
            status = TransactionStatus.PENDING,
            date = startMillis,
            description = "Pending expense",
            category = null,
            isSandbox = false,
            linkedTransactionId = null,
        )
        transactionRepository.createTransaction(pendingExpense)

        // Act
        val projection = calculateMonthProjectionUseCase(yearMonth).first()

        // Assert
        // $500 (balance) + (-$75) (pending expense) = $425
        assertEquals(425_00L, projection)
    }

    @Test
    fun calculateMonthProjection_afterBalanceAdjust_stillReturnsCorrectProjection() = runBlocking {
        // Arrange
        val spendingPoolAccount = Account(
            id = 1L,
            name = "Test Checking",
            type = AccountType.CHECKING,
            balance = 500_00L, // $500
            isInSpendingPool = true,
            description = null,
            createdAt = 0L,
            updatedAt = 0L,
        )
        accountRepository.seedAccounts(spendingPoolAccount)

        val yearMonth = currentYearMonth()
        val startMillis = localDateMillis(yearMonth.year, yearMonth.month.number, 1)
        val endMillis = localDateMillis(yearMonth.year, yearMonth.month.number, 28)

        val pendingExpense = CreateTransactionRequest(
            accountId = 1L,
            amount = 75_00L, // $75
            type = TransactionType.EXPENSE,
            status = TransactionStatus.PENDING,
            date = startMillis,
            description = "Pending expense",
            category = null,
            isSandbox = false,
            linkedTransactionId = null,
        )
        transactionRepository.createTransaction(pendingExpense)

        // Simulate the expense being confirmed and balance adjusted
        // (in real app, UpdateTransactionStatusUseCase calls adjustBalance)
        accountRepository.adjustBalance(1L, -75_00L)
        // After adjustBalance to $425 and pending still there, the projection should NOT double count
        // The projection = poolBalance + signedTransactionSum
        // poolBalance = $425 (new balance after adjustBalance)
        // signedTransactionSum = -$75 (pending expense still in the system)
        // projection = $425 + (-$75) = $350 if double-counted OR $425 if properly handled

        // Act
        val projection = calculateMonthProjectionUseCase(yearMonth).first()

        // Assert
        // After adjustBalance to $425 and pending still there, we should NOT double count
        // The projection should use current balance, and pending expenses should be 0 extra deduction
        // since balance already reflects the confirmed expense
        assertEquals(425_00L, projection)
    }
}
