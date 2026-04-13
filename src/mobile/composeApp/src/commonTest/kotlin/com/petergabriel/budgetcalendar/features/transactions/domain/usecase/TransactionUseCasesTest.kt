package com.petergabriel.budgetcalendar.features.transactions.domain.usecase

import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account
import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType
import com.petergabriel.budgetcalendar.features.accounts.testutil.FakeAccountRepository
import com.petergabriel.budgetcalendar.features.transactions.domain.model.CreateTransactionRequest
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import com.petergabriel.budgetcalendar.features.transactions.domain.model.UpdateTransactionStatusRequest
import com.petergabriel.budgetcalendar.features.transactions.testutil.FakeTransactionRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TransactionUseCasesTest {
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var createTransactionUseCase: CreateTransactionUseCase
    private lateinit var updateTransactionStatusUseCase: UpdateTransactionStatusUseCase
    private lateinit var deleteTransactionUseCase: DeleteTransactionUseCase
    private lateinit var markOverdueTransactionsUseCase: MarkOverdueTransactionsUseCase

    @BeforeTest
    fun setUp() {
        transactionRepository = FakeTransactionRepository()
        accountRepository = FakeAccountRepository(
            transactionProvider = transactionRepository::allTransactions,
            transactionChangedTrigger = transactionRepository.transactionChangedTrigger,
        )
        createTransactionUseCase = CreateTransactionUseCase(transactionRepository)
        updateTransactionStatusUseCase = UpdateTransactionStatusUseCase(transactionRepository)
        deleteTransactionUseCase = DeleteTransactionUseCase(transactionRepository)
        markOverdueTransactionsUseCase = MarkOverdueTransactionsUseCase(transactionRepository)
    }

    @Test
    fun createTransaction_createsValidIncomeTransaction() = runBlocking {
        val today = DateUtils.startOfDayMillis(DateUtils.nowMillis())
        val result = createTransactionUseCase(
            CreateTransactionRequest(
                accountId = 1L,
                amount = 10_000L,
                date = today,
                type = TransactionType.INCOME,
                description = "Salary",
                category = "Paycheck",
            ),
        )

        assertTrue(result.isSuccess)
        val transaction = result.getOrThrow()
        assertEquals(TransactionType.INCOME, transaction.type)
        assertEquals(TransactionStatus.PENDING, transaction.status)
        assertEquals(10_000L, transaction.amount)
        assertEquals(1, transactionRepository.allTransactions().size)
    }

    @Test
    fun createTransaction_createsValidExpenseWithin30Days() = runBlocking {
        val today = DateUtils.startOfDayMillis(DateUtils.nowMillis())
        val result = createTransactionUseCase(
            CreateTransactionRequest(
                accountId = 1L,
                amount = 2_000L,
                date = today + (30 * DateUtils.MILLIS_IN_DAY),
                type = TransactionType.EXPENSE,
                category = "Groceries",
            ),
        )

        assertTrue(result.isSuccess)
        assertEquals(TransactionType.EXPENSE, result.getOrThrow().type)
    }

    @Test
    fun createTransaction_transferCreatesLinkedPair() = runBlocking {
        val today = DateUtils.startOfDayMillis(DateUtils.nowMillis())
        val result = createTransactionUseCase(
            CreateTransactionRequest(
                accountId = 10L,
                destinationAccountId = 20L,
                amount = 5_000L,
                date = today,
                type = TransactionType.TRANSFER,
                description = "Move cash",
            ),
        )

        assertTrue(result.isSuccess)
        val sourceTransaction = result.getOrThrow()
        val allTransactions = transactionRepository.allTransactions()

        assertEquals(2, allTransactions.size)

        val persistedSource = allTransactions.first { transaction -> transaction.id == sourceTransaction.id }
        val destination = allTransactions.first { transaction -> transaction.id == persistedSource.linkedTransactionId }

        assertEquals(destination.id, persistedSource.linkedTransactionId)
        assertEquals(persistedSource.id, destination.linkedTransactionId)
        assertEquals(20L, destination.accountId)
        assertEquals(10L, destination.destinationAccountId)
    }

    @Test
    fun createTransaction_transferToSameAccountReturnsError() = runBlocking {
        val today = DateUtils.startOfDayMillis(DateUtils.nowMillis())
        val result = createTransactionUseCase(
            CreateTransactionRequest(
                accountId = 7L,
                destinationAccountId = 7L,
                amount = 100L,
                date = today,
                type = TransactionType.TRANSFER,
            ),
        )

        assertTrue(result.isFailure)
        assertEquals("Cannot transfer to the same account", result.exceptionOrNull()?.message)
    }

    @Test
    fun createTransaction_returnsExpectedValidationErrors() = runBlocking {
        val today = DateUtils.startOfDayMillis(DateUtils.nowMillis())

        val invalidAmountResult = createTransactionUseCase(
            CreateTransactionRequest(
                accountId = 1L,
                amount = 0L,
                date = today,
                type = TransactionType.EXPENSE,
            ),
        )

        val pastIncomeResult = createTransactionUseCase(
            CreateTransactionRequest(
                accountId = 1L,
                amount = 100L,
                date = today - DateUtils.MILLIS_IN_DAY,
                type = TransactionType.INCOME,
            ),
        )

        val farFutureExpenseResult = createTransactionUseCase(
            CreateTransactionRequest(
                accountId = 1L,
                amount = 100L,
                date = today + (31 * DateUtils.MILLIS_IN_DAY),
                type = TransactionType.EXPENSE,
            ),
        )

        assertTrue(invalidAmountResult.isFailure)
        assertEquals(
            "Transaction amount must be greater than 0",
            invalidAmountResult.exceptionOrNull()?.message,
        )

        assertTrue(pastIncomeResult.isFailure)
        assertEquals("Cannot record income in the past", pastIncomeResult.exceptionOrNull()?.message)

        assertTrue(farFutureExpenseResult.isFailure)
        assertEquals(
            "Cannot schedule expenses more than 30 days ahead",
            farFutureExpenseResult.exceptionOrNull()?.message,
        )
    }

    @Test
    fun updateTransactionStatus_supportsAllValidTransitions() = runBlocking {
        val today = DateUtils.startOfDayMillis(DateUtils.nowMillis())

        val pendingToConfirmed = transactionRepository.createTransaction(
            CreateTransactionRequest(1L, amount = 100L, date = today, type = TransactionType.EXPENSE, status = TransactionStatus.PENDING),
        )
        val pendingToCancelled = transactionRepository.createTransaction(
            CreateTransactionRequest(1L, amount = 100L, date = today, type = TransactionType.EXPENSE, status = TransactionStatus.PENDING),
        )
        val overdueToConfirmed = transactionRepository.createTransaction(
            CreateTransactionRequest(1L, amount = 100L, date = today, type = TransactionType.EXPENSE, status = TransactionStatus.OVERDUE),
        )
        val overdueToCancelled = transactionRepository.createTransaction(
            CreateTransactionRequest(1L, amount = 100L, date = today, type = TransactionType.EXPENSE, status = TransactionStatus.OVERDUE),
        )

        assertTrue(updateTransactionStatusUseCase(
            pendingToConfirmed.id,
            UpdateTransactionStatusRequest(TransactionStatus.CONFIRMED),
        ).isSuccess)
        assertTrue(updateTransactionStatusUseCase(
            pendingToCancelled.id,
            UpdateTransactionStatusRequest(TransactionStatus.CANCELLED),
        ).isSuccess)
        assertTrue(updateTransactionStatusUseCase(
            overdueToConfirmed.id,
            UpdateTransactionStatusRequest(TransactionStatus.CONFIRMED),
        ).isSuccess)
        assertTrue(updateTransactionStatusUseCase(
            overdueToCancelled.id,
            UpdateTransactionStatusRequest(TransactionStatus.CANCELLED),
        ).isSuccess)

        assertEquals(TransactionStatus.CONFIRMED, transactionRepository.getTransactionById(pendingToConfirmed.id)?.status)
        assertEquals(TransactionStatus.CANCELLED, transactionRepository.getTransactionById(pendingToCancelled.id)?.status)
        assertEquals(TransactionStatus.CONFIRMED, transactionRepository.getTransactionById(overdueToConfirmed.id)?.status)
        assertEquals(TransactionStatus.CANCELLED, transactionRepository.getTransactionById(overdueToCancelled.id)?.status)
    }

    @Test
    fun updateTransactionStatus_rejectsInvalidTransitions() = runBlocking {
        val today = DateUtils.startOfDayMillis(DateUtils.nowMillis())

        val confirmedTransaction = transactionRepository.createTransaction(
            CreateTransactionRequest(1L, amount = 100L, date = today, type = TransactionType.EXPENSE, status = TransactionStatus.CONFIRMED),
        )
        val cancelledTransaction = transactionRepository.createTransaction(
            CreateTransactionRequest(1L, amount = 100L, date = today, type = TransactionType.EXPENSE, status = TransactionStatus.CANCELLED),
        )

        val confirmedToPending = updateTransactionStatusUseCase(
            confirmedTransaction.id,
            UpdateTransactionStatusRequest(TransactionStatus.PENDING),
        )
        val cancelledToConfirmed = updateTransactionStatusUseCase(
            cancelledTransaction.id,
            UpdateTransactionStatusRequest(TransactionStatus.CONFIRMED),
        )

        assertTrue(confirmedToPending.isFailure)
        assertEquals(
            "Invalid transaction status transition: CONFIRMED -> PENDING",
            confirmedToPending.exceptionOrNull()?.message,
        )

        assertTrue(cancelledToConfirmed.isFailure)
        assertEquals(
            "Invalid transaction status transition: CANCELLED -> CONFIRMED",
            cancelledToConfirmed.exceptionOrNull()?.message,
        )
    }

    @Test
    fun deleteTransaction_deletingTransferDeletesLinkedTransactionToo() = runBlocking {
        val today = DateUtils.startOfDayMillis(DateUtils.nowMillis())
        val transfer = createTransactionUseCase(
            CreateTransactionRequest(
                accountId = 1L,
                destinationAccountId = 2L,
                amount = 2_000L,
                date = today,
                type = TransactionType.TRANSFER,
            ),
        ).getOrThrow()

        val result = deleteTransactionUseCase(transfer.id)

        assertTrue(result.isSuccess)
        assertTrue(transactionRepository.allTransactions().isEmpty())
    }

    @Test
    fun deleteTransaction_deletingIncomeOrExpenseDeletesOnlyTargetTransaction() = runBlocking {
        val today = DateUtils.startOfDayMillis(DateUtils.nowMillis())

        val income = transactionRepository.createTransaction(
            CreateTransactionRequest(1L, amount = 500L, date = today, type = TransactionType.INCOME),
        )
        val expense = transactionRepository.createTransaction(
            CreateTransactionRequest(1L, amount = 300L, date = today, type = TransactionType.EXPENSE),
        )

        val result = deleteTransactionUseCase(income.id)

        assertTrue(result.isSuccess)

        val remaining = transactionRepository.allTransactions()
        assertEquals(1, remaining.size)
        assertEquals(expense.id, remaining.first().id)
    }

    @Test
    fun markOverdueTransactions_updatesOnlyPendingTransactionsPastDatePlusOneDay() = runBlocking {
        val today = DateUtils.startOfDayMillis(DateUtils.nowMillis())

        val pendingPast = transactionRepository.createTransaction(
            CreateTransactionRequest(
                accountId = 1L,
                amount = 100L,
                date = today - (2 * DateUtils.MILLIS_IN_DAY),
                type = TransactionType.EXPENSE,
                status = TransactionStatus.PENDING,
            ),
        )
        val pendingFuture = transactionRepository.createTransaction(
            CreateTransactionRequest(
                accountId = 1L,
                amount = 100L,
                date = today + DateUtils.MILLIS_IN_DAY,
                type = TransactionType.EXPENSE,
                status = TransactionStatus.PENDING,
            ),
        )
        val confirmedPast = transactionRepository.createTransaction(
            CreateTransactionRequest(
                accountId = 1L,
                amount = 100L,
                date = today - (3 * DateUtils.MILLIS_IN_DAY),
                type = TransactionType.EXPENSE,
                status = TransactionStatus.CONFIRMED,
            ),
        )

        val updatedCount = markOverdueTransactionsUseCase(today)

        assertEquals(1, updatedCount)
        assertEquals(TransactionStatus.OVERDUE, transactionRepository.getTransactionById(pendingPast.id)?.status)
        assertEquals(TransactionStatus.PENDING, transactionRepository.getTransactionById(pendingFuture.id)?.status)
        assertEquals(TransactionStatus.CONFIRMED, transactionRepository.getTransactionById(confirmedPast.id)?.status)
    }

    // ========== Balance-Related Reproduction Tests ==========

    /**
     * Bug: Account balance doesn't change after creating/confirming a transaction.
     *
     * Test: Creating a PENDING expense does NOT change actual balance (STS is reserved separately),
     * but confirming the expense SHOULD decrease the account balance.
     *
     * The fix: account balance is derived from confirmed transaction history.
     */
    @Test
    fun confirmExpense_deductsAccountBalance() = runBlocking {
        val today = DateUtils.startOfDayMillis(DateUtils.nowMillis())

        // Seed account with initial balance
        accountRepository.seedAccounts(
            Account(
                id = 1L,
                name = "Checking",
                type = AccountType.CHECKING,
                balance = 500_00L,
                isInSpendingPool = true,
                createdAt = today,
                updatedAt = today,
            ),
        )

        // Create PENDING expense - actual balance should NOT change (STS reservation is separate)
        val expenseResult = createTransactionUseCase(
            CreateTransactionRequest(
                accountId = 1L,
                amount = 75_00L,
                date = today,
                type = TransactionType.EXPENSE,
                category = "Groceries",
            ),
        )
        assertTrue(expenseResult.isSuccess)
        val expense = expenseResult.getOrThrow()
        assertEquals(TransactionStatus.PENDING, expense.status)

        // Actual balance should still be 500_00 (pending doesn't change balance)
        val balanceAfterCreate = accountRepository.getBalance(1L)
        assertEquals(500_00L, balanceAfterCreate, "Account balance should not change when pending expense is created")

        // Confirm the expense
        val confirmResult = updateTransactionStatusUseCase(
            expense.id,
            UpdateTransactionStatusRequest(TransactionStatus.CONFIRMED),
        )
        assertTrue(confirmResult.isSuccess)

        // Account balance should now be 425_00 (500_00 - 75_00)
        val balanceAfterConfirm = accountRepository.getBalance(1L)
        assertEquals(425_00L, balanceAfterConfirm, "Account balance should decrease when expense is confirmed")
    }

    /**
     * Bug: Account balance doesn't change after creating/confirming a transaction.
     *
     * Test: Creating PENDING income and confirming it should increase account balance.
     */
    @Test
    fun confirmIncome_increasesAccountBalance() = runBlocking {
        val today = DateUtils.startOfDayMillis(DateUtils.nowMillis())

        // Seed account with initial balance
        accountRepository.seedAccounts(
            Account(
                id = 1L,
                name = "Checking",
                type = AccountType.CHECKING,
                balance = 5_000L,
                isInSpendingPool = true,
                createdAt = today,
                updatedAt = today,
            ),
        )

        // Create PENDING income
        val incomeResult = createTransactionUseCase(
            CreateTransactionRequest(
                accountId = 1L,
                amount = 3_000L,
                date = today,
                type = TransactionType.INCOME,
                category = "Salary",
            ),
        )
        assertTrue(incomeResult.isSuccess)
        val income = incomeResult.getOrThrow()
        assertEquals(TransactionStatus.PENDING, income.status)

        // Confirm the income
        val confirmResult = updateTransactionStatusUseCase(
            income.id,
            UpdateTransactionStatusRequest(TransactionStatus.CONFIRMED),
        )
        assertTrue(confirmResult.isSuccess)

        // Account balance should now be 8_000 (5_000 + 3_000)
        val balanceAfterConfirm = accountRepository.getBalance(1L)
        assertEquals(8_000L, balanceAfterConfirm, "Account balance should increase when income is confirmed")
    }

    /**
     * Bug: Account balance doesn't change after creating/confirming a transaction.
     *
     * Test: Creating PENDING transfer and confirming it should decrease source balance
     * and increase destination balance.
     */
    @Test
    fun confirmTransfer_decreasesSourceAndIncreasesDestination() = runBlocking {
        val today = DateUtils.startOfDayMillis(DateUtils.nowMillis())

        // Seed two accounts
        accountRepository.seedAccounts(
            Account(
                id = 1L,
                name = "Checking",
                type = AccountType.CHECKING,
                balance = 10_000L,
                isInSpendingPool = true,
                createdAt = today,
                updatedAt = today,
            ),
            Account(
                id = 2L,
                name = "Savings",
                type = AccountType.SAVINGS,
                balance = 5_000L,
                isInSpendingPool = true,
                createdAt = today,
                updatedAt = today,
            ),
        )

        // Create PENDING transfer
        val transferResult = createTransactionUseCase(
            CreateTransactionRequest(
                accountId = 1L,
                destinationAccountId = 2L,
                amount = 2_000L,
                date = today,
                type = TransactionType.TRANSFER,
                description = "Move to savings",
            ),
        )
        assertTrue(transferResult.isSuccess)
        val transfer = transferResult.getOrThrow()
        assertEquals(TransactionStatus.PENDING, transfer.status)

        // Confirm the transfer
        val confirmResult = updateTransactionStatusUseCase(
            transfer.id,
            UpdateTransactionStatusRequest(TransactionStatus.CONFIRMED),
        )
        assertTrue(confirmResult.isSuccess)

        // Source balance should decrease by 2_000 (10_000 -> 8_000)
        val sourceBalance = accountRepository.getBalance(1L)
        assertEquals(8_000L, sourceBalance, "Source account balance should decrease when transfer is confirmed")

        // Destination balance should increase by 2_000 (5_000 -> 7_000)
        val destBalance = accountRepository.getBalance(2L)
        assertEquals(7_000L, destBalance, "Destination account balance should increase when transfer is confirmed")
    }

    /**
     * Bug: Safe to Spend incorrectly increases when a pending expense is confirmed.
     *
     * This tests that cancelling a pending expense does NOT change the account balance
     * (pending was "reserved" via STS but never applied to actual balance).
     *
     * Note: STS (Safe to Spend) is calculated separately as:
     *   totalBalance - pendingReservations - overdueReservations - ccReserved
     * Cancelling a pending transaction removes it from pendingReservations, restoring STS.
     * But the actual account balance doesn't change because pending was never applied.
     */
    @Test
    fun cancelPendingExpense_doesNotChangeBalance() = runBlocking {
        val today = DateUtils.startOfDayMillis(DateUtils.nowMillis())

        // Seed account with initial balance
        accountRepository.seedAccounts(
            Account(
                id = 1L,
                name = "Checking",
                type = AccountType.CHECKING,
                balance = 10_000L,
                isInSpendingPool = true,
                createdAt = today,
                updatedAt = today,
            ),
        )

        // Create PENDING expense - actual balance should NOT change
        val expenseResult = createTransactionUseCase(
            CreateTransactionRequest(
                accountId = 1L,
                amount = 2_000L,
                date = today,
                type = TransactionType.EXPENSE,
                category = "Groceries",
            ),
        )
        assertTrue(expenseResult.isSuccess)
        val expense = expenseResult.getOrThrow()

        // Actual balance should still be 10_000 (pending doesn't change actual balance)
        val balanceAfterCreate = accountRepository.getBalance(1L)
        assertEquals(10_000L, balanceAfterCreate, "Account balance should not change when pending expense is created")

        // Cancel the expense
        val cancelResult = updateTransactionStatusUseCase(
            expense.id,
            UpdateTransactionStatusRequest(TransactionStatus.CANCELLED),
        )
        assertTrue(cancelResult.isSuccess)

        // Account balance should STILL be 10_000 (cancelling pending doesn't change balance)
        val balanceAfterCancel = accountRepository.getBalance(1L)
        assertEquals(10_000L, balanceAfterCancel, "Account balance should not change when pending expense is cancelled")
    }
}
