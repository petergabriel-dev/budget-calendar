package com.petergabriel.budgetcalendar.features.accounts.testutil

import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account
import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType
import com.petergabriel.budgetcalendar.features.accounts.domain.model.CreateAccountRequest
import com.petergabriel.budgetcalendar.features.accounts.domain.model.UpdateAccountRequest
import com.petergabriel.budgetcalendar.features.accounts.domain.repository.IAccountRepository
import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlin.math.abs

class FakeAccountRepository(
    private val transactionProvider: () -> List<Transaction> = { emptyList() },
    transactionChangedTrigger: SharedFlow<Unit>? = null,
) : IAccountRepository {
    private val accounts = mutableListOf<Account>()
    private val accountsFlow = MutableStateFlow<List<Account>>(emptyList())
    private val hasTransactionsByAccountId = mutableMapOf<Long, Boolean>()
    private var nextId = 1L
    private val transactionRefreshTrigger = transactionChangedTrigger?.onStart { emit(Unit) }

    override fun getAllAccounts(): Flow<List<Account>> {
        val computedAccountsFlow = accountsFlow.map { allAccounts ->
            allAccounts
                .map(::toComputedBalanceAccount)
                .sortedByDescending { account -> account.createdAt }
        }
        return transactionRefreshTrigger?.let { refresh ->
            combine(computedAccountsFlow, refresh) { computedAccounts, _ -> computedAccounts }
        } ?: computedAccountsFlow
    }

    override suspend fun getAccountById(id: Long): Account? {
        return accounts.firstOrNull { account -> account.id == id }?.let(::toComputedBalanceAccount)
    }

    override fun getSpendingPoolAccounts(): Flow<List<Account>> {
        return getAllAccounts().map { allAccounts ->
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
        return toComputedBalanceAccount(updated)
    }

    override suspend fun deleteAccount(id: Long) {
        accounts.removeAll { account -> account.id == id }
        hasTransactionsByAccountId.remove(id)
        emitAccounts()
    }

    override suspend fun getTotalSpendingPoolBalance(): Long {
        return accounts
            .asSequence()
            .map(::toComputedBalanceAccount)
            .filter { account -> account.isInSpendingPool }
            .sumOf { account -> account.balance }
    }

    override suspend fun hasTransactionsForAccount(accountId: Long): Boolean {
        return hasTransactionsByAccountId[accountId] == true
    }

    override suspend fun calculateNetWorth(): Long {
        val computedAccounts = accounts.map(::toComputedBalanceAccount)
        val assets = computedAccounts
            .filter { account -> account.type != AccountType.CREDIT_CARD }
            .sumOf { account -> account.balance }
        val liabilities = computedAccounts
            .filter { account -> account.type == AccountType.CREDIT_CARD }
            .sumOf { account -> abs(account.balance) }
        return assets - liabilities
    }

    suspend fun seedAccounts(vararg seededAccounts: Account) {
        accounts.clear()
        accounts.addAll(seededAccounts)
        nextId = ((seededAccounts.maxOfOrNull { account -> account.id } ?: 0L) + 1L)
        emitAccounts()
    }

    fun setHasTransactions(accountId: Long, hasTransactions: Boolean) {
        hasTransactionsByAccountId[accountId] = hasTransactions
    }

    fun currentAccounts(): List<Account> = accounts.map(::toComputedBalanceAccount)

    /**
     * Returns the current balance of an account.
     */
    fun getBalance(accountId: Long): Long {
        return accounts.firstOrNull { account -> account.id == accountId }
            ?.let(::toComputedBalanceAccount)
            ?.balance
            ?: 0L
    }

    private fun emitAccounts() {
        accountsFlow.value = accounts.toList()
    }

    private fun toComputedBalanceAccount(account: Account): Account {
        val confirmedSignedAmount = transactionProvider()
            .asSequence()
            .filter { transaction ->
                transaction.accountId == account.id &&
                    transaction.status == TransactionStatus.CONFIRMED &&
                    !transaction.isSandbox
            }
            .sumOf(::signedAmountFor)

        return account.copy(balance = account.balance + confirmedSignedAmount)
    }

    private fun signedAmountFor(transaction: Transaction): Long {
        return when (transaction.type) {
            TransactionType.INCOME -> transaction.amount
            TransactionType.EXPENSE -> -transaction.amount
            TransactionType.TRANSFER -> {
                val linkedTransactionId = transaction.linkedTransactionId
                if (linkedTransactionId == null || transaction.id < linkedTransactionId) {
                    -transaction.amount
                } else {
                    transaction.amount
                }
            }
        }
    }
}
