package com.petergabriel.budgetcalendar.features.transactions.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.petergabriel.budgetcalendar.core.database.BudgetCalendarDatabase
import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.transactions.data.local.TransactionEntity
import com.petergabriel.budgetcalendar.features.transactions.data.mapper.TransactionMapper
import com.petergabriel.budgetcalendar.features.transactions.domain.model.CreateTransactionRequest
import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import com.petergabriel.budgetcalendar.features.transactions.domain.repository.ITransactionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map

class TransactionRepositoryImpl(
    private val database: BudgetCalendarDatabase,
    private val transactionMapper: TransactionMapper,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ITransactionRepository {

    private val _transactionChangedTrigger = MutableSharedFlow<Unit>(replay = 1)
    override val transactionChangedTrigger: SharedFlow<Unit> = _transactionChangedTrigger.asSharedFlow()

    override fun getTransactionsByAccount(accountId: Long): Flow<List<Transaction>> {
        return database.transactionsQueries
            .getTransactionsByAccount(accountId, ::toEntity)
            .asFlow()
            .mapToList(dispatcher)
            .map { entities -> entities.map(transactionMapper::toDomain) }
    }

    override fun getTransactionsByDateRange(
        startDate: Long,
        endDate: Long,
        typeFilter: TransactionType?,
    ): Flow<List<Transaction>> {
        return database.transactionsQueries
            .getTransactionsByDateRange(startDate, endDate, ::toEntity)
            .asFlow()
            .mapToList(dispatcher)
            .map { entities ->
                val domain = entities.map(transactionMapper::toDomain)
                typeFilter?.let { filterType ->
                    domain.filter { transaction -> transaction.type == filterType }
                } ?: domain
            }
    }

    override fun getTransactionsByDate(date: Long): Flow<List<Transaction>> {
        return database.transactionsQueries
            .getTransactionsByDate(date, ::toEntity)
            .asFlow()
            .mapToList(dispatcher)
            .map { entities -> entities.map(transactionMapper::toDomain) }
    }

    override fun getPendingTransactions(): Flow<List<Transaction>> {
        return database.transactionsQueries
            .getPendingTransactions(::toEntity)
            .asFlow()
            .mapToList(dispatcher)
            .map { entities -> entities.map(transactionMapper::toDomain) }
    }

    override fun getOverdueTransactions(): Flow<List<Transaction>> {
        return database.transactionsQueries
            .getOverdueTransactions(::toEntity)
            .asFlow()
            .mapToList(dispatcher)
            .map { entities -> entities.map(transactionMapper::toDomain) }
    }

    override fun getConfirmedTransactions(): Flow<List<Transaction>> {
        return database.transactionsQueries
            .getConfirmedTransactions(::toEntity)
            .asFlow()
            .mapToList(dispatcher)
            .map { entities -> entities.map(transactionMapper::toDomain) }
    }

    override fun getPendingAndOverdueTransactionsForSpendingPool(): Flow<List<Transaction>> {
        return database.transactionsQueries
            .getPendingTransactionsForSpendingPool(::toEntity)
            .asFlow()
            .mapToList(dispatcher)
            .map { entities -> entities.map(transactionMapper::toDomain) }
    }

    override fun getMonthProjectionTransactions(startMillis: Long, endMillis: Long): Flow<List<Transaction>> {
        return database.transactionsQueries
            .getPendingAndOverdueForSpendingPoolInRange(startMillis, endMillis, ::toEntity)
            .asFlow()
            .mapToList(dispatcher)
            .map { entities -> entities.map(transactionMapper::toDomain) }
    }

    override fun getPendingAndOverdueExpensesByAccount(accountId: Long): Flow<List<Transaction>> {
        return database.transactionsQueries
            .getPendingAndOverdueExpensesByAccount(accountId, ::toEntity)
            .asFlow()
            .mapToList(dispatcher)
            .map { entities -> entities.map(transactionMapper::toDomain) }
    }

    override fun getConfirmedTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return database.transactionsQueries
            .getConfirmedTransactionsByDateRange(startDate, endDate, ::toEntity)
            .asFlow()
            .mapToList(dispatcher)
            .map { entities -> entities.map(transactionMapper::toDomain) }
    }

    override suspend fun getTransactionById(id: Long): Transaction? {
        return database.transactionsQueries
            .getTransactionById(id, ::toEntity)
            .executeAsOneOrNull()
            ?.let(transactionMapper::toDomain)
    }

    override suspend fun createTransaction(request: CreateTransactionRequest): Transaction {
        val now = DateUtils.nowMillis()
        database.transactionsQueries.insertTransaction(
            request.accountId,
            request.destinationAccountId,
            request.amount,
            request.signedAmount,
            request.date,
            request.type.dbValue,
            request.status.dbValue,
            request.description,
            request.category,
            request.linkedTransactionId,
            if (request.isSandbox) 1L else 0L,
            now,
            now,
        )

        val insertedId = database.transactionsQueries.getLastInsertedTransactionId().executeAsOne()
        _transactionChangedTrigger.emit(Unit)
        return checkNotNull(getTransactionById(insertedId)) {
            "Failed to load newly inserted transaction"
        }
    }

    override suspend fun updateTransactionStatus(id: Long, status: TransactionStatus): Transaction? {
        database.transactionsQueries.updateTransactionStatus(
            status.dbValue,
            DateUtils.nowMillis(),
            id,
        )
        _transactionChangedTrigger.emit(Unit)
        return getTransactionById(id)
    }

    override suspend fun updateLinkedTransactionId(id: Long, linkedTransactionId: Long?): Boolean {
        database.transactionsQueries.updateLinkedTransactionId(
            linkedTransactionId,
            DateUtils.nowMillis(),
            id,
        )
        return getTransactionById(id) != null
    }

    override suspend fun deleteTransaction(id: Long) {
        database.transactionsQueries.deleteTransaction(id)
        _transactionChangedTrigger.emit(Unit)
    }

    override suspend fun getTransactionByLinkedId(linkedTransactionId: Long): Transaction? {
        return database.transactionsQueries
            .getTransactionByLinkedId(linkedTransactionId, ::toEntity)
            .executeAsOneOrNull()
            ?.let(transactionMapper::toDomain)
    }

    override suspend fun markOverdueTransactions(nowMillis: Long): Int {
        val pendingTransactions = database.transactionsQueries
            .getPendingTransactions(::toEntity)
            .executeAsList()
            .map(transactionMapper::toDomain)

        val overdue = pendingTransactions.filter { transaction ->
            nowMillis > (transaction.date + DateUtils.MILLIS_IN_DAY)
        }

        overdue.forEach { transaction ->
            database.transactionsQueries.updateTransactionStatus(
                TransactionStatus.OVERDUE.dbValue,
                DateUtils.nowMillis(),
                transaction.id,
            )
        }

        return overdue.size
    }

    private fun toEntity(
        id: Long,
        accountId: Long,
        destinationAccountId: Long?,
        amount: Long,
        date: Long,
        type: String,
        status: String,
        description: String?,
        category: String?,
        linkedTransactionId: Long?,
        isSandbox: Long,
        createdAt: Long,
        updatedAt: Long,
    ): TransactionEntity {
        return TransactionEntity(
            id = id,
            accountId = accountId,
            destinationAccountId = destinationAccountId,
            amount = amount,
            date = date,
            type = type,
            status = status,
            description = description,
            category = category,
            linkedTransactionId = linkedTransactionId,
            isSandbox = isSandbox == 1L,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }
}
