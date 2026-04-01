package com.petergabriel.budgetcalendar.features.recurring.domain.usecase

import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.recurring.domain.model.CreateRecurringTransactionRequest
import com.petergabriel.budgetcalendar.features.recurring.domain.model.RecurrenceType
import com.petergabriel.budgetcalendar.features.recurring.testutil.FakeRecurringTransactionRepository
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import com.petergabriel.budgetcalendar.features.transactions.testutil.FakeTransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RecurringUseCasesTest {
    private lateinit var recurringRepository: FakeRecurringTransactionRepository
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var createRecurringTransactionUseCase: CreateRecurringTransactionUseCase
    private lateinit var generateMonthlyTransactionsUseCase: GenerateMonthlyTransactionsUseCase
    private lateinit var toggleRecurringActiveUseCase: ToggleRecurringActiveUseCase
    private lateinit var deleteRecurringTransactionUseCase: DeleteRecurringTransactionUseCase

    @BeforeTest
    fun setUp() {
        recurringRepository = FakeRecurringTransactionRepository()
        transactionRepository = FakeTransactionRepository()
        createRecurringTransactionUseCase = CreateRecurringTransactionUseCase(recurringRepository)
        generateMonthlyTransactionsUseCase = GenerateMonthlyTransactionsUseCase(recurringRepository, transactionRepository)
        toggleRecurringActiveUseCase = ToggleRecurringActiveUseCase(recurringRepository, generateMonthlyTransactionsUseCase)
        deleteRecurringTransactionUseCase = DeleteRecurringTransactionUseCase(recurringRepository)
    }

    @Test
    fun createRecurringTransaction_validAndInvalidInputs() = runBlocking {
        val validResult = createRecurringTransactionUseCase(
            CreateRecurringTransactionRequest(
                accountId = 1L,
                amount = 12_500L,
                dayOfMonth = 5,
                type = RecurrenceType.INCOME,
                description = "Salary",
            ),
        )

        assertTrue(validResult.isSuccess)
        val recurring = validResult.getOrThrow()
        assertEquals(RecurrenceType.INCOME, recurring.type)
        assertEquals(12_500L, recurring.amount)
        assertEquals(5, recurring.dayOfMonth)

        val invalidAmount = createRecurringTransactionUseCase(
            CreateRecurringTransactionRequest(
                accountId = 1L,
                amount = 0L,
                dayOfMonth = 5,
                type = RecurrenceType.INCOME,
            ),
        )
        assertTrue(invalidAmount.isFailure)
        assertEquals("Amount must be greater than zero", invalidAmount.exceptionOrNull()?.message)

        val invalidDay = createRecurringTransactionUseCase(
            CreateRecurringTransactionRequest(
                accountId = 1L,
                amount = 1_000L,
                dayOfMonth = 32,
                type = RecurrenceType.EXPENSE,
            ),
        )
        assertTrue(invalidDay.isFailure)

        val transferMissingDestination = createRecurringTransactionUseCase(
            CreateRecurringTransactionRequest(
                accountId = 1L,
                amount = 1_000L,
                dayOfMonth = 10,
                type = RecurrenceType.TRANSFER,
            ),
        )
        assertTrue(transferMissingDestination.isFailure)

        val transferSameAccount = createRecurringTransactionUseCase(
            CreateRecurringTransactionRequest(
                accountId = 4L,
                destinationAccountId = 4L,
                amount = 1_000L,
                dayOfMonth = 10,
                type = RecurrenceType.TRANSFER,
            ),
        )
        assertTrue(transferSameAccount.isFailure)
        assertEquals("Cannot transfer to the same account", transferSameAccount.exceptionOrNull()?.message)
    }

    @Test
    fun generateMonthlyTransactions_generatesSkipsAndClamps() = runBlocking {
        recurringRepository.insert(
            CreateRecurringTransactionRequest(
                accountId = 10L,
                amount = 2_500L,
                dayOfMonth = 31,
                type = RecurrenceType.EXPENSE,
                description = "Rent",
                isActive = true,
            ),
        )

        val februaryNow = LocalDate(2026, 2, 5).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

        val firstGeneration = generateMonthlyTransactionsUseCase(
            monthsAhead = 0,
            nowMillis = februaryNow,
            timeZone = TimeZone.UTC,
        )
        assertTrue(firstGeneration.isSuccess)

        val generated = transactionRepository.allTransactions()
        assertEquals(1, generated.size)
        assertEquals(TransactionStatus.PENDING, generated.first().status)

        val generatedDate = kotlin.time.Instant
            .fromEpochMilliseconds(generated.first().date)
            .toLocalDateTime(TimeZone.UTC)
            .date
        assertEquals(2, generatedDate.month.number)
        assertEquals(28, generatedDate.day)

        val secondGeneration = generateMonthlyTransactionsUseCase(
            monthsAhead = 0,
            nowMillis = februaryNow,
            timeZone = TimeZone.UTC,
        )
        assertTrue(secondGeneration.isSuccess)
        assertEquals(1, transactionRepository.allTransactions().size)

        recurringRepository.insert(
            CreateRecurringTransactionRequest(
                accountId = 20L,
                amount = 5_000L,
                dayOfMonth = 15,
                type = RecurrenceType.INCOME,
                isActive = false,
            ),
        )

        val thirdGeneration = generateMonthlyTransactionsUseCase(
            monthsAhead = 0,
            nowMillis = februaryNow,
            timeZone = TimeZone.UTC,
        )
        assertTrue(thirdGeneration.isSuccess)
        assertEquals(1, transactionRepository.allTransactions().size)
    }

    @Test
    fun toggleRecurringActive_deactivateKeepsPending_activateGeneratesCurrentAndFuture() = runBlocking {
        val recurring = recurringRepository.insert(
            CreateRecurringTransactionRequest(
                accountId = 1L,
                amount = 1_000L,
                dayOfMonth = 10,
                type = RecurrenceType.EXPENSE,
                isActive = true,
            ),
        )

        generateMonthlyTransactionsUseCase(monthsAhead = 0)
        val pendingBeforeDeactivate = transactionRepository.getPendingTransactions().first()

        val deactivateResult = toggleRecurringActiveUseCase(recurring.id, false)
        assertTrue(deactivateResult.isSuccess)
        assertEquals(false, deactivateResult.getOrThrow().isActive)

        val pendingAfterDeactivate = transactionRepository.getPendingTransactions().first()
        assertEquals(
            pendingBeforeDeactivate,
            pendingAfterDeactivate,
            "Deactivating should not remove existing pending generated transactions",
        )

        val inactiveRecurring = recurringRepository.insert(
            CreateRecurringTransactionRequest(
                accountId = 3L,
                amount = 4_200L,
                dayOfMonth = 20,
                type = RecurrenceType.INCOME,
                isActive = false,
            ),
        )

        val activateResult = toggleRecurringActiveUseCase(inactiveRecurring.id, true)
        assertTrue(activateResult.isSuccess)

        val generatedForActivated = transactionRepository
            .allTransactions()
            .filter { transaction ->
                RecurringGenerationUtils.extractRecurringId(transaction.description) == inactiveRecurring.id &&
                    transaction.type == TransactionType.INCOME
            }

        assertEquals(2, generatedForActivated.size)

        val (currentMonthStart, _) = DateUtils.currentMonthBounds()
        assertTrue(generatedForActivated.all { transaction -> transaction.date >= currentMonthStart })
    }

    @Test
    fun deleteRecurringTransaction_removesRecurringOnly_keepsGeneratedTransactions() = runBlocking {
        val recurring = recurringRepository.insert(
            CreateRecurringTransactionRequest(
                accountId = 6L,
                amount = 7_000L,
                dayOfMonth = 12,
                type = RecurrenceType.EXPENSE,
                isActive = true,
            ),
        )

        generateMonthlyTransactionsUseCase(monthsAhead = 0)
        val generatedCountBeforeDelete = transactionRepository.allTransactions().size
        assertTrue(generatedCountBeforeDelete > 0)

        val deleteResult = deleteRecurringTransactionUseCase(recurring.id)
        assertTrue(deleteResult.isSuccess)

        val deletedRecurring = recurringRepository.getById(recurring.id)
        assertEquals(null, deletedRecurring)
        assertEquals(generatedCountBeforeDelete, transactionRepository.allTransactions().size)
        assertNotNull(transactionRepository.allTransactions().firstOrNull())
        Unit
    }
}
