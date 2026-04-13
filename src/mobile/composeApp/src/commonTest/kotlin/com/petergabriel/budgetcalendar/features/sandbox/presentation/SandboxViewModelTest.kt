package com.petergabriel.budgetcalendar.features.sandbox.presentation

import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType
import com.petergabriel.budgetcalendar.features.accounts.domain.model.CreateAccountRequest
import com.petergabriel.budgetcalendar.features.accounts.testutil.FakeAccountRepository
import com.petergabriel.budgetcalendar.features.budget.domain.usecase.CalculateSafeToSpendUseCase
import com.petergabriel.budgetcalendar.features.budget.testutil.FakeBudgetRepository
import com.petergabriel.budgetcalendar.features.budget.testutil.FakeMonthlyRolloverRepository
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.AddSandboxTransactionRequest
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxSnapshot
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.AddSimulationTransactionUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.CheckAndExpireSandboxesUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.CompareSandboxWithRealityUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.CreateSandboxUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.DeleteSandboxUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.GetSandboxByIdUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.GetSandboxSafeToSpendUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.GetSandboxTransactionsUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.GetSandboxesUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.PromoteTransactionUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.RemoveSimulationTransactionUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.UpdateSnapshotLastAccessedUseCase
import com.petergabriel.budgetcalendar.features.sandbox.testutil.FakeSandboxRepository
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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SandboxViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private lateinit var sandboxRepository: FakeSandboxRepository
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var budgetRepository: FakeBudgetRepository
    private lateinit var monthlyRolloverRepository: FakeMonthlyRolloverRepository

    private lateinit var viewModel: SandboxViewModel
    private var accountId: Long = 0L

    @BeforeTest
    fun setUp() = runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)

        sandboxRepository = FakeSandboxRepository()
        transactionRepository = FakeTransactionRepository()
        accountRepository = FakeAccountRepository(
            transactionProvider = transactionRepository::allTransactions,
            transactionChangedTrigger = transactionRepository.transactionChangedTrigger,
        )
        budgetRepository = FakeBudgetRepository()
        monthlyRolloverRepository = FakeMonthlyRolloverRepository()

        val account = accountRepository.createAccount(
            CreateAccountRequest(
                name = "Primary",
                type = AccountType.CHECKING,
                initialBalance = 100_000L,
                isInSpendingPool = true,
            ),
        )
        accountId = account.id

        budgetRepository.setTotalSpendingPoolBalance(10_000L)
        budgetRepository.setPendingReservations(0L)
        budgetRepository.setOverdueReservations(0L)

        val calculateSafeToSpendUseCase = CalculateSafeToSpendUseCase(
            budgetRepository,
            monthlyRolloverRepository,
            transactionRepository,
        )
        val getSandboxSafeToSpendUseCase = GetSandboxSafeToSpendUseCase(sandboxRepository)

        viewModel = SandboxViewModel(
            createSandboxUseCase = CreateSandboxUseCase(
                sandboxRepository,
                calculateSafeToSpendUseCase,
            ),
            getSandboxesUseCase = GetSandboxesUseCase(sandboxRepository),
            getSandboxByIdUseCase = GetSandboxByIdUseCase(sandboxRepository),
            deleteSandboxUseCase = DeleteSandboxUseCase(sandboxRepository),
            addSimulationTransactionUseCase = AddSimulationTransactionUseCase(
                sandboxRepository,
                accountRepository,
            ),
            removeSimulationTransactionUseCase = RemoveSimulationTransactionUseCase(sandboxRepository),
            getSandboxTransactionsUseCase = GetSandboxTransactionsUseCase(sandboxRepository),
            getSandboxSafeToSpendUseCase = getSandboxSafeToSpendUseCase,
            compareSandboxWithRealityUseCase = CompareSandboxWithRealityUseCase(
                calculateSafeToSpendUseCase,
                getSandboxSafeToSpendUseCase,
                sandboxRepository,
            ),
            promoteTransactionUseCase = PromoteTransactionUseCase(
                transactionRepository,
                sandboxRepository,
            ),
            updateSnapshotLastAccessedUseCase = UpdateSnapshotLastAccessedUseCase(sandboxRepository),
            checkAndExpireSandboxesUseCase = CheckAndExpireSandboxesUseCase(sandboxRepository),
            calculateSafeToSpendUseCase = calculateSafeToSpendUseCase,
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun selectSnapshot_startsReactiveCollectionsAndUpdatesState() = runTest(dispatcher) {
        val snapshot = createSnapshot(initialSafeToSpend = 10_000L)

        viewModel.setSandboxMode(true)
        advanceUntilIdle()
        viewModel.selectSnapshot(snapshot.id)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isSandboxMode)
        assertEquals(snapshot.id, state.activeSnapshot?.id)
        assertEquals(10_000L, state.projectedSafeToSpend)
        assertEquals(emptyList(), state.sandboxTransactions)
        assertEquals(0L, state.comparison?.difference)
    }

    @Test
    fun addTransaction_updatesTransactionsAndProjectedSafeToSpend() = runTest(dispatcher) {
        val snapshot = createSnapshot(initialSafeToSpend = 10_000L)
        viewModel.setSandboxMode(true)
        advanceUntilIdle()
        viewModel.selectSnapshot(snapshot.id)
        advanceUntilIdle()

        viewModel.addTransaction(
            AddSandboxTransactionRequest(
                snapshotId = snapshot.id,
                accountId = accountId,
                amount = 1_500L,
                date = DateUtils.nowMillis(),
                type = TransactionType.EXPENSE,
                category = "Groceries",
            ),
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.sandboxTransactions.size)
        assertEquals(8_500L, state.projectedSafeToSpend)
    }

    @Test
    fun removeTransaction_updatesTransactionsAndProjectedSafeToSpend() = runTest(dispatcher) {
        val snapshot = createSnapshot(initialSafeToSpend = 10_000L)
        viewModel.setSandboxMode(true)
        advanceUntilIdle()
        viewModel.selectSnapshot(snapshot.id)
        advanceUntilIdle()

        val inserted = sandboxRepository.insertSandboxTransaction(
            snapshotId = snapshot.id,
            accountId = accountId,
            amount = 1_000L,
            date = DateUtils.nowMillis(),
            type = TransactionType.EXPENSE,
            status = TransactionStatus.PENDING,
            description = "Expense",
            category = "General",
            originalTransactionId = null,
        ).getOrThrow()
        advanceUntilIdle()

        viewModel.removeTransaction(inserted.id)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(emptyList(), state.sandboxTransactions)
        assertEquals(10_000L, state.projectedSafeToSpend)
    }

    @Test
    fun setSandboxModeFalse_cancelsSnapshotCollection() = runTest(dispatcher) {
        val snapshot = createSnapshot(initialSafeToSpend = 10_000L)
        viewModel.setSandboxMode(true)
        advanceUntilIdle()
        viewModel.selectSnapshot(snapshot.id)
        advanceUntilIdle()

        viewModel.setSandboxMode(false)
        advanceUntilIdle()

        sandboxRepository.insertSandboxTransaction(
            snapshotId = snapshot.id,
            accountId = accountId,
            amount = 900L,
            date = DateUtils.nowMillis(),
            type = TransactionType.EXPENSE,
            status = TransactionStatus.PENDING,
            description = "Should not update state",
            category = "General",
            originalTransactionId = null,
        ).getOrThrow()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(!state.isSandboxMode)
        assertEquals(emptyList(), state.sandboxTransactions)
        assertEquals(0L, state.projectedSafeToSpend)
    }

    @Test
    fun promoteTransaction_removesSandboxTransactionAndProjectedExcludesIt() = runTest(dispatcher) {
        val snapshot = createSnapshot(initialSafeToSpend = 10_000L)
        viewModel.setSandboxMode(true)
        advanceUntilIdle()
        viewModel.selectSnapshot(snapshot.id)
        advanceUntilIdle()

        val inserted = sandboxRepository.insertSandboxTransaction(
            snapshotId = snapshot.id,
            accountId = accountId,
            amount = 1_200L,
            date = DateUtils.nowMillis(),
            type = TransactionType.EXPENSE,
            status = TransactionStatus.PENDING,
            description = "Promote me",
            category = "General",
            originalTransactionId = null,
        ).getOrThrow()
        advanceUntilIdle()

        assertEquals(8_800L, viewModel.uiState.value.projectedSafeToSpend)

        viewModel.promoteTransaction(inserted.id)
        advanceUntilIdle()

        assertEquals(emptyList(), viewModel.uiState.value.sandboxTransactions)
        assertEquals(10_000L, viewModel.uiState.value.projectedSafeToSpend)
        assertEquals(1, transactionRepository.allTransactions().size)
    }

    private suspend fun createSnapshot(initialSafeToSpend: Long): SandboxSnapshot {
        return sandboxRepository.createSnapshot(
            name = "Scenario",
            description = null,
            initialSafeToSpend = initialSafeToSpend,
        )
    }
}
