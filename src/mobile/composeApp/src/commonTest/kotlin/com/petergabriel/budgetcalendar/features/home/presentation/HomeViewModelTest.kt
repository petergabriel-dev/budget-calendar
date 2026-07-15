package com.petergabriel.budgetcalendar.features.home.presentation

import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType
import com.petergabriel.budgetcalendar.features.accounts.domain.model.CreateAccountRequest
import com.petergabriel.budgetcalendar.features.accounts.testutil.FakeAccountRepository
import com.petergabriel.budgetcalendar.features.creditcard.domain.usecase.GetCreditCardSummariesUseCase
import com.petergabriel.budgetcalendar.features.creditcard.testutil.FakeCreditCardRepository
import com.petergabriel.budgetcalendar.features.transactions.domain.model.CreateTransactionRequest
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
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var creditCardRepository: FakeCreditCardRepository
    private lateinit var transactionRepository: FakeTransactionRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        accountRepository = FakeAccountRepository()
        creditCardRepository = FakeCreditCardRepository()
        transactionRepository = FakeTransactionRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun summaries_sumConfiguredCreditAndClampOverLimitCard() = runTest(dispatcher) {
        createCreditCard(name = "Visa", creditLimit = 10_000L, projectedBalance = -3_000L)
        createCreditCard(name = "Over limit", creditLimit = 10_000L, projectedBalance = -12_000L)
        createCreditCard(name = "No limit", creditLimit = null, projectedBalance = -1_000L)

        val viewModel = HomeViewModel(getCreditCardSummariesUseCase())
        advanceUntilIdle()

        assertEquals(7_000L, viewModel.uiState.value.availableCreditForSpending)
    }

    @Test
    fun summaries_withoutConfiguredLimits_hideAvailableCredit() = runTest(dispatcher) {
        createCreditCard(name = "No limit", creditLimit = null, projectedBalance = -1_000L)

        val viewModel = HomeViewModel(getCreditCardSummariesUseCase())
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.availableCreditForSpending)
    }

    @Test
    fun transactionChange_recalculatesAvailableCredit() = runTest(dispatcher) {
        val card = createCreditCard(name = "Visa", creditLimit = 10_000L, projectedBalance = -1_000L)
        val viewModel = HomeViewModel(getCreditCardSummariesUseCase())
        advanceUntilIdle()
        assertEquals(9_000L, viewModel.uiState.value.availableCreditForSpending)

        creditCardRepository.setProjectedBalance(card.id, -3_000L)
        transactionRepository.createTransaction(
            CreateTransactionRequest(
                accountId = card.id,
                amount = 2_000L,
                date = DateUtils.nowMillis(),
                type = TransactionType.EXPENSE,
                status = TransactionStatus.CONFIRMED,
            ),
        )
        advanceUntilIdle()

        assertEquals(7_000L, viewModel.uiState.value.availableCreditForSpending)
    }

    private suspend fun createCreditCard(
        name: String,
        creditLimit: Long?,
        projectedBalance: Long,
    ) = accountRepository.createAccount(
        CreateAccountRequest(
            name = name,
            type = AccountType.CREDIT_CARD,
            initialBalance = projectedBalance,
            isInSpendingPool = false,
        ),
    ).also { account ->
        creditCardRepository.insert(
            accountId = account.id,
            creditLimit = creditLimit,
            statementBalance = null,
            dueDate = null,
        ).getOrThrow()
        creditCardRepository.setProjectedBalance(account.id, projectedBalance)
    }

    private fun getCreditCardSummariesUseCase() = GetCreditCardSummariesUseCase(
        creditCardRepository = creditCardRepository,
        accountRepository = accountRepository,
        transactionRepository = transactionRepository,
    )
}
