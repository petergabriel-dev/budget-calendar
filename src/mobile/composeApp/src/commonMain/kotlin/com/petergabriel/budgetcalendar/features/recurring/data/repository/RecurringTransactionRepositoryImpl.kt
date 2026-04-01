package com.petergabriel.budgetcalendar.features.recurring.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.petergabriel.budgetcalendar.core.database.BudgetCalendarDatabase
import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.recurring.data.local.RecurringTransactionEntity
import com.petergabriel.budgetcalendar.features.recurring.data.mapper.RecurringTransactionMapper
import com.petergabriel.budgetcalendar.features.recurring.domain.model.CreateRecurringTransactionRequest
import com.petergabriel.budgetcalendar.features.recurring.domain.model.RecurringTransaction
import com.petergabriel.budgetcalendar.features.recurring.domain.model.RecurrenceType
import com.petergabriel.budgetcalendar.features.recurring.domain.model.UpdateRecurringTransactionRequest
import com.petergabriel.budgetcalendar.features.recurring.domain.repository.IRecurringTransactionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RecurringTransactionRepositoryImpl(
    private val database: BudgetCalendarDatabase,
    private val recurringMapper: RecurringTransactionMapper,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : IRecurringTransactionRepository {

    override fun getAll(): Flow<List<RecurringTransaction>> {
        return database.recurring_transactionsQueries
            .getAllRecurringTransactions(::toEntity)
            .asFlow()
            .mapToList(dispatcher)
            .map { entities -> entities.map(recurringMapper::toDomain) }
    }

    override fun getActive(): Flow<List<RecurringTransaction>> {
        return database.recurring_transactionsQueries
            .getActiveRecurringTransactions(::toEntity)
            .asFlow()
            .mapToList(dispatcher)
            .map { entities -> entities.map(recurringMapper::toDomain) }
    }

    override suspend fun getById(id: Long): RecurringTransaction? {
        return database.recurring_transactionsQueries
            .getRecurringTransactionById(id, ::toEntity)
            .executeAsOneOrNull()
            ?.let(recurringMapper::toDomain)
    }

    override fun getByAccount(accountId: Long): Flow<List<RecurringTransaction>> {
        return database.recurring_transactionsQueries
            .getRecurringTransactionsByAccount(accountId, ::toEntity)
            .asFlow()
            .mapToList(dispatcher)
            .map { entities -> entities.map(recurringMapper::toDomain) }
    }

    override suspend fun insert(request: CreateRecurringTransactionRequest): RecurringTransaction {
        val now = DateUtils.nowMillis()
        database.recurring_transactionsQueries.insertRecurringTransaction(
            request.accountId,
            request.destinationAccountId,
            request.amount,
            request.dayOfMonth.toLong(),
            request.type.dbValue,
            request.description,
            if (request.isActive) 1L else 0L,
            now,
            now,
        )

        val insertedId = database.recurring_transactionsQueries.getLastInsertedRecurringTransactionId().executeAsOne()
        return checkNotNull(getById(insertedId)) {
            "Failed to load newly inserted recurring transaction"
        }
    }

    override suspend fun update(id: Long, request: UpdateRecurringTransactionRequest): RecurringTransaction? {
        val existing = getById(id) ?: return null

        val resolvedType = request.type ?: existing.type
        val resolvedAccountId = request.accountId ?: existing.accountId
        val resolvedAmount = request.amount ?: existing.amount
        val resolvedDayOfMonth = request.dayOfMonth ?: existing.dayOfMonth
        val resolvedDestinationAccountId = if (resolvedType == RecurrenceType.TRANSFER) {
            request.destinationAccountId ?: existing.destinationAccountId
        } else {
            null
        }
        val resolvedDescription = request.description ?: existing.description
        val resolvedIsActive = request.isActive ?: existing.isActive

        database.recurring_transactionsQueries.updateRecurringTransaction(
            resolvedAccountId,
            resolvedDestinationAccountId,
            resolvedAmount,
            resolvedDayOfMonth.toLong(),
            resolvedType.dbValue,
            resolvedDescription,
            if (resolvedIsActive) 1L else 0L,
            DateUtils.nowMillis(),
            id,
        )

        return getById(id)
    }

    override suspend fun toggleActive(id: Long, isActive: Boolean): RecurringTransaction? {
        database.recurring_transactionsQueries.toggleRecurringActive(
            if (isActive) 1L else 0L,
            DateUtils.nowMillis(),
            id,
        )
        return getById(id)
    }

    override suspend fun delete(id: Long) {
        database.recurring_transactionsQueries.deleteRecurringTransaction(id)
    }

    private fun toEntity(
        id: Long,
        accountId: Long,
        destinationAccountId: Long?,
        amount: Long,
        dayOfMonth: Long,
        type: String,
        description: String?,
        isActive: Long,
        createdAt: Long,
        updatedAt: Long,
    ): RecurringTransactionEntity {
        return RecurringTransactionEntity(
            id = id,
            accountId = accountId,
            destinationAccountId = destinationAccountId,
            amount = amount,
            dayOfMonth = dayOfMonth.toInt(),
            type = type,
            description = description,
            isActive = isActive == 1L,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }
}
