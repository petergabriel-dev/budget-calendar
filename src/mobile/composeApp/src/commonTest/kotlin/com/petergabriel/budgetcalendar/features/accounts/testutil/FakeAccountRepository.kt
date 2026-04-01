package com.petergabriel.budgetcalendar.features.accounts.testutil

import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account
import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType
import com.petergabriel.budgetcalendar.features.accounts.domain.model.CreateAccountRequest
import com.petergabriel.budgetcalendar.features.accounts.domain.model.UpdateAccountRequest
import com.petergabriel.budgetcalendar.features.accounts.domain.repository.IAccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlin.math.abs

class FakeAccountRepository : IAccountRepository {
    private val accounts = mutableListOf<Account>()
    private val accountsFlow = MutableStateFlow<List<Account>>(emptyList())
    private val hasTransactionsByAccountId = mutableMapOf<Long, Boolean>()
    private var nextId = 1L
    private val _balanceChangedTrigger = MutableSharedFlow<Unit>(replay = 1)
    override val balanceChangedTrigger: SharedFlow<Unit> = _balanceChangedTrigger.asSharedFlow()

    override fun getAllAccounts(): Flow<List<Account>> = accountsFlow

    override suspend fun getAccountById(id: Long): Account? {
        return accounts.firstOrNull { account -> account.id == id }
    }

    override fun getSpendingPoolAccounts(): Flow<List<Account>> {
        return accountsFlow.map { allAccounts ->
            allAccounts.filter { account -> account.isInSpendingPool }
                .sortedBy { account -> account.name }
        }
    }

    override suspend fun createAccount(request: CreateAccountRequest): Account {
        val now = DateUtils.nowMillis()
        val account = Account(
            id = nextId++,
            name = request.name.trim(),
            type = request.type,
            balance = request.initialBalance,
            isInSpendingPool = request.isInSpendingPool,
            createdAt = now,
            updatedAt = now,
            description = request.description?.trim()?.takeUnless { value -> value.isEmpty() },
        )
        accounts += account
        emitAccounts()
        _balanceChangedTrigger.emit(Unit)
        return account
    }

    override suspend fun updateAccount(id: Long, request: UpdateAccountRequest): Account? {
        val index = accounts.indexOfFirst { account -> account.id == id }
        if (index < 0) {
            return null
        }

        val existing = accounts[index]
        val updated = existing.copy(
            name = request.name?.trim().takeUnless { it.isNullOrBlank() } ?: existing.name,
            type = request.type ?: existing.type,
            balance = request.balance ?: existing.balance,
            isInSpendingPool = request.isInSpendingPool ?: existing.isInSpendingPool,
            description = if (request.description == null) {
                existing.description
            } else {
                request.description.trim().ifEmpty { null }
            },
            updatedAt = DateUtils.nowMillis(),
        )

        accounts[index] = updated
        emitAccounts()
        _balanceChangedTrigger.emit(Unit)
        return updated
    }

    override suspend fun deleteAccount(id: Long) {
        accounts.removeAll { account -> account.id == id }
        hasTransactionsByAccountId.remove(id)
        emitAccounts()
        _balanceChangedTrigger.emit(Unit)
    }

    override suspend fun getTotalSpendingPoolBalance(): Long {
        return accounts.filter { account -> account.isInSpendingPool }
            .sumOf { account -> account.balance }
    }

    override suspend fun hasTransactionsForAccount(accountId: Long): Boolean {
        return hasTransactionsByAccountId[accountId] == true
    }

    override suspend fun calculateNetWorth(): Long {
        val assets = accounts
            .filter { account -> account.type != AccountType.CREDIT_CARD }
            .sumOf { account -> account.balance }
        val liabilities = accounts
            .filter { account -> account.type == AccountType.CREDIT_CARD }
            .sumOf { account -> abs(account.balance) }
        return assets - liabilities
    }

    override suspend fun adjustBalance(accountId: Long, delta: Long) {
        adjustBalanceInternal(accountId, delta)
        _balanceChangedTrigger.emit(Unit)
    }

    suspend fun seedAccounts(vararg seededAccounts: Account) {
        accounts.clear()
        accounts.addAll(seededAccounts)
        nextId = ((seededAccounts.maxOfOrNull { account -> account.id } ?: 0L) + 1L)
        emitAccounts()
        _balanceChangedTrigger.emit(Unit)
    }

    fun setHasTransactions(accountId: Long, hasTransactions: Boolean) {
        hasTransactionsByAccountId[accountId] = hasTransactions
    }

    fun currentAccounts(): List<Account> = accounts.toList()

    /**
     * Adjusts the balance of an account by the given delta.
     * This is used for testing to simulate balance changes from transaction operations.
     */
    private fun adjustBalanceInternal(accountId: Long, delta: Long) {
        val index = accounts.indexOfFirst { account -> account.id == accountId }
        if (index >= 0) {
            val account = accounts[index]
            accounts[index] = account.copy(
                balance = account.balance + delta,
                updatedAt = DateUtils.nowMillis(),
            )
            emitAccounts()
        }
    }

    /**
     * Returns the current balance of an account.
     */
    fun getBalance(accountId: Long): Long {
        return accounts.firstOrNull { account -> account.id == accountId }?.balance ?: 0L
    }

    private fun emitAccounts() {
        accountsFlow.value = accounts
            .sortedByDescending { account -> account.createdAt }
    }
}
