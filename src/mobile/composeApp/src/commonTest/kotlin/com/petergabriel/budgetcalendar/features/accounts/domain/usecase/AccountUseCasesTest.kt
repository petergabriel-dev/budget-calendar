package com.petergabriel.budgetcalendar.features.accounts.domain.usecase

import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType
import com.petergabriel.budgetcalendar.features.accounts.domain.model.CreateAccountRequest
import com.petergabriel.budgetcalendar.features.accounts.domain.model.UpdateAccountRequest
import com.petergabriel.budgetcalendar.features.accounts.testutil.FakeAccountRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountUseCasesTest {
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var createAccountUseCase: CreateAccountUseCase
    private lateinit var updateAccountUseCase: UpdateAccountUseCase
    private lateinit var deleteAccountUseCase: DeleteAccountUseCase
    private lateinit var getSpendingPoolAccountsUseCase: GetSpendingPoolAccountsUseCase
    private lateinit var calculateNetWorthUseCase: CalculateNetWorthUseCase

    @BeforeTest
    fun setUp() {
        accountRepository = FakeAccountRepository()
        createAccountUseCase = CreateAccountUseCase(accountRepository)
        updateAccountUseCase = UpdateAccountUseCase(accountRepository)
        deleteAccountUseCase = DeleteAccountUseCase(accountRepository)
        getSpendingPoolAccountsUseCase = GetSpendingPoolAccountsUseCase(accountRepository)
        calculateNetWorthUseCase = CalculateNetWorthUseCase(accountRepository)
    }

    @Test
    fun createAccount_createsValidCheckingAccountWithExpectedFields() = runBlocking {
        val result = createAccountUseCase(
            CreateAccountRequest(
                name = "  Primary Checking  ",
                type = AccountType.CHECKING,
                initialBalance = 150_00L,
                isInSpendingPool = true,
            ),
        )

        assertTrue(result.isSuccess)
        val account = result.getOrThrow()

        assertEquals("Primary Checking", account.name)
        assertEquals(AccountType.CHECKING, account.type)
        assertEquals(150_00L, account.balance)
        assertTrue(account.isInSpendingPool)
        assertEquals(account, accountRepository.getAccountById(account.id))
    }

    @Test
    fun createAccount_returnsErrorWhenNameIsEmpty() = runBlocking {
        val result = createAccountUseCase(
            CreateAccountRequest(
                name = "   ",
                type = AccountType.CHECKING,
                initialBalance = 100_00L,
                isInSpendingPool = true,
            ),
        )

        assertTrue(result.isFailure)
        assertEquals("Account name must be 1-50 characters", result.exceptionOrNull()?.message)
    }

    @Test
    fun createAccount_returnsErrorWhenNameExceeds50Characters() = runBlocking {
        val result = createAccountUseCase(
            CreateAccountRequest(
                name = "a".repeat(51),
                type = AccountType.CHECKING,
                initialBalance = 100_00L,
                isInSpendingPool = true,
            ),
        )

        assertTrue(result.isFailure)
        assertEquals("Account name must be 1-50 characters", result.exceptionOrNull()?.message)
    }

    @Test
    fun createAccount_rejectsNegativeBalanceForNonCreditCard() = runBlocking {
        val result = createAccountUseCase(
            CreateAccountRequest(
                name = "Savings",
                type = AccountType.SAVINGS,
                initialBalance = -1L,
                isInSpendingPool = false,
            ),
        )

        assertTrue(result.isFailure)
        assertEquals(
            "Asset accounts cannot have negative initial balance",
            result.exceptionOrNull()?.message,
        )
    }

    @Test
    fun createAccount_allowsNegativeBalanceForCreditCard() = runBlocking {
        val result = createAccountUseCase(
            CreateAccountRequest(
                name = "Card",
                type = AccountType.CREDIT_CARD,
                initialBalance = -10_00L,
                isInSpendingPool = false,
            ),
        )

        assertTrue(result.isSuccess)
        assertEquals(-10_00L, result.getOrThrow().balance)
    }

    @Test
    fun createAccount_creditCardIsAlwaysExcludedFromSpendingPool() = runBlocking {
        val result = createAccountUseCase(
            CreateAccountRequest(
                name = "Card",
                type = AccountType.CREDIT_CARD,
                initialBalance = -1_000L,
                isInSpendingPool = true,
            ),
        )

        assertTrue(result.isSuccess)
        assertEquals(false, result.getOrThrow().isInSpendingPool)
    }

    @Test
    fun updateAccount_persistsValidNameUpdate() = runBlocking {
        val account = accountRepository.createAccount(
            CreateAccountRequest(
                name = "Old Name",
                type = AccountType.CHECKING,
                initialBalance = 1000L,
                isInSpendingPool = true,
            ),
        )

        val result = updateAccountUseCase(
            id = account.id,
            request = UpdateAccountRequest(name = "  Updated Name  "),
        )

        assertTrue(result.isSuccess)
        assertEquals("Updated Name", result.getOrThrow().name)
        assertEquals("Updated Name", accountRepository.getAccountById(account.id)?.name)
    }

    @Test
    fun updateAccount_returnsErrorForNonExistentId() = runBlocking {
        val result = updateAccountUseCase(
            id = 999L,
            request = UpdateAccountRequest(name = "Renamed"),
        )

        assertTrue(result.isFailure)
        assertEquals("Account with id=999 was not found", result.exceptionOrNull()?.message)
    }

    @Test
    fun updateAccount_creditCardIsAlwaysExcludedFromSpendingPool() = runBlocking {
        val account = accountRepository.createAccount(
            CreateAccountRequest(
                name = "Checking",
                type = AccountType.CHECKING,
                initialBalance = 5_000L,
                isInSpendingPool = true,
            ),
        )

        val result = updateAccountUseCase(
            id = account.id,
            request = UpdateAccountRequest(
                type = AccountType.CREDIT_CARD,
                isInSpendingPool = true,
            ),
        )

        assertTrue(result.isSuccess)
        assertEquals(false, result.getOrThrow().isInSpendingPool)
    }

    @Test
    fun deleteAccount_deletesAccountWithoutTransactions() = runBlocking {
        val account = accountRepository.createAccount(
            CreateAccountRequest(
                name = "Delete Me",
                type = AccountType.CHECKING,
                initialBalance = 0L,
                isInSpendingPool = false,
            ),
        )

        val result = deleteAccountUseCase(account.id)

        assertTrue(result.isSuccess)
        assertNull(accountRepository.getAccountById(account.id))
    }

    @Test
    fun deleteAccount_returnsErrorWhenAccountHasTransactions() = runBlocking {
        val account = accountRepository.createAccount(
            CreateAccountRequest(
                name = "Has Tx",
                type = AccountType.CHECKING,
                initialBalance = 0L,
                isInSpendingPool = false,
            ),
        )
        accountRepository.setHasTransactions(account.id, hasTransactions = true)

        val result = deleteAccountUseCase(account.id)

        assertTrue(result.isFailure)
        assertEquals(
            "Cannot delete account with existing transactions",
            result.exceptionOrNull()?.message,
        )
    }

    @Test
    fun getSpendingPoolAccounts_returnsOnlyPoolAccounts() = runBlocking {
        accountRepository.createAccount(
            CreateAccountRequest("Pool A", AccountType.CHECKING, 1000L, isInSpendingPool = true),
        )
        accountRepository.createAccount(
            CreateAccountRequest("Not Pool", AccountType.SAVINGS, 2000L, isInSpendingPool = false),
        )
        accountRepository.createAccount(
            CreateAccountRequest("Pool B", AccountType.CASH, 3000L, isInSpendingPool = true),
        )

        val poolAccounts = getSpendingPoolAccountsUseCase().first()

        assertEquals(listOf("Pool A", "Pool B"), poolAccounts.map { account -> account.name })
    }

    @Test
    fun getSpendingPoolAccounts_returnsEmptyWhenPoolIsEmpty() = runBlocking {
        accountRepository.createAccount(
            CreateAccountRequest("Not Pool", AccountType.CHECKING, 1000L, isInSpendingPool = false),
        )

        val poolAccounts = getSpendingPoolAccountsUseCase().first()

        assertTrue(poolAccounts.isEmpty())
    }

    @Test
    fun calculateNetWorth_subtractsCreditCardLiabilitiesFromAssets() = runBlocking {
        accountRepository.createAccount(
            CreateAccountRequest("Checking", AccountType.CHECKING, 20_000L, isInSpendingPool = true),
        )
        accountRepository.createAccount(
            CreateAccountRequest("Savings", AccountType.SAVINGS, 5_000L, isInSpendingPool = false),
        )
        accountRepository.createAccount(
            CreateAccountRequest("Card", AccountType.CREDIT_CARD, -4_000L, isInSpendingPool = false),
        )

        val result = calculateNetWorthUseCase()

        assertTrue(result.isSuccess)
        assertEquals(21_000L, result.getOrThrow())
    }

    @Test
    fun calculateNetWorth_returnsPositiveWhenAllAccountsAreAssets() = runBlocking {
        accountRepository.createAccount(
            CreateAccountRequest("Checking", AccountType.CHECKING, 10_000L, isInSpendingPool = true),
        )
        accountRepository.createAccount(
            CreateAccountRequest("Savings", AccountType.SAVINGS, 8_000L, isInSpendingPool = false),
        )

        val result = calculateNetWorthUseCase()

        assertTrue(result.isSuccess)
        assertEquals(18_000L, result.getOrThrow())
    }

    @Test
    fun calculateNetWorth_returnsNegativeWhenAllAccountsAreLiabilities() = runBlocking {
        accountRepository.createAccount(
            CreateAccountRequest("Card A", AccountType.CREDIT_CARD, -7_500L, isInSpendingPool = false),
        )
        accountRepository.createAccount(
            CreateAccountRequest("Card B", AccountType.CREDIT_CARD, -2_500L, isInSpendingPool = false),
        )

        val result = calculateNetWorthUseCase()

        assertTrue(result.isSuccess)
        assertEquals(-10_000L, result.getOrThrow())
    }

    @Test
    fun calculateNetWorth_returnsZeroWhenNoAccountsExist() = runBlocking {
        val result = calculateNetWorthUseCase()

        assertTrue(result.isSuccess)
        assertEquals(0L, result.getOrThrow())
    }
}
