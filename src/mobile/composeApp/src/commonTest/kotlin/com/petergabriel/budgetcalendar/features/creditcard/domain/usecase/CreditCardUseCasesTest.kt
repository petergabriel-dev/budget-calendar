package com.petergabriel.budgetcalendar.features.creditcard.domain.usecase

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
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var creditCardRepository: FakeCreditCardRepository
    private lateinit var transactionRepository: FakeTransactionRepository

    private lateinit var createCreditCardSettingsUseCase: CreateCreditCardSettingsUseCase
    private lateinit var getCreditCardSettingsUseCase: GetCreditCardSettingsUseCase
    private lateinit var updateCreditCardSettingsUseCase: UpdateCreditCardSettingsUseCase
    private lateinit var calculateReservedAmountUseCase: CalculateReservedAmountUseCase
    private lateinit var getCreditCardSummariesUseCase: GetCreditCardSummariesUseCase
    private lateinit var getCreditCardSummaryByIdUseCase: GetCreditCardSummaryByIdUseCase
    private lateinit var makeCreditCardPaymentUseCase: MakeCreditCardPaymentUseCase

    @BeforeTest
    fun setUp() {
        accountRepository = FakeAccountRepository()
        creditCardRepository = FakeCreditCardRepository()
        transactionRepository = FakeTransactionRepository()

        createCreditCardSettingsUseCase = CreateCreditCardSettingsUseCase(accountRepository, creditCardRepository)
        getCreditCardSettingsUseCase = GetCreditCardSettingsUseCase(creditCardRepository, createCreditCardSettingsUseCase)
        updateCreditCardSettingsUseCase = UpdateCreditCardSettingsUseCase(creditCardRepository)
        calculateReservedAmountUseCase = CalculateReservedAmountUseCase(creditCardRepository)
        getCreditCardSummariesUseCase = GetCreditCardSummariesUseCase(
            creditCardRepository,
            accountRepository,
            calculateReservedAmountUseCase,
        )
        getCreditCardSummaryByIdUseCase = GetCreditCardSummaryByIdUseCase(
            getCreditCardSettingsUseCase,
            accountRepository,
            calculateReservedAmountUseCase,
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
        )

        assertTrue(result.isSuccess)
        val settings = result.getOrThrow()
        assertEquals(card.id, settings.accountId)
        assertEquals(20_000L, settings.creditLimit)
        assertEquals(4_500L, settings.statementBalance)
        assertEquals(1_234_567L, settings.dueDate)
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
        )

        assertTrue(result.isSuccess)
        val updated = result.getOrThrow()
        assertEquals(12_000L, updated.creditLimit)
        assertEquals(2_300L, updated.statementBalance)
        assertEquals(9_999_000L, updated.dueDate)
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
    fun calculateReservedAmount_noPendingExpenses_returnsZero() = runBlocking {
        val card = createAccount("CC A", AccountType.CREDIT_CARD, -1_200L, isInSpendingPool = false)
        createCreditCardSettingsUseCase(accountId = card.id).getOrThrow()

        val result = calculateReservedAmountUseCase(card.id)

        assertTrue(result.isSuccess)
        assertEquals(0L, result.getOrThrow())
    }

    @Test
    fun calculateReservedAmount_multiplePendingOverdueExpenses_returnsSummedAmount() = runBlocking {
        val card = createAccount("CC B", AccountType.CREDIT_CARD, -4_000L, isInSpendingPool = false)
        createCreditCardSettingsUseCase(accountId = card.id).getOrThrow()
        creditCardRepository.setReservedAmount(card.id, 2_750L)

        val result = calculateReservedAmountUseCase(card.id)

        assertTrue(result.isSuccess)
        assertEquals(2_750L, result.getOrThrow())
    }

    @Test
    fun calculateReservedAmount_confirmedCancelledExpensesExcluded_returnsPendingOverdueOnly() = runBlocking {
        val card = createAccount("CC C", AccountType.CREDIT_CARD, -4_000L, isInSpendingPool = false)
        createCreditCardSettingsUseCase(accountId = card.id).getOrThrow()

        // Fake repository stores pre-aggregated reserved amount from pending+overdue expenses only.
        creditCardRepository.setReservedAmount(card.id, 1_900L)

        val result = calculateReservedAmountUseCase(card.id)

        assertTrue(result.isSuccess)
        assertEquals(1_900L, result.getOrThrow())
    }

    @Test
    fun getCreditCardSummaries_multipleCards_producesIndependentSummaries() = runBlocking {
        val visa = createAccount("Visa", AccountType.CREDIT_CARD, -3_000L, isInSpendingPool = false)
        val amex = createAccount("Amex", AccountType.CREDIT_CARD, -5_000L, isInSpendingPool = false)
        createCreditCardSettingsUseCase(accountId = visa.id, creditLimit = 12_000L).getOrThrow()
        createCreditCardSettingsUseCase(accountId = amex.id, creditLimit = 20_000L).getOrThrow()
        creditCardRepository.setReservedAmount(visa.id, 800L)
        creditCardRepository.setReservedAmount(amex.id, 1_900L)

        val summaries = getCreditCardSummariesUseCase().first()

        assertEquals(2, summaries.size)
        assertEquals(
            mapOf(visa.id to 800L, amex.id to 1_900L),
            summaries.associate { summary -> summary.accountId to summary.reservedAmount },
        )
    }

    @Test
    fun getCreditCardSummaries_creditLimitComputesAvailableCredit_withoutLimitReturnsNull() = runBlocking {
        val limited = createAccount("Limited", AccountType.CREDIT_CARD, -3_500L, isInSpendingPool = false)
        val unlimited = createAccount("Unlimited", AccountType.CREDIT_CARD, -1_500L, isInSpendingPool = false)
        createCreditCardSettingsUseCase(accountId = limited.id, creditLimit = 10_000L).getOrThrow()
        createCreditCardSettingsUseCase(accountId = unlimited.id, creditLimit = null).getOrThrow()

        val summaries = getCreditCardSummariesUseCase().first()
        val limitedSummary = summaries.first { summary -> summary.accountId == limited.id }
        val unlimitedSummary = summaries.first { summary -> summary.accountId == unlimited.id }

        assertEquals(6_500L, limitedSummary.availableCredit)
        assertNull(unlimitedSummary.availableCredit)
    }

    @Test
    fun getCreditCardSummaryById_returnsSingleSummaryWithReservedAmount() = runBlocking {
        val card = createAccount("Single", AccountType.CREDIT_CARD, -2_000L, isInSpendingPool = false)
        createCreditCardSettingsUseCase(accountId = card.id, creditLimit = 6_000L).getOrThrow()
        creditCardRepository.setReservedAmount(card.id, 750L)

        val result = getCreditCardSummaryByIdUseCase(card.id)

        assertTrue(result.isSuccess)
        val summary = result.getOrThrow()
        assertEquals(card.id, summary.accountId)
        assertEquals(750L, summary.reservedAmount)
        assertEquals(4_000L, summary.availableCredit)
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
        creditCardRepository.setReservedAmount(destination.id, 700L)

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
