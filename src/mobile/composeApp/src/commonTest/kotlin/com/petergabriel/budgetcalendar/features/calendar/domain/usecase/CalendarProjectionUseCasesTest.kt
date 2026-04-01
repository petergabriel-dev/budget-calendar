package com.petergabriel.budgetcalendar.features.calendar.domain.usecase

import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account
import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType
import com.petergabriel.budgetcalendar.features.accounts.testutil.FakeAccountRepository
import com.petergabriel.budgetcalendar.features.transactions.domain.model.CreateTransactionRequest
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import com.petergabriel.budgetcalendar.features.transactions.domain.model.UpdateTransactionStatusRequest
import com.petergabriel.budgetcalendar.features.transactions.domain.usecase.UpdateTransactionStatusUseCase
import com.petergabriel.budgetcalendar.features.transactions.testutil.FakeTransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CalendarProjectionUseCasesTest {
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var calculateMonthProjectionUseCase: CalculateMonthProjectionUseCase
    private lateinit var updateTransactionStatusUseCase: UpdateTransactionStatusUseCase

    @BeforeTest
    fun setUp() {
        accountRepository = FakeAccountRepository()
        transactionRepository = FakeTransactionRepository()
        calculateMonthProjectionUseCase = CalculateMonthProjectionUseCase(
            transactionRepository,
            accountRepository,
        )
        updateTransactionStatusUseCase = UpdateTransactionStatusUseCase(
            transactionRepository,
            accountRepository,
        )
    }

    @Test
    fun calculateMonthProjection_withPendingExpense_returnsCorrectProjection() = runBlocking {
        // Arrange
        val today = DateUtils.startOfDayMillis(DateUtils.nowMillis())
        val (year, month) = DateUtils.currentYearMonth()
        val yearMonth = kotlinx.datetime.YearMonth(year, month)
        
        val spendingPoolAccount = Account(
            id = 1L,
            name = "Test Checking",
            type = AccountType.CHECKING,
            balance = 500_00L, // $500
            isInSpendingPool = true,
            description = null,
            createdAt = today,
            updatedAt = today,
        )
        
        // Create transaction FIRST (before seeding account)
        val pendingExpense = CreateTransactionRequest(
            accountId = 1L,
            amount = 75_00L, // $75
            type = TransactionType.EXPENSE,
            status = TransactionStatus.PENDING,
            date = today,
            description = "Pending expense",
            category = null,
            isSandbox = false,
            linkedTransactionId = null,
        )
        transactionRepository.createTransaction(pendingExpense)
        
        // Seed account and trigger
        accountRepository.seedAccounts(spendingPoolAccount)
        
        // Collect the projection - it should reflect the pending expense
        val projection = calculateMonthProjectionUseCase(yearMonth).first()

        // Assert
        // $500 (balance) + (-$75) (pending expense) = $425
        assertEquals(425_00L, projection)
    }

    @Test
    fun calculateMonthProjection_afterExpenseConfirmed_doesNotDoubleCount() = runBlocking {
        // Arrange
        val today = DateUtils.startOfDayMillis(DateUtils.nowMillis())
        val (year, month) = DateUtils.currentYearMonth()
        val yearMonth = kotlinx.datetime.YearMonth(year, month)
        
        val spendingPoolAccount = Account(
            id = 1L,
            name = "Test Checking",
            type = AccountType.CHECKING,
            balance = 500_00L, // $500
            isInSpendingPool = true,
            description = null,
            createdAt = today,
            updatedAt = today,
        )
        
        val pendingExpense = CreateTransactionRequest(
            accountId = 1L,
            amount = 75_00L, // $75
            type = TransactionType.EXPENSE,
            status = TransactionStatus.PENDING,
            date = today,
            description = "Pending expense",
            category = null,
            isSandbox = false,
            linkedTransactionId = null,
        )
        val transaction = transactionRepository.createTransaction(pendingExpense)
        
        accountRepository.seedAccounts(spendingPoolAccount)

        // Confirm the expense - this calls adjustBalance internally
        updateTransactionStatusUseCase(transaction.id, UpdateTransactionStatusRequest(TransactionStatus.CONFIRMED))

        // Act
        val projection = calculateMonthProjectionUseCase(yearMonth).first()

        // Assert
        // After confirmation, balance is $425 and pending is gone
        // Projection should just be the balance since no more pending transactions
        assertEquals(425_00L, projection)
    }
}
