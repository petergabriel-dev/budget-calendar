package com.petergabriel.budgetcalendar.features.budget.domain.usecase

import com.petergabriel.budgetcalendar.features.budget.testutil.FakeBudgetRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BudgetUseCasesTest {
    private lateinit var budgetRepository: FakeBudgetRepository

    private lateinit var calculateSafeToSpendUseCase: CalculateSafeToSpendUseCase

    @BeforeTest
    fun setUp() {
        budgetRepository = FakeBudgetRepository()

        calculateSafeToSpendUseCase = CalculateSafeToSpendUseCase(budgetRepository)
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
    fun calculateSafeToSpend_subtractsCardReserveAndReservations() = runBlocking {
        budgetRepository.setTotalSpendingPoolBalance(12_000L)
        budgetRepository.setPendingReservations(1_000L)
        budgetRepository.setOverdueReservations(0L)
        budgetRepository.setCreditCardReservedAmount(2_000L)

        val summary = calculateSafeToSpendUseCase().first()

        assertEquals(9_000L, summary.availableToSpend)
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

    // ========== Bugfix Reproduction Tests (2026-04-01) ==========
    
    /**
     * Bug: Safe to Spend double-deducts confirmed spending.
     * 
     * Test verifies that when a PENDING expense is confirmed, STS correctly
     * stays stable when account balance is adjusted on confirmation.
     * 
     * Scenario:
     * 1. Initial state: $500 balance, no pending, no confirmed → STS = $500
     * 2. Add $75 PENDING expense → STS = $500 - $75 = $425
     * 3. Confirm the expense:
     *    - pendingReservations goes to $0 (no longer pending)
     *    - totalBalance becomes $425 (account balance adjusted)
     *    - confirmed expense is already reflected in the lower pool balance
     *    - STS = $425 - $0 = $425 (stable)
     * 4. Add another $50 PENDING expense → STS = $425 - $50 = $375
     * 5. Confirm → totalBalance becomes $375, STS = $375 - $0 = $375 (stable)
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
        
        // Step 3: Confirm the expense
        // After confirmation: pending goes to 0 and account balance decreases to $425
        budgetRepository.setPendingReservations(0L)
        budgetRepository.setTotalSpendingPoolBalance(425_00L)
        
        val stsAfterConfirm = calculateSafeToSpendUseCase().first()
        assertEquals(425_00L, stsAfterConfirm.availableToSpend, "STS should remain stable after confirming")
        
        // Step 4: Add another $50 PENDING expense
        budgetRepository.setPendingReservations(50_00L)
        
        val stsWithNewPending = calculateSafeToSpendUseCase().first()
        // STS = 425 - 50(pending) = 375
        assertEquals(375_00L, stsWithNewPending.availableToSpend, "STS should reduce by new pending amount")
        assertEquals(50_00L, stsWithNewPending.pendingReservations)
        
        // Step 5: Confirm the second expense
        budgetRepository.setPendingReservations(0L)
        budgetRepository.setTotalSpendingPoolBalance(375_00L)
        val stsAfterSecondConfirm = calculateSafeToSpendUseCase().first()
        // STS = 375 - 0 = 375
        assertEquals(375_00L, stsAfterSecondConfirm.availableToSpend, "STS should remain stable after confirming second expense")
    }

}
