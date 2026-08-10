package com.petergabriel.budgetcalendar.features.accounts.presentation

import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType
import com.petergabriel.budgetcalendar.features.accounts.domain.model.CreateAccountRequest
import com.petergabriel.budgetcalendar.features.accounts.domain.usecase.CalculateNetWorthUseCase
import com.petergabriel.budgetcalendar.features.accounts.domain.usecase.CreateAccountUseCase
import com.petergabriel.budgetcalendar.features.accounts.domain.usecase.DeleteAccountUseCase
import com.petergabriel.budgetcalendar.features.accounts.domain.usecase.GetAccountsUseCase
import com.petergabriel.budgetcalendar.features.accounts.domain.usecase.UpdateAccountUseCase
import com.petergabriel.budgetcalendar.features.accounts.testutil.FakeAccountRepository
import com.petergabriel.budgetcalendar.features.creditcard.domain.usecase.CreateCreditCardSettingsUseCase
import com.petergabriel.budgetcalendar.features.creditcard.testutil.FakeCreditCardRepository
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var accountRepository: FakeAccountRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        accountRepository = FakeAccountRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun deleteFailure_keepsMutationStateOpenForDetailsSheet() = runTest(dispatcher) {
        val account = accountRepository.createAccount(
            CreateAccountRequest(
                name = "Primary",
                type = AccountType.CHECKING,
                initialBalance = 10_000L,
                isInSpendingPool = true,
            ),
        )
        accountRepository.setHasTransactions(account.id, true)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.deleteAccount(account.id)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isDeleting)
        assertEquals("Cannot delete account with existing transactions", viewModel.uiState.value.mutationError)
        assertEquals(account.id, viewModel.uiState.value.accounts.single().id)
    }

    @Test
    fun saveSuccess_closesFormOnlyAfterUseCaseCompletes() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.showCreateForm()
        assertTrue(viewModel.uiState.value.showForm)

        viewModel.createAccount(
            CreateAccountRequest(
                name = "Primary",
                type = AccountType.CHECKING,
                initialBalance = 10_000L,
                isInSpendingPool = true,
            ),
        )
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showForm)
        assertEquals("Primary", viewModel.uiState.value.accounts.single().name)
    }

    private fun viewModel(): AccountViewModel {
        val creditCardRepository = FakeCreditCardRepository()
        return AccountViewModel(
            createAccountUseCase = CreateAccountUseCase(
                accountRepository,
                CreateCreditCardSettingsUseCase(accountRepository, creditCardRepository),
            ),
            getAccountsUseCase = GetAccountsUseCase(accountRepository),
            updateAccountUseCase = UpdateAccountUseCase(accountRepository),
            deleteAccountUseCase = DeleteAccountUseCase(accountRepository),
            calculateNetWorthUseCase = CalculateNetWorthUseCase(accountRepository),
        )
    }
}
