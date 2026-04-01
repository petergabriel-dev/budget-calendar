package com.petergabriel.budgetcalendar.features.budget.domain.usecase

import com.petergabriel.budgetcalendar.features.budget.domain.model.CreditCardReservation
import com.petergabriel.budgetcalendar.features.budget.testutil.FakeBudgetRepository
import com.petergabriel.budgetcalendar.features.budget.testutil.FakeMonthlyRolloverRepository
import com.petergabriel.budgetcalendar.features.transactions.domain.model.CreateTransactionRequest
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import com.petergabriel.budgetcalendar.features.transactions.testutil.FakeTransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BudgetUseCasesTest {
    private lateinit var budgetRepository: FakeBudgetRepository
    private lateinit var monthlyRolloverRepository: FakeMonthlyRolloverRepository
    private lateinit var transactionRepository: FakeTransactionRepository

    private lateinit var calculateSafeToSpendUseCase: CalculateSafeToSpendUseCase
    private lateinit var getCreditCardReservationsUseCase: GetCreditCardReservationsUseCase
    private lateinit var saveMonthlyRolloverUseCase: SaveMonthlyRolloverUseCase
    private lateinit var getRolloverHistoryUseCase: GetRolloverHistoryUseCase
    private lateinit var calculateMonthEndRolloverUseCase: CalculateMonthEndRolloverUseCase

    @BeforeTest
    fun setUp() {
        budgetRepository = FakeBudgetRepository()
        monthlyRolloverRepository = FakeMonthlyRolloverRepository()
        transactionRepository = FakeTransactionRepository()

        calculateSafeToSpendUseCase = CalculateSafeToSpendUseCase(
            budgetRepository,
            monthlyRolloverRepository,
            transactionRepository,
        )
        getCreditCardReservationsUseCase = GetCreditCardReservationsUseCase(budgetRepository)
        saveMonthlyRolloverUseCase = SaveMonthlyRolloverUseCase(monthlyRolloverRepository)
        getRolloverHistoryUseCase = GetRolloverHistoryUseCase(monthlyRolloverRepository)
        calculateMonthEndRolloverUseCase = CalculateMonthEndRolloverUseCase(budgetRepository, transactionRepository)
    }

    @Test
    fun calculateSafeToSpend_computesPoolMinusPendingAndOverdue() = runBlocking {
        budgetRepository.setTotalSpendingPoolBalance(20_000L)
        budgetRepository.setPendingReservations(2_500L)
        budgetRepository.setOverdueReservations(1_000L)

        val summary = calculateSafeToSpendUseCase().first()

        assertEquals(20_000L, summary.totalLiquidAssets)
        assertEquals(2_500L, summary.pendingReservations)
        assertEquals(1_000L, summary.overdueReservations)
        assertEquals(16_500L, summary.availableToSpend)
    }

    @Test
    fun calculateSafeToSpend_noAccountsInPoolReturnsZero() = runBlocking {
        budgetRepository.setTotalSpendingPoolBalance(0L)
        budgetRepository.setPendingReservations(0L)
        budgetRepository.setOverdueReservations(0L)

        val summary = calculateSafeToSpendUseCase().first()

        assertEquals(0L, summary.availableToSpend)
    }

    @Test
    fun calculateSafeToSpend_allPendingCancelledReturnsFullBalance() = runBlocking {
        budgetRepository.setTotalSpendingPoolBalance(9_000L)
        budgetRepository.setPendingReservations(0L)
        budgetRepository.setOverdueReservations(0L)

        val summary = calculateSafeToSpendUseCase().first()

        assertEquals(9_000L, summary.availableToSpend)
    }

    @Test
    fun calculateSafeToSpend_negativeResultIsClampedToZero() = runBlocking {
        budgetRepository.setTotalSpendingPoolBalance(2_000L)
        budgetRepository.setPendingReservations(1_500L)
        budgetRepository.setOverdueReservations(2_000L)

        val summary = calculateSafeToSpendUseCase().first()

        assertEquals(0L, summary.availableToSpend)
    }

    @Test
    fun calculateSafeToSpend_creditCardExpensesAreDeductedFromSafeToSpend() = runBlocking {
        budgetRepository.setTotalSpendingPoolBalance(10_000L)
        budgetRepository.setPendingReservations(500L)
        budgetRepository.setOverdueReservations(0L)
        budgetRepository.setCreditCardReservedAmount(2_000L)

        val summary = calculateSafeToSpendUseCase().first()

        assertEquals(7_500L, summary.availableToSpend)
        assertEquals(2_000L, summary.creditCardReserved)
    }

    @Test
    fun calculateSafeToSpend_creditCardPaymentTransferDoesNotDoubleDeduct() = runBlocking {
        budgetRepository.setTotalSpendingPoolBalance(12_000L)
        budgetRepository.setPendingReservations(1_000L)
        budgetRepository.setOverdueReservations(0L)
        budgetRepository.setCreditCardReservedAmount(2_000L)

        transactionRepository.createTransaction(
            CreateTransactionRequest(
                accountId = 1L,
                destinationAccountId = 9L,
                amount = 2_000L,
                date = localDateMillis(2026, 3, 11),
                type = TransactionType.TRANSFER,
                status = TransactionStatus.CONFIRMED,
            ),
        )

        val summary = calculateSafeToSpendUseCase().first()

        assertEquals(9_000L, summary.availableToSpend)
        assertEquals(0L, summary.confirmedSpending)
    }

    @Test
    fun calculateSafeToSpend_whenReservedDecreasesAfterCcPayment_availableToSpendIncreases() = runBlocking {
        budgetRepository.setTotalSpendingPoolBalance(10_000L)
        budgetRepository.setPendingReservations(0L)
        budgetRepository.setOverdueReservations(0L)
        budgetRepository.setCreditCardReservedAmount(3_000L)

        val beforePayment = calculateSafeToSpendUseCase().first()
        assertEquals(7_000L, beforePayment.availableToSpend)

        budgetRepository.setCreditCardReservedAmount(1_000L)

        val afterPayment = calculateSafeToSpendUseCase().first()
        assertEquals(9_000L, afterPayment.availableToSpend)
    }

    @Test
    fun getCreditCardReservations_ccWithPendingExpenseReturnsReservedAmount() = runBlocking {
        budgetRepository.setCreditCardReservations(
            listOf(
                CreditCardReservation(
                    accountId = 7L,
                    accountName = "Visa",
                    reservedAmount = 3_250L,
                    pendingTransactions = emptyList(),
                ),
            ),
        )

        val reservations = getCreditCardReservationsUseCase().first()

        assertEquals(1, reservations.size)
        assertEquals(3_250L, reservations.first().reservedAmount)
    }

    @Test
    fun getCreditCardReservations_ccWithNoPendingReturnsZeroReserved() = runBlocking {
        budgetRepository.setCreditCardReservations(
            listOf(
                CreditCardReservation(
                    accountId = 8L,
                    accountName = "Mastercard",
                    reservedAmount = 0L,
                    pendingTransactions = emptyList(),
                ),
            ),
        )

        val reservations = getCreditCardReservationsUseCase().first()

        assertEquals(1, reservations.size)
        assertEquals(0L, reservations.first().reservedAmount)
    }

    @Test
    fun getCreditCardReservations_multipleAccountsRemainSeparated() = runBlocking {
        budgetRepository.setCreditCardReservations(
            listOf(
                CreditCardReservation(1L, "Visa", 2_000L, emptyList()),
                CreditCardReservation(2L, "Amex", 1_100L, emptyList()),
            ),
        )

        val reservations = getCreditCardReservationsUseCase().first()

        assertEquals(2, reservations.size)
        assertEquals(setOf(1L, 2L), reservations.map { reservation -> reservation.accountId }.toSet())
    }

    @Test
    fun saveMonthlyRollover_validYearAndMonthArePersisted() = runBlocking {
        val result = saveMonthlyRolloverUseCase(year = 2026, month = 3, amount = 4_000L)

        assertTrue(result.isSuccess)

        val saved = monthlyRolloverRepository.getRolloverForMonth(2026, 3).getOrNull()
        assertNotNull(saved)
        assertEquals(4_000L, saved.rolloverAmount)
    }

    @Test
    fun saveMonthlyRollover_invalidMonthFailsValidation() = runBlocking {
        assertTrue(saveMonthlyRolloverUseCase(year = 2026, month = 0, amount = 10L).isFailure)
        assertTrue(saveMonthlyRolloverUseCase(year = 2026, month = 13, amount = 10L).isFailure)
    }

    @Test
    fun saveMonthlyRollover_yearBefore2000FailsValidation() = runBlocking {
        val result = saveMonthlyRolloverUseCase(year = 1999, month = 12, amount = 20L)

        assertTrue(result.isFailure)
    }

    @Test
    fun saveMonthlyRollover_savingSameMonthTwiceUpserts() = runBlocking {
        saveMonthlyRolloverUseCase(year = 2026, month = 4, amount = 150L)
        saveMonthlyRolloverUseCase(year = 2026, month = 4, amount = 325L)

        val allRollovers = getRolloverHistoryUseCase().first()

        assertEquals(1, allRollovers.count { rollover -> rollover.year == 2026 && rollover.month == 4 })
        assertEquals(325L, allRollovers.first().rolloverAmount)
    }

    @Test
    fun calculateMonthEndRollover_rolloverEqualsPoolMinusConfirmedExpenses() = runBlocking {
        budgetRepository.setTotalSpendingPoolBalance(20_000L)

        transactionRepository.createTransaction(
            CreateTransactionRequest(
                accountId = 1L,
                amount = 5_500L,
                date = localDateMillis(2026, 3, 10),
                type = TransactionType.EXPENSE,
                status = TransactionStatus.CONFIRMED,
            ),
        )

        val rollover = calculateMonthEndRolloverUseCase(2026, 3)

        assertEquals(14_500L, rollover)
    }

    @Test
    fun calculateMonthEndRollover_negativeResultIsClampedToZero() = runBlocking {
        budgetRepository.setTotalSpendingPoolBalance(3_000L)

        transactionRepository.createTransaction(
            CreateTransactionRequest(
                accountId = 1L,
                amount = 4_500L,
                date = localDateMillis(2026, 3, 5),
                type = TransactionType.EXPENSE,
                status = TransactionStatus.CONFIRMED,
            ),
        )

        val rollover = calculateMonthEndRolloverUseCase(2026, 3)

        assertEquals(0L, rollover)
    }

    @Test
    fun calculateMonthEndRollover_monthWithNoConfirmedExpensesReturnsFullBalance() = runBlocking {
        budgetRepository.setTotalSpendingPoolBalance(6_250L)

        val rollover = calculateMonthEndRolloverUseCase(2026, 3)

        assertEquals(6_250L, rollover)
    }

    // ========== Bugfix Reproduction Tests (2026-04-01) ==========
    
    /**
     * Bug: Safe to Spend Not Subtracting Confirmed Spending
     * 
     * Test verifies that when a PENDING expense is confirmed, STS correctly
     * accounts for the confirmed spending.
     * 
     * Scenario:
     * 1. Initial state: $500 balance, no pending, no confirmed → STS = $500
     * 2. Add $75 PENDING expense → STS = $500 - $75 = $425
     * 3. Confirm the expense:
     *    - pendingReservations goes to $0 (no longer pending)
     *    - confirmedSpending = $75 (expense is now confirmed this month)
     *    - With bugfix: STS = $500 - $0 - $75 = $425 (stable)
     *    - Without bugfix: STS = $500 - $0 = $500 (incorrect increase)
     * 4. Add another $50 PENDING expense → STS = $500 - $50 - $75 = $375
     * 5. Confirm → STS = $500 - $0 - $125 = $375 (stable)
     */
    @Test
    fun calculateSafeToSpend_confirmedExpenseDoesNotIncreaseSTS() = runBlocking {
        // Step 1: Initial state
        budgetRepository.setTotalSpendingPoolBalance(500_00L)  // $500.00
        budgetRepository.setPendingReservations(0L)
        
        val initialSTS = calculateSafeToSpendUseCase().first()
        assertEquals(500_00L, initialSTS.availableToSpend)
        
        // Step 2: Add $75 PENDING expense
        budgetRepository.setPendingReservations(75_00L)  // $75.00 pending
        
        val stsWithPending = calculateSafeToSpendUseCase().first()
        assertEquals(425_00L, stsWithPending.availableToSpend, "STS should reduce by pending amount")
        assertEquals(75_00L, stsWithPending.pendingReservations)
        assertEquals(0L, stsWithPending.confirmedSpending)
        
        // Step 3: Confirm the expense
        // After confirmation: pending goes to 0, confirmed spending = $75
        budgetRepository.setPendingReservations(0L)
        
        // Create confirmed expense in repository
        transactionRepository.createTransaction(
            CreateTransactionRequest(
                accountId = 1L,
                amount = 75_00L,
                date = localDateMillis(2026, 4, 1),
                type = TransactionType.EXPENSE,
                status = TransactionStatus.CONFIRMED,
            ),
        )
        
        val stsAfterConfirm = calculateSafeToSpendUseCase().first()
        assertEquals(425_00L, stsAfterConfirm.availableToSpend, "STS should remain stable after confirming (confirmedSpending offsets the deduction)")
        assertEquals(75_00L, stsAfterConfirm.confirmedSpending, "Confirmed spending should be tracked")
        
        // Step 4: Add another $50 PENDING expense
        budgetRepository.setPendingReservations(50_00L)
        
        val stsWithNewPending = calculateSafeToSpendUseCase().first()
        // STS = 500 - 50(pending) - 75(confirmed) = 375
        assertEquals(375_00L, stsWithNewPending.availableToSpend, "STS should reduce by new pending amount")
        assertEquals(50_00L, stsWithNewPending.pendingReservations)
        assertEquals(75_00L, stsWithNewPending.confirmedSpending)
        
        // Step 5: Confirm the second expense
        budgetRepository.setPendingReservations(0L)
        transactionRepository.createTransaction(
            CreateTransactionRequest(
                accountId = 1L,
                amount = 50_00L,
                date = localDateMillis(2026, 4, 1),
                type = TransactionType.EXPENSE,
                status = TransactionStatus.CONFIRMED,
            ),
        )
        
        val stsAfterSecondConfirm = calculateSafeToSpendUseCase().first()
        // STS = 500 - 0 - 125(confirmed) = 375
        assertEquals(375_00L, stsAfterSecondConfirm.availableToSpend, "STS should remain stable after confirming second expense")
        assertEquals(125_00L, stsAfterSecondConfirm.confirmedSpending, "Total confirmed spending should be $125")
    }

    private fun localDateMillis(year: Int, month: Int, day: Int): Long {
        return LocalDate(year, month, day)
            .atStartOfDayIn(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()
    }
}
