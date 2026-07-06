package com.petergabriel.budgetcalendar.features.transactions.presentation

import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType
import com.petergabriel.budgetcalendar.features.accounts.domain.model.CreateAccountRequest
import com.petergabriel.budgetcalendar.features.accounts.domain.usecase.GetAccountsUseCase
import com.petergabriel.budgetcalendar.features.accounts.testutil.FakeAccountRepository
import com.petergabriel.budgetcalendar.features.creditcard.domain.usecase.GetCreditCardSummariesUseCase
import com.petergabriel.budgetcalendar.features.creditcard.testutil.FakeCreditCardRepository
import com.petergabriel.budgetcalendar.features.transactions.domain.usecase.CreateTransactionUseCase
import com.petergabriel.budgetcalendar.features.transactions.domain.usecase.DeleteTransactionUseCase
import com.petergabriel.budgetcalendar.features.transactions.domain.usecase.UpdateTransactionStatusUseCase
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

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionFormViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_emitsCreditCardSummariesForCreditCardAccount() = runTest(dispatcher) {
        val accountRepository = FakeAccountRepository()
        val creditCardRepository = FakeCreditCardRepository()
        val transactionRepository = FakeTransactionRepository()

        val card = accountRepository.createAccount(
            CreateAccountRequest(
                name = "MariBank",
                type = AccountType.CREDIT_CARD,
                initialBalance = -2_500L,
                isInSpendingPool = false,
            ),
        )
        creditCardRepository.insert(
            accountId = card.id,
            creditLimit = 10_000L,
            statementBalance = null,
            dueDate = null,
        ).getOrThrow()
        creditCardRepository.setProjectedBalance(card.id, -2_500L)

        val viewModel = TransactionFormViewModel(
            createTransactionUseCase = CreateTransactionUseCase(transactionRepository),
            updateTransactionStatusUseCase = UpdateTransactionStatusUseCase(transactionRepository),
            deleteTransactionUseCase = DeleteTransactionUseCase(transactionRepository),
            getAccountsUseCase = GetAccountsUseCase(accountRepository),
            getCreditCardSummariesUseCase = GetCreditCardSummariesUseCase(
                creditCardRepository = creditCardRepository,
                accountRepository = accountRepository,
                transactionRepository = transactionRepository,
            ),
        )
        advanceUntilIdle()

        val summary = viewModel.uiState.value.creditCardSummaries.single()
        assertEquals(card.id, summary.accountId)
        assertEquals("MariBank", summary.accountName)
        assertEquals(-2_500L, summary.currentBalance)
        assertEquals(2_500L, summary.reservedAmount)
        assertEquals(7_500L, summary.availableCredit)
    }
}
