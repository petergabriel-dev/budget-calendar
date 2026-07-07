package com.petergabriel.budgetcalendar.features.creditcard.domain.usecase

import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account
import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType
import com.petergabriel.budgetcalendar.features.accounts.domain.model.CreateAccountRequest
import com.petergabriel.budgetcalendar.features.accounts.testutil.FakeAccountRepository
import com.petergabriel.budgetcalendar.features.creditcard.testutil.FakeCreditCardRepository
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import com.petergabriel.budgetcalendar.features.transactions.domain.usecase.CreateTransactionUseCase
import com.petergabriel.budgetcalendar.features.transactions.testutil.FakeTransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CreditCardUseCasesTest {
    private val fixedNowMillis = 1_774_656_000_000L
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var creditCardRepository: FakeCreditCardRepository
    private lateinit var transactionRepository: FakeTransactionRepository

    private lateinit var createCreditCardSettingsUseCase: CreateCreditCardSettingsUseCase
    private lateinit var getCreditCardSettingsUseCase: GetCreditCardSettingsUseCase
    private lateinit var updateCreditCardSettingsUseCase: UpdateCreditCardSettingsUseCase
    private lateinit var getCreditCardSummariesUseCase: GetCreditCardSummariesUseCase
    private lateinit var getCreditCardSummaryByIdUseCase: GetCreditCardSummaryByIdUseCase
    private lateinit var makeCreditCardPaymentUseCase: MakeCreditCardPaymentUseCase

    @BeforeTest
    fun setUp() {
        transactionRepository = FakeTransactionRepository()
        accountRepository = FakeAccountRepository(
            transactionProvider = transactionRepository::allTransactions,
            transactionChangedTrigger = transactionRepository.transactionChangedTrigger,
        )
        creditCardRepository = FakeCreditCardRepository()

        createCreditCardSettingsUseCase = CreateCreditCardSettingsUseCase(accountRepository, creditCardRepository)
        getCreditCardSettingsUseCase = GetCreditCardSettingsUseCase(creditCardRepository, createCreditCardSettingsUseCase)
        updateCreditCardSettingsUseCase = UpdateCreditCardSettingsUseCase(creditCardRepository)
        getCreditCardSummariesUseCase = GetCreditCardSummariesUseCase(
            creditCardRepository,
            accountRepository,
            transactionRepository,
            nowMillis = { fixedNowMillis },
        )
        getCreditCardSummaryByIdUseCase = GetCreditCardSummaryByIdUseCase(
            getCreditCardSettingsUseCase,
            accountRepository,
            creditCardRepository,
            nowMillis = { fixedNowMillis },
        )
        makeCreditCardPaymentUseCase = MakeCreditCardPaymentUseCase(
            accountRepository,
            CreateTransactionUseCase(transactionRepository),
        )
    }

    @Test
    fun createCreditCardSettings_validCreditCardAccount_createsSettings() = runBlocking {
        val card = createAccount("Visa", AccountType.CREDIT_CARD, -5_000L, isInSpendingPool = false)

        val result = createCreditCardSettingsUseCase(
            accountId = card.id,
            creditLimit = 20_000L,
            statementBalance = 4_500L,
            dueDate = 1_234_567L,
            statementClosingDay = 28,
            paymentDueDay = 15,
        )

        assertTrue(result.isSuccess)
        val settings = result.getOrThrow()
        assertEquals(card.id, settings.accountId)
        assertEquals(20_000L, settings.creditLimit)
        assertEquals(4_500L, settings.statementBalance)
        assertEquals(1_234_567L, settings.dueDate)
        assertEquals(28, settings.statementClosingDay)
        assertEquals(15, settings.paymentDueDay)
    }

    @Test
    fun createCreditCardSettings_nonCreditCardAccount_returnsError() = runBlocking {
        val checking = createAccount("Checking", AccountType.CHECKING, 10_000L, isInSpendingPool = true)

        val result = createCreditCardSettingsUseCase(accountId = checking.id)

        assertTrue(result.isFailure)
        assertEquals(
            "Credit card settings can only be created for credit card accounts",
            result.exceptionOrNull()?.message,
        )
    }

    @Test
    fun createCreditCardSettings_missingAccount_returnsError() = runBlocking {
        val result = createCreditCardSettingsUseCase(accountId = 999L)

        assertTrue(result.isFailure)
        assertEquals("Account with id=999 was not found", result.exceptionOrNull()?.message)
    }

    @Test
    fun getCreditCardSettings_existingSettings_returnsStoredRow() = runBlocking {
        val card = createAccount("Mastercard", AccountType.CREDIT_CARD, -3_200L, isInSpendingPool = false)
        val created = createCreditCardSettingsUseCase(
            accountId = card.id,
            creditLimit = 15_000L,
            statementBalance = 3_200L,
            dueDate = 4_567_890L,
        ).getOrThrow()

        val result = getCreditCardSettingsUseCase(card.id)

        assertTrue(result.isSuccess)
        assertEquals(created.id, result.getOrThrow().id)
    }

    @Test
    fun getCreditCardSettings_missingRow_autoCreatesWithNullOptionals() = runBlocking {
        val card = createAccount("Amex", AccountType.CREDIT_CARD, -1_900L, isInSpendingPool = false)

        val result = getCreditCardSettingsUseCase(card.id)

        assertTrue(result.isSuccess)
        val settings = result.getOrThrow()
        assertEquals(card.id, settings.accountId)
        assertNull(settings.creditLimit)
        assertNull(settings.statementBalance)
        assertNull(settings.dueDate)
        assertEquals(1, creditCardRepository.getAllSettings().first().size)
    }

    @Test
    fun updateCreditCardSettings_validUpdate_persistsChanges() = runBlocking {
        val card = createAccount("Union", AccountType.CREDIT_CARD, -2_100L, isInSpendingPool = false)
        createCreditCardSettingsUseCase(accountId = card.id).getOrThrow()

        val result = updateCreditCardSettingsUseCase(
            accountId = card.id,
            creditLimit = 12_000L,
            statementBalance = 2_300L,
            dueDate = 9_999_000L,
            statementClosingDay = 31,
            paymentDueDay = 9,
        )

        assertTrue(result.isSuccess)
        val updated = result.getOrThrow()
        assertEquals(12_000L, updated.creditLimit)
        assertEquals(2_300L, updated.statementBalance)
        assertEquals(9_999_000L, updated.dueDate)
        assertEquals(31, updated.statementClosingDay)
        assertEquals(9, updated.paymentDueDay)

        val loaded = getCreditCardSettingsUseCase(card.id).getOrThrow()
        assertEquals(31, loaded.statementClosingDay)
        assertEquals(9, loaded.paymentDueDay)
    }

    @Test
    fun updateCreditCardSettings_invalidCycleDay_returnsValidationError() = runBlocking {
        val card = createAccount("Discover", AccountType.CREDIT_CARD, -800L, isInSpendingPool = false)
        createCreditCardSettingsUseCase(accountId = card.id).getOrThrow()

        val result = updateCreditCardSettingsUseCase(
            accountId = card.id,
            creditLimit = null,
            statementBalance = null,
            dueDate = null,
            statementClosingDay = 32,
            paymentDueDay = 9,
        )

        assertTrue(result.isFailure)
        assertEquals("Statement cycle days must be 1-31", result.exceptionOrNull()?.message)
    }

    @Test
    fun updateCreditCardSettings_negativeCreditLimit_returnsValidationError() = runBlocking {
        val card = createAccount("JCB", AccountType.CREDIT_CARD, -800L, isInSpendingPool = false)
        createCreditCardSettingsUseCase(accountId = card.id).getOrThrow()

        val result = updateCreditCardSettingsUseCase(
            accountId = card.id,
            creditLimit = -1L,
            statementBalance = null,
            dueDate = null,
        )

        assertTrue(result.isFailure)
        assertEquals("Credit limit must be >= 0", result.exceptionOrNull()?.message)
    }

    @Test
    fun getCreditCardSummaries_multipleCards_producesIndependentSummaries() = runBlocking {
        val visa = createAccount("Visa", AccountType.CREDIT_CARD, -3_000L, isInSpendingPool = false)
        val amex = createAccount("Amex", AccountType.CREDIT_CARD, -5_000L, isInSpendingPool = false)
        createCreditCardSettingsUseCase(
            accountId = visa.id,
            creditLimit = 12_000L,
            statementClosingDay = 28,
            paymentDueDay = 15,
        ).getOrThrow()
        createCreditCardSettingsUseCase(accountId = amex.id, creditLimit = 20_000L).getOrThrow()
        creditCardRepository.setProjectedBalance(visa.id, -800L)
        creditCardRepository.setProjectedBalance(amex.id, -1_900L)

        val summaries = getCreditCardSummariesUseCase().first()

        assertEquals(2, summaries.size)
        assertEquals(
            mapOf(visa.id to 800L, amex.id to 1_900L),
            summaries.associate { summary -> summary.accountId to summary.reservedAmount },
        )
        val visaSummary = summaries.first { summary -> summary.accountId == visa.id }
        assertEquals(28, visaSummary.statementClosingDay)
        assertEquals(15, visaSummary.paymentDueDay)
    }

    @Test
    fun getCreditCardSummaries_creditLimitComputesAvailableCredit_withoutLimitReturnsNull() = runBlocking {
        val limited = createAccount("Limited", AccountType.CREDIT_CARD, -3_500L, isInSpendingPool = false)
        val unlimited = createAccount("Unlimited", AccountType.CREDIT_CARD, -1_500L, isInSpendingPool = false)
        createCreditCardSettingsUseCase(accountId = limited.id, creditLimit = 10_000L).getOrThrow()
        createCreditCardSettingsUseCase(accountId = unlimited.id, creditLimit = null).getOrThrow()
        creditCardRepository.setProjectedBalance(limited.id, -3_500L)
        creditCardRepository.setProjectedBalance(unlimited.id, -1_500L)

        val summaries = getCreditCardSummariesUseCase().first()
        val limitedSummary = summaries.first { summary -> summary.accountId == limited.id }
        val unlimitedSummary = summaries.first { summary -> summary.accountId == unlimited.id }

        assertEquals(6_500L, limitedSummary.availableCredit)
        assertNull(unlimitedSummary.availableCredit)
    }

    @Test
    fun getCreditCardSummaries_statementDueUsesLastCloseBalanceAndPaymentsSince() = runBlocking {
        val card = createAccount("Due", AccountType.CREDIT_CARD, -4_000L, isInSpendingPool = false)
        createCreditCardSettingsUseCase(
            accountId = card.id,
            creditLimit = 20_000L,
            statementClosingDay = 20,
            paymentDueDay = 5,
        ).getOrThrow()
        val lastClose = DateUtils.lastStatementClose(20, fixedNowMillis)
        creditCardRepository.setProjectedBalance(card.id, -7_000L)
        creditCardRepository.setBalanceAsOf(card.id, lastClose, -5_000L)
        creditCardRepository.setPaymentsSince(card.id, lastClose, 1_500L)

        val summary = getCreditCardSummariesUseCase().first().single()

        assertEquals(3_500L, summary.statementDueAmount)
        assertEquals(DateUtils.paymentDueAfter(5, lastClose), summary.statementDueDate)
    }

    @Test
    fun getCreditCardSummaries_postClosingChargeDoesNotIncreaseStatementDue() = runBlocking {
        val card = createAccount("Post Close", AccountType.CREDIT_CARD, -4_000L, isInSpendingPool = false)
        createCreditCardSettingsUseCase(
            accountId = card.id,
            creditLimit = 20_000L,
            statementClosingDay = 20,
            paymentDueDay = 5,
        ).getOrThrow()
        val lastClose = DateUtils.lastStatementClose(20, fixedNowMillis)
        creditCardRepository.setProjectedBalance(card.id, -9_000L)
        creditCardRepository.setBalanceAsOf(card.id, lastClose, -5_000L)
        creditCardRepository.setPaymentsSince(card.id, lastClose, 0L)

        val summary = getCreditCardSummariesUseCase().first().single()

        assertEquals(5_000L, summary.statementDueAmount)
    }

    @Test
    fun getCreditCardSummaries_missingCycleLeavesStatementDueNull() = runBlocking {
        val card = createAccount("No Cycle", AccountType.CREDIT_CARD, -4_000L, isInSpendingPool = false)
        createCreditCardSettingsUseCase(accountId = card.id, creditLimit = 20_000L).getOrThrow()
        creditCardRepository.setProjectedBalance(card.id, -4_000L)

        val summary = getCreditCardSummariesUseCase().first().single()

        assertNull(summary.statementDueAmount)
        assertNull(summary.statementDueDate)
    }

    @Test
    fun getCreditCardSummaryById_returnsSingleSummaryWithReservedAmount() = runBlocking {
        val card = createAccount("Single", AccountType.CREDIT_CARD, -2_000L, isInSpendingPool = false)
        createCreditCardSettingsUseCase(
            accountId = card.id,
            creditLimit = 6_000L,
            statementClosingDay = 31,
            paymentDueDay = 9,
        ).getOrThrow()
        val lastClose = DateUtils.lastStatementClose(31, fixedNowMillis)
        creditCardRepository.setProjectedBalance(card.id, -750L)
        creditCardRepository.setBalanceAsOf(card.id, lastClose, -1_200L)
        creditCardRepository.setPaymentsSince(card.id, lastClose, 200L)

        val result = getCreditCardSummaryByIdUseCase(card.id)

        assertTrue(result.isSuccess)
        val summary = result.getOrThrow()
        assertEquals(card.id, summary.accountId)
        assertEquals(750L, summary.reservedAmount)
        assertEquals(5_250L, summary.availableCredit)
        assertEquals(31, summary.statementClosingDay)
        assertEquals(9, summary.paymentDueDay)
        assertEquals(1_000L, summary.statementDueAmount)
        assertEquals(DateUtils.paymentDueAfter(9, lastClose), summary.statementDueDate)
    }

    @Test
    fun makeCreditCardPayment_validPayment_createsTransfer() = runBlocking {
        val source = createAccount("Checking", AccountType.CHECKING, 30_000L, isInSpendingPool = true)
        val destination = createAccount("Visa", AccountType.CREDIT_CARD, -3_000L, isInSpendingPool = false)
        createCreditCardSettingsUseCase(accountId = destination.id).getOrThrow()

        val result = makeCreditCardPaymentUseCase(
            sourceAccountId = source.id,
            ccAccountId = destination.id,
            amount = 1_500L,
        )

        assertTrue(result.isSuccess)
        val payment = result.getOrThrow()
        assertEquals(TransactionType.TRANSFER, payment.type)
        assertEquals(source.id, payment.accountId)
        assertEquals(destination.id, payment.destinationAccountId)
        assertEquals(2, transactionRepository.allTransactions().size)
    }

    @Test
    fun makeCreditCardPayment_amountLessOrEqualZero_returnsError() = runBlocking {
        val source = createAccount("Checking", AccountType.CHECKING, 30_000L, isInSpendingPool = true)
        val destination = createAccount("Visa", AccountType.CREDIT_CARD, -3_000L, isInSpendingPool = false)
        createCreditCardSettingsUseCase(accountId = destination.id).getOrThrow()

        val result = makeCreditCardPaymentUseCase(
            sourceAccountId = source.id,
            ccAccountId = destination.id,
            amount = 0L,
        )

        assertTrue(result.isFailure)
        assertEquals("Payment amount must be greater than 0", result.exceptionOrNull()?.message)
    }

    @Test
    fun makeCreditCardPayment_sourceEqualsDestination_returnsError() = runBlocking {
        val card = createAccount("Visa", AccountType.CREDIT_CARD, -3_000L, isInSpendingPool = false)
        createCreditCardSettingsUseCase(accountId = card.id).getOrThrow()

        val result = makeCreditCardPaymentUseCase(
            sourceAccountId = card.id,
            ccAccountId = card.id,
            amount = 500L,
        )

        assertTrue(result.isFailure)
        assertEquals("Source and destination accounts must be different", result.exceptionOrNull()?.message)
    }

    @Test
    fun makeCreditCardPayment_destinationNotCreditCard_returnsError() = runBlocking {
        val source = createAccount("Checking", AccountType.CHECKING, 30_000L, isInSpendingPool = true)
        val destination = createAccount("Savings", AccountType.SAVINGS, 20_000L, isInSpendingPool = false)

        val result = makeCreditCardPaymentUseCase(
            sourceAccountId = source.id,
            ccAccountId = destination.id,
            amount = 500L,
        )

        assertTrue(result.isFailure)
        assertEquals("Destination must be a credit card account", result.exceptionOrNull()?.message)
    }

    @Test
    fun makeCreditCardPayment_amountGreaterThanReserved_isAllowed() = runBlocking {
        val source = createAccount("Checking", AccountType.CHECKING, 30_000L, isInSpendingPool = true)
        val destination = createAccount("Visa", AccountType.CREDIT_CARD, -3_000L, isInSpendingPool = false)
        createCreditCardSettingsUseCase(accountId = destination.id).getOrThrow()
        creditCardRepository.setProjectedBalance(destination.id, -700L)

        val result = makeCreditCardPaymentUseCase(
            sourceAccountId = source.id,
            ccAccountId = destination.id,
            amount = 1_250L,
        )

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().id > 0L)
    }

    private suspend fun createAccount(
        name: String,
        type: AccountType,
        initialBalance: Long,
        isInSpendingPool: Boolean,
    ): Account {
        return accountRepository.createAccount(
            CreateAccountRequest(
                name = name,
                type = type,
                initialBalance = initialBalance,
                isInSpendingPool = isInSpendingPool,
            ),
        )
    }
}
