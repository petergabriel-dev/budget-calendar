package com.petergabriel.budgetcalendar.features.sandbox.domain.usecase

import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType
import com.petergabriel.budgetcalendar.features.accounts.domain.model.CreateAccountRequest
import com.petergabriel.budgetcalendar.features.accounts.testutil.FakeAccountRepository
import com.petergabriel.budgetcalendar.features.budget.domain.usecase.CalculateSafeToSpendUseCase
import com.petergabriel.budgetcalendar.features.budget.testutil.FakeBudgetRepository
import com.petergabriel.budgetcalendar.features.budget.testutil.FakeMonthlyRolloverRepository
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxSnapshot
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxTransaction
import com.petergabriel.budgetcalendar.features.sandbox.testutil.FakeSandboxRepository
import com.petergabriel.budgetcalendar.features.transactions.domain.model.CreateTransactionRequest
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import com.petergabriel.budgetcalendar.features.transactions.testutil.FakeTransactionRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SandboxUseCasesTest {
    private lateinit var sandboxRepository: FakeSandboxRepository
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var budgetRepository: FakeBudgetRepository
    private lateinit var monthlyRolloverRepository: FakeMonthlyRolloverRepository

    private lateinit var calculateSafeToSpendUseCase: CalculateSafeToSpendUseCase
    private lateinit var createSandboxUseCase: CreateSandboxUseCase
    private lateinit var addSimulationTransactionUseCase: AddSimulationTransactionUseCase
    private lateinit var getSandboxSafeToSpendUseCase: GetSandboxSafeToSpendUseCase
    private lateinit var compareSandboxWithRealityUseCase: CompareSandboxWithRealityUseCase
    private lateinit var promoteTransactionUseCase: PromoteTransactionUseCase
    private lateinit var checkAndExpireSandboxesUseCase: CheckAndExpireSandboxesUseCase

    @BeforeTest
    fun setUp() {
        runBlocking {
            sandboxRepository = FakeSandboxRepository()
            accountRepository = FakeAccountRepository()
            transactionRepository = FakeTransactionRepository()
            budgetRepository = FakeBudgetRepository()
            monthlyRolloverRepository = FakeMonthlyRolloverRepository()

            calculateSafeToSpendUseCase = CalculateSafeToSpendUseCase(
                budgetRepository,
                monthlyRolloverRepository,
                transactionRepository,
            )
            createSandboxUseCase = CreateSandboxUseCase(
                sandboxRepository,
                calculateSafeToSpendUseCase,
            )
            addSimulationTransactionUseCase = AddSimulationTransactionUseCase(
                sandboxRepository,
                accountRepository,
            )
            getSandboxSafeToSpendUseCase = GetSandboxSafeToSpendUseCase(sandboxRepository)
            compareSandboxWithRealityUseCase = CompareSandboxWithRealityUseCase(
                calculateSafeToSpendUseCase,
                getSandboxSafeToSpendUseCase,
                sandboxRepository,
            )
            promoteTransactionUseCase = PromoteTransactionUseCase(
                transactionRepository,
                sandboxRepository,
            )
            checkAndExpireSandboxesUseCase = CheckAndExpireSandboxesUseCase(sandboxRepository)

            accountRepository.createAccount(
                CreateAccountRequest(
                    name = "Primary",
                    type = AccountType.CHECKING,
                    initialBalance = 100_000L,
                    isInSpendingPool = true,
                ),
            )
        }
    }

    @Test
    fun createSandbox_validNameCreatesSnapshotWithCurrentSafeToSpend() = runBlocking {
        budgetRepository.setTotalSpendingPoolBalance(20_000L)
        budgetRepository.setPendingReservations(2_000L)
        budgetRepository.setOverdueReservations(500L)

        val result = createSandboxUseCase("  Weekend Plan  ", "Trip simulation")

        assertTrue(result.isSuccess)
        val snapshot = result.getOrThrow()
        assertEquals("Weekend Plan", snapshot.name)
        assertEquals(17_500L, snapshot.initialSafeToSpend)
    }

    @Test
    fun createSandbox_emptyNameReturnsValidationError() = runBlocking {
        val result = createSandboxUseCase("   ", null)

        assertTrue(result.isFailure)
        assertEquals("Sandbox name must be 1-50 characters", result.exceptionOrNull()?.message)
    }

    @Test
    fun createSandbox_nameOver50CharsReturnsValidationError() = runBlocking {
        val result = createSandboxUseCase("a".repeat(51), null)

        assertTrue(result.isFailure)
        assertEquals("Sandbox name must be 1-50 characters", result.exceptionOrNull()?.message)
    }

    @Test
    fun addSimulationTransaction_validExpenseInsertsPendingTransaction() = runBlocking {
        val snapshot = createSnapshot(initialSafeToSpend = 10_000L)

        val result = addSimulationTransactionUseCase(
            snapshotId = snapshot.id,
            accountId = 1L,
            amount = 2_000L,
            date = DateUtils.nowMillis(),
            type = TransactionType.EXPENSE,
            description = "Gas",
            category = "Transport",
        )

        assertTrue(result.isSuccess)
        val transaction = result.getOrThrow()
        assertEquals(TransactionStatus.PENDING, transaction.status)
        assertEquals(TransactionType.EXPENSE, transaction.type)
        assertEquals(1, sandboxRepository.allTransactions().size)
    }

    @Test
    fun addSimulationTransaction_amountNotPositiveReturnsValidationError() = runBlocking {
        val snapshot = createSnapshot(initialSafeToSpend = 10_000L)

        val result = addSimulationTransactionUseCase(
            snapshotId = snapshot.id,
            accountId = 1L,
            amount = 0L,
            date = DateUtils.nowMillis(),
            type = TransactionType.EXPENSE,
            description = null,
            category = "Bills",
        )

        assertTrue(result.isFailure)
        assertEquals("Transaction amount must be greater than 0", result.exceptionOrNull()?.message)
    }

    @Test
    fun addSimulationTransaction_snapshotNotFoundReturnsError() = runBlocking {
        val result = addSimulationTransactionUseCase(
            snapshotId = 999L,
            accountId = 1L,
            amount = 500L,
            date = DateUtils.nowMillis(),
            type = TransactionType.EXPENSE,
            description = null,
            category = "Food",
        )

        assertTrue(result.isFailure)
        assertEquals("Sandbox snapshot with id=999 was not found", result.exceptionOrNull()?.message)
    }

    @Test
    fun getSandboxSafeToSpend_noTransactionsEqualsInitialSafeToSpend() = runBlocking {
        val snapshot = createSnapshot(initialSafeToSpend = 12_000L)

        val result = getSandboxSafeToSpendUseCase(snapshot.id)

        assertTrue(result.isSuccess)
        assertEquals(12_000L, result.getOrThrow())
    }

    @Test
    fun getSandboxSafeToSpend_expenseDecreasesSafeToSpend() = runBlocking {
        val snapshot = createSnapshot(initialSafeToSpend = 12_000L)
        addExpense(snapshot.id, amount = 2_000L)

        val result = getSandboxSafeToSpendUseCase(snapshot.id)

        assertTrue(result.isSuccess)
        assertEquals(10_000L, result.getOrThrow())
    }

    @Test
    fun getSandboxSafeToSpend_incomeIncreasesSafeToSpend() = runBlocking {
        val snapshot = createSnapshot(initialSafeToSpend = 12_000L)
        addIncome(snapshot.id, amount = 3_500L)

        val result = getSandboxSafeToSpendUseCase(snapshot.id)

        assertTrue(result.isSuccess)
        assertEquals(15_500L, result.getOrThrow())
    }

    @Test
    fun getSandboxSafeToSpend_resultNeverNegative() = runBlocking {
        val snapshot = createSnapshot(initialSafeToSpend = 2_000L)
        addExpense(snapshot.id, amount = 5_000L)

        val result = getSandboxSafeToSpendUseCase(snapshot.id)

        assertTrue(result.isSuccess)
        assertEquals(0L, result.getOrThrow())
    }

    @Test
    fun compareSandboxWithReality_computesDifferenceAsSandboxMinusReal() = runBlocking {
        budgetRepository.setTotalSpendingPoolBalance(10_000L)
        budgetRepository.setPendingReservations(0L)
        budgetRepository.setOverdueReservations(0L)

        val snapshot = createSnapshot(initialSafeToSpend = 10_000L)
        addIncome(snapshot.id, amount = 1_000L)

        val result = compareSandboxWithRealityUseCase(snapshot.id)

        assertTrue(result.isSuccess)
        val comparison = result.getOrThrow()
        assertEquals(10_000L, comparison.realSafeToSpend)
        assertEquals(11_000L, comparison.sandboxSafeToSpend)
        assertEquals(1_000L, comparison.difference)
    }

    @Test
    fun compareSandboxWithReality_noTransactionsHasZeroDifference() = runBlocking {
        budgetRepository.setTotalSpendingPoolBalance(9_000L)
        val snapshot = createSnapshot(initialSafeToSpend = 9_000L)

        val result = compareSandboxWithRealityUseCase(snapshot.id)

        assertTrue(result.isSuccess)
        assertEquals(0L, result.getOrThrow().difference)
    }

    @Test
    fun compareSandboxWithReality_negativeDifferenceIsReturned() = runBlocking {
        budgetRepository.setTotalSpendingPoolBalance(9_000L)
        val snapshot = createSnapshot(initialSafeToSpend = 9_000L)
        addExpense(snapshot.id, amount = 2_500L)

        val result = compareSandboxWithRealityUseCase(snapshot.id)

        assertTrue(result.isSuccess)
        assertEquals(-2_500L, result.getOrThrow().difference)
    }

    @Test
    fun promoteTransaction_removesSandboxTransactionAndCreatesRealTransaction() = runBlocking {
        val snapshot = createSnapshot(initialSafeToSpend = 8_000L)
        val sandboxTransaction = addExpense(snapshot.id, amount = 1_500L)

        val result = promoteTransactionUseCase(sandboxTransaction)

        assertTrue(result.isSuccess)
        val promoted = result.getOrThrow()
        assertEquals(sandboxTransaction.amount, promoted.amount)
        assertEquals(sandboxTransaction.accountId, promoted.accountId)
        assertEquals(0, sandboxRepository.allTransactions().size)
        assertEquals(1, transactionRepository.allTransactions().size)
    }

    @Test
    fun promoteTransaction_withOriginalTransactionIdStillRemovesFromSandbox() = runBlocking {
        val real = transactionRepository.createTransaction(
            CreateTransactionRequest(
                accountId = 1L,
                amount = 2_000L,
                date = DateUtils.nowMillis(),
                type = TransactionType.EXPENSE,
                status = TransactionStatus.PENDING,
                description = "Existing",
                category = "Bills",
            ),
        )

        val snapshot = createSnapshot(initialSafeToSpend = 8_000L)
        val sandboxTransaction = sandboxRepository.insertSandboxTransaction(
            snapshotId = snapshot.id,
            accountId = 1L,
            amount = 2_000L,
            date = DateUtils.nowMillis(),
            type = TransactionType.EXPENSE,
            status = TransactionStatus.PENDING,
            description = "Existing",
            category = "Bills",
            originalTransactionId = real.id,
        ).getOrThrow()

        val result = promoteTransactionUseCase(sandboxTransaction)

        assertTrue(result.isSuccess)
        assertEquals(real.id, result.getOrThrow().id)
        assertEquals(0, sandboxRepository.allTransactions().size)
        assertEquals(1, transactionRepository.allTransactions().size)
    }

    @Test
    fun checkAndExpireSandboxes_deletesOlderThan30DaysAndKeepsRecentOnes() = runBlocking {
        val now = DateUtils.nowMillis()
        sandboxRepository.seedSnapshots(
            SandboxSnapshot(
                id = 1L,
                name = "Old",
                description = null,
                createdAt = now - (31L * DateUtils.MILLIS_IN_DAY),
                lastAccessedAt = now - (31L * DateUtils.MILLIS_IN_DAY),
                initialSafeToSpend = 5_000L,
            ),
            SandboxSnapshot(
                id = 2L,
                name = "Recent",
                description = null,
                createdAt = now - (29L * DateUtils.MILLIS_IN_DAY),
                lastAccessedAt = now - (29L * DateUtils.MILLIS_IN_DAY),
                initialSafeToSpend = 6_000L,
            ),
        )

        val result = checkAndExpireSandboxesUseCase()

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow())
        assertEquals(listOf(2L), sandboxRepository.allSnapshots().map { snapshot -> snapshot.id })
    }

    private suspend fun createSnapshot(initialSafeToSpend: Long): SandboxSnapshot {
        return sandboxRepository.createSnapshot(
            name = "Scenario",
            description = null,
            initialSafeToSpend = initialSafeToSpend,
        )
    }

    private suspend fun addExpense(snapshotId: Long, amount: Long): SandboxTransaction {
        return sandboxRepository.insertSandboxTransaction(
            snapshotId = snapshotId,
            accountId = 1L,
            amount = amount,
            date = DateUtils.nowMillis(),
            type = TransactionType.EXPENSE,
            status = TransactionStatus.PENDING,
            description = "Expense",
            category = "General",
            originalTransactionId = null,
        ).getOrThrow()
    }

    private suspend fun addIncome(snapshotId: Long, amount: Long): SandboxTransaction {
        return sandboxRepository.insertSandboxTransaction(
            snapshotId = snapshotId,
            accountId = 1L,
            amount = amount,
            date = DateUtils.nowMillis(),
            type = TransactionType.INCOME,
            status = TransactionStatus.PENDING,
            description = "Income",
            category = "General",
            originalTransactionId = null,
        ).getOrThrow()
    }
}
