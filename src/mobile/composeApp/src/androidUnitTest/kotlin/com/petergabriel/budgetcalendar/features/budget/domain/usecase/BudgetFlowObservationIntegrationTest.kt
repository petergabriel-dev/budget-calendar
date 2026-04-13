package com.petergabriel.budgetcalendar.features.budget.domain.usecase

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.petergabriel.budgetcalendar.core.database.BudgetCalendarDatabase
import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.accounts.data.mapper.AccountMapper
import com.petergabriel.budgetcalendar.features.accounts.data.repository.AccountRepositoryImpl
import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType
import com.petergabriel.budgetcalendar.features.accounts.domain.model.CreateAccountRequest
import com.petergabriel.budgetcalendar.features.accounts.domain.usecase.CreateAccountUseCase
import com.petergabriel.budgetcalendar.features.budget.data.mapper.BudgetMapper
import com.petergabriel.budgetcalendar.features.budget.data.repository.BudgetRepositoryImpl
import com.petergabriel.budgetcalendar.features.budget.data.repository.MonthlyRolloverRepositoryImpl
import com.petergabriel.budgetcalendar.features.transactions.data.mapper.TransactionMapper
import com.petergabriel.budgetcalendar.features.transactions.data.repository.TransactionRepositoryImpl
import com.petergabriel.budgetcalendar.features.transactions.domain.model.CreateTransactionRequest
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import com.petergabriel.budgetcalendar.features.transactions.domain.model.UpdateTransactionStatusRequest
import com.petergabriel.budgetcalendar.features.transactions.domain.usecase.CreateTransactionUseCase
import com.petergabriel.budgetcalendar.features.transactions.domain.usecase.UpdateTransactionStatusUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BudgetFlowObservationIntegrationTest {

    @Test
    fun calculateSafeToSpend_reemitsAfterCreatingPendingExpense() = runBlocking {
        val fixture = createFixture()

        try {
            val createAccountResult = fixture.createAccountUseCase(
                CreateAccountRequest(
                    name = "Primary Checking",
                    type = AccountType.CHECKING,
                    initialBalance = 500_00L,
                    isInSpendingPool = true,
                ),
            )
            assertTrue(createAccountResult.isSuccess)
            val account = createAccountResult.getOrThrow()

            val emissions = mutableListOf<com.petergabriel.budgetcalendar.features.budget.domain.model.BudgetSummary>()
            val collectJob = launch {
                withTimeout(5_000) {
                    fixture.calculateSafeToSpendUseCase()
                        .first { summary ->
                            emissions += summary
                            summary.pendingReservations == 75_00L
                        }
                }
            }

            withTimeout(5_000) {
                while (emissions.isEmpty()) {
                    delay(10)
                }
            }

            val initial = emissions.first()
            assertEquals(500_00L, initial.availableToSpend)
            assertEquals(0L, initial.pendingReservations)

            val createTransactionResult = fixture.createTransactionUseCase(
                CreateTransactionRequest(
                    accountId = account.id,
                    amount = 75_00L,
                    date = DateUtils.startOfDayMillis(DateUtils.nowMillis()),
                    type = TransactionType.EXPENSE,
                    status = TransactionStatus.PENDING,
                    description = "Groceries",
                    category = "Food",
                ),
            )
            assertTrue(createTransactionResult.isSuccess)

            collectJob.join()

            val updated = emissions.last { summary -> summary.pendingReservations == 75_00L }
            assertEquals(75_00L, updated.pendingReservations)
            assertEquals(425_00L, updated.availableToSpend)
        } finally {
            fixture.driver.close()
        }
    }

    @Test
    fun createConfirmedExpense_deductsAccountBalance() = runBlocking {
        val fixture = createFixture()

        try {
            val account = fixture.createAccountUseCase(
                CreateAccountRequest(
                    name = "Primary Checking",
                    type = AccountType.CHECKING,
                    initialBalance = 500_00L,
                    isInSpendingPool = true,
                ),
            ).getOrThrow()

            val created = fixture.createTransactionUseCase(
                CreateTransactionRequest(
                    accountId = account.id,
                    amount = 75_00L,
                    date = DateUtils.startOfDayMillis(DateUtils.nowMillis()),
                    type = TransactionType.EXPENSE,
                    status = TransactionStatus.CONFIRMED,
                    description = "Confirmed expense",
                    category = "Food",
                ),
            )
            assertTrue(created.isSuccess)
            assertEquals(TransactionStatus.CONFIRMED, created.getOrThrow().status)

            val summary = withTimeout(5_000) {
                fixture.calculateSafeToSpendUseCase().first { sts ->
                    sts.pendingReservations == 0L &&
                        sts.availableToSpend == 425_00L &&
                        sts.confirmedSpending == 75_00L
                }
            }

            assertEquals(425_00L, summary.availableToSpend)
            assertEquals(75_00L, summary.confirmedSpending)
            assertEquals(425_00L, fixture.accountRepository.getAccountById(account.id)?.balance)
        } finally {
            fixture.driver.close()
        }
    }

    @Test
    fun confirmingPendingExpense_updatesAccountBalance_andKeepsSafeToSpendStable() = runBlocking {
        val fixture = createFixture()

        try {
            val account = fixture.createAccountUseCase(
                CreateAccountRequest(
                    name = "Primary Checking",
                    type = AccountType.CHECKING,
                    initialBalance = 500_00L,
                    isInSpendingPool = true,
                ),
            ).getOrThrow()

            val pendingExpense = fixture.createTransactionUseCase(
                CreateTransactionRequest(
                    accountId = account.id,
                    amount = 75_00L,
                    date = DateUtils.startOfDayMillis(DateUtils.nowMillis()),
                    type = TransactionType.EXPENSE,
                    status = TransactionStatus.PENDING,
                    description = "Pending expense",
                ),
            ).getOrThrow()

            val pendingSummary = withTimeout(5_000) {
                fixture.calculateSafeToSpendUseCase().first { summary ->
                    summary.pendingReservations == 75_00L
                }
            }
            assertEquals(425_00L, pendingSummary.availableToSpend)

            val confirmResult = fixture.updateTransactionStatusUseCase(
                pendingExpense.id,
                UpdateTransactionStatusRequest(status = TransactionStatus.CONFIRMED),
            )
            assertTrue(confirmResult.isSuccess)

            val confirmedSummary = withTimeout(5_000) {
                fixture.calculateSafeToSpendUseCase().first { summary ->
                    summary.pendingReservations == 0L && summary.availableToSpend == 425_00L
                }
            }

            assertEquals(425_00L, confirmedSummary.availableToSpend)
            assertEquals(75_00L, confirmedSummary.confirmedSpending)
            assertEquals(425_00L, fixture.accountRepository.getAccountById(account.id)?.balance)
        } finally {
            fixture.driver.close()
        }
    }

    private fun createFixture(): Fixture {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        BudgetCalendarDatabase.Schema.create(driver)
        val database = BudgetCalendarDatabase(driver)

        val transactionRepository = TransactionRepositoryImpl(database, TransactionMapper())
        val accountRepository = AccountRepositoryImpl(database, AccountMapper(), transactionRepository)
        val budgetMapper = BudgetMapper()
        val budgetRepository = BudgetRepositoryImpl(database, budgetMapper, transactionRepository)
        val monthlyRolloverRepository = MonthlyRolloverRepositoryImpl(database, budgetMapper)

        return Fixture(
            driver = driver,
            accountRepository = accountRepository,
            createAccountUseCase = CreateAccountUseCase(accountRepository),
            createTransactionUseCase = CreateTransactionUseCase(transactionRepository),
            updateTransactionStatusUseCase = UpdateTransactionStatusUseCase(transactionRepository),
            calculateSafeToSpendUseCase = CalculateSafeToSpendUseCase(
                budgetRepository,
                monthlyRolloverRepository,
                transactionRepository,
            ),
        )
    }

    private data class Fixture(
        val driver: JdbcSqliteDriver,
        val accountRepository: AccountRepositoryImpl,
        val createAccountUseCase: CreateAccountUseCase,
        val createTransactionUseCase: CreateTransactionUseCase,
        val updateTransactionStatusUseCase: UpdateTransactionStatusUseCase,
        val calculateSafeToSpendUseCase: CalculateSafeToSpendUseCase,
    )
}
