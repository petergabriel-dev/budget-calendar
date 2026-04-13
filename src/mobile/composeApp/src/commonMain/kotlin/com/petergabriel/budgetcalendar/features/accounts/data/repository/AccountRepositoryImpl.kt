package com.petergabriel.budgetcalendar.features.accounts.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.petergabriel.budgetcalendar.core.database.BudgetCalendarDatabase
import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.accounts.data.local.AccountEntity
import com.petergabriel.budgetcalendar.features.accounts.data.mapper.AccountMapper
import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account
import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType
import com.petergabriel.budgetcalendar.features.accounts.domain.model.CreateAccountRequest
import com.petergabriel.budgetcalendar.features.accounts.domain.model.UpdateAccountRequest
import com.petergabriel.budgetcalendar.features.accounts.domain.repository.IAccountRepository
import com.petergabriel.budgetcalendar.features.transactions.domain.repository.ITransactionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlin.math.abs

@OptIn(ExperimentalCoroutinesApi::class)
class AccountRepositoryImpl(
    private val database: BudgetCalendarDatabase,
    private val accountMapper: AccountMapper,
    private val transactionRepository: ITransactionRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : IAccountRepository {
    private val transactionRefreshTrigger = transactionRepository.transactionChangedTrigger.onStart { emit(Unit) }

    override fun getAllAccounts(): Flow<List<Account>> {
        return transactionRefreshTrigger.flatMapLatest {
            database.accountsQueries
                .getAllAccountsWithComputedBalance(::toEntity)
                .asFlow()
                .mapToList(dispatcher)
                .map { entities -> entities.map(accountMapper::toDomain) }
        }
    }

    override suspend fun getAccountById(id: Long): Account? {
        return database.accountsQueries
            .getAccountById(id, ::toEntity)
            .executeAsOneOrNull()
            ?.let(accountMapper::toDomain)
    }

    override fun getSpendingPoolAccounts(): Flow<List<Account>> {
        return transactionRefreshTrigger.flatMapLatest {
            database.accountsQueries
                .getAllAccountsWithComputedBalance(::toEntity)
                .asFlow()
                .mapToList(dispatcher)
                .map { entities ->
                    entities
                        .map(accountMapper::toDomain)
                        .filter { account -> account.isInSpendingPool }
                        .sortedBy { account -> account.name }
                }
        }
    }

    override suspend fun createAccount(request: CreateAccountRequest): Account {
        val now = DateUtils.nowMillis()
        val description = request.description?.trim()?.takeUnless { it.isEmpty() }
        database.accountsQueries.insertAccount(
            request.name.trim(),
            request.type.dbValue,
            request.initialBalance,
            if (request.isInSpendingPool) 1L else 0L,
            description,
            now,
            now,
        )

        val insertedId = database.accountsQueries.getLastInsertedAccountId().executeAsOne()
        return checkNotNull(getAccountById(insertedId)) {
            "Failed to load newly inserted account"
        }
    }

    override suspend fun updateAccount(id: Long, request: UpdateAccountRequest): Account? {
        val existing = getAccountById(id) ?: return null
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

        database.accountsQueries.updateAccount(
            updated.name,
            updated.type.dbValue,
            if (updated.isInSpendingPool) 1L else 0L,
            updated.description,
            updated.updatedAt,
            id,
        )

        return getAccountById(id)
    }

    override suspend fun deleteAccount(id: Long) {
        database.accountsQueries.deleteAccount(id)
    }

    override suspend fun getTotalSpendingPoolBalance(): Long {
        return database.accountsQueries.getTotalSpendingPoolBalance().executeAsOne()
    }

    override suspend fun hasTransactionsForAccount(accountId: Long): Boolean {
        val count = database.transactionsQueries.countTransactionsByAccount(accountId, accountId).executeAsOne()
        return count > 0
    }

    override suspend fun calculateNetWorth(): Long {
        val accounts = database.accountsQueries.getAllAccountsWithComputedBalance(::toEntity).executeAsList().map(accountMapper::toDomain)
        val assets = accounts.filter { it.type != AccountType.CREDIT_CARD }.sumOf { it.balance }
        val liabilities = accounts.filter { it.type == AccountType.CREDIT_CARD }.sumOf { abs(it.balance) }
        return assets - liabilities
    }

    private fun toEntity(
        id: Long,
        name: String,
        type: String,
        balance: Long,
        isInSpendingPool: Long,
        description: String?,
        createdAt: Long,
        updatedAt: Long,
    ): AccountEntity {
        return AccountEntity(
            id = id,
            name = name,
            type = type,
            balance = balance,
            isInSpendingPool = isInSpendingPool == 1L,
            description = description,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }
}
