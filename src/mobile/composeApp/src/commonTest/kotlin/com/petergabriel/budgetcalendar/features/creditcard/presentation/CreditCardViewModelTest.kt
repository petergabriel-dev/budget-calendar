package com.petergabriel.budgetcalendar.features.creditcard.presentation

import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType
import com.petergabriel.budgetcalendar.features.accounts.domain.model.CreateAccountRequest
import com.petergabriel.budgetcalendar.features.accounts.domain.usecase.GetAccountsUseCase
import com.petergabriel.budgetcalendar.features.accounts.testutil.FakeAccountRepository
import com.petergabriel.budgetcalendar.features.creditcard.domain.model.UpdateCreditCardSettingsRequest
import com.petergabriel.budgetcalendar.features.creditcard.domain.usecase.CreateCreditCardSettingsUseCase
import com.petergabriel.budgetcalendar.features.creditcard.domain.usecase.GetCreditCardSettingsUseCase
import com.petergabriel.budgetcalendar.features.creditcard.domain.usecase.GetCreditCardSummariesUseCase
import com.petergabriel.budgetcalendar.features.creditcard.domain.usecase.GetCreditCardSummaryByIdUseCase
import com.petergabriel.budgetcalendar.features.creditcard.domain.usecase.MakeCreditCardPaymentUseCase
import com.petergabriel.budgetcalendar.features.creditcard.domain.usecase.UpdateCreditCardSettingsUseCase
import com.petergabriel.budgetcalendar.features.creditcard.testutil.FakeCreditCardRepository
import com.petergabriel.budgetcalendar.features.transactions.domain.usecase.GetPendingAndOverdueExpensesByAccountUseCase
import com.petergabriel.budgetcalendar.features.transactions.domain.usecase.CreateTransactionUseCase
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CreditCardViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var creditCardRepository: FakeCreditCardRepository
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var createSettings: CreateCreditCardSettingsUseCase

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        transactionRepository = FakeTransactionRepository()
        accountRepository = FakeAccountRepository(
            transactionProvider = transactionRepository::allTransactions,
            transactionChangedTrigger = transactionRepository.transactionChangedTrigger,
        )
        creditCardRepository = FakeCreditCardRepository()
        createSettings = CreateCreditCardSettingsUseCase(accountRepository, creditCardRepository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun paymentSources_includeSavings_andExcludeCreditCards() = runTest(dispatcher) {
        val savings = accountRepository.createAccount(
            CreateAccountRequest(
                name = "Savings",
                type = AccountType.SAVINGS,
                initialBalance = 20_000L,
            ),
        )
        createCard()

        val viewModel = viewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.paymentSourceAccounts.any { account -> account.id == savings.id })
        assertTrue(viewModel.uiState.value.paymentSourceAccounts.none { account -> account.type == AccountType.CREDIT_CARD })
    }

    @Test
    fun paymentFailure_keepsPaymentSheetOpenAndPreservesError() = runTest(dispatcher) {
        val card = createCard()
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.selectCard(card.id)
        advanceUntilIdle()
        viewModel.openPaymentSheet()
        viewModel.makePayment(sourceAccountId = 999L, amount = 1_000L)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isPaymentSheetVisible)
        assertFalse(viewModel.uiState.value.isPaymentSubmitting)
        assertNotNull(viewModel.uiState.value.paymentError)
    }

    @Test
    fun settingsFailure_keepsSettingsOperationOpenWithScopedError() = runTest(dispatcher) {
        val card = createCard()
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.selectCard(card.id)
        advanceUntilIdle()
        viewModel.updateSettings(
            UpdateCreditCardSettingsRequest(
                creditLimit = 20_000L,
                statementBalance = null,
                dueDate = null,
                statementClosingDay = 32,
                paymentDueDay = 5,
            ),
        )
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSettingsSubmitting)
        assertNotNull(viewModel.uiState.value.settingsError)
        assertNotNull(viewModel.uiState.value.selectedCardSettings)
    }

    private suspend fun createCard() = accountRepository.createAccount(
        CreateAccountRequest(
            name = "Visa",
            type = AccountType.CREDIT_CARD,
            initialBalance = -5_000L,
        ),
    ).also { card ->
        createSettings(
            accountId = card.id,
            creditLimit = 20_000L,
            statementClosingDay = 20,
            paymentDueDay = 5,
        ).getOrThrow()
    }

    private fun viewModel() = CreditCardViewModel(
        getCreditCardSummariesUseCase = GetCreditCardSummariesUseCase(
            creditCardRepository = creditCardRepository,
            accountRepository = accountRepository,
            transactionRepository = transactionRepository,
        ),
        getCreditCardSummaryByIdUseCase = GetCreditCardSummaryByIdUseCase(
            getCreditCardSettingsUseCase = GetCreditCardSettingsUseCase(
                creditCardRepository,
                createSettings,
            ),
            accountRepository = accountRepository,
            creditCardRepository = creditCardRepository,
        ),
        getCreditCardSettingsUseCase = GetCreditCardSettingsUseCase(
            creditCardRepository,
            createSettings,
        ),
        updateCreditCardSettingsUseCase = UpdateCreditCardSettingsUseCase(creditCardRepository),
        makeCreditCardPaymentUseCase = MakeCreditCardPaymentUseCase(
            accountRepository,
            CreateTransactionUseCase(transactionRepository),
        ),
        getPendingAndOverdueExpensesByAccountUseCase = GetPendingAndOverdueExpensesByAccountUseCase(transactionRepository),
        getAccountsUseCase = GetAccountsUseCase(accountRepository),
    )
}
