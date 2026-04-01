package com.petergabriel.budgetcalendar.features.recurring.testutil

import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.recurring.domain.model.CreateRecurringTransactionRequest
import com.petergabriel.budgetcalendar.features.recurring.domain.model.RecurringTransaction
import com.petergabriel.budgetcalendar.features.recurring.domain.model.RecurrenceType
import com.petergabriel.budgetcalendar.features.recurring.domain.model.UpdateRecurringTransactionRequest
import com.petergabriel.budgetcalendar.features.recurring.domain.repository.IRecurringTransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeRecurringTransactionRepository : IRecurringTransactionRepository {
    private val recurringTransactions = mutableListOf<RecurringTransaction>()
    private val recurringFlow = MutableStateFlow<List<RecurringTransaction>>(emptyList())
    private var nextId = 1L

    override fun getAll(): Flow<List<RecurringTransaction>> {
        return recurringFlow.map { items ->
            items.sortedWith(
                compareBy<RecurringTransaction> { recurring -> recurring.dayOfMonth }
                    .thenBy { recurring -> recurring.createdAt },
            )
        }
    }

    override fun getActive(): Flow<List<RecurringTransaction>> {
        return recurringFlow.map { items ->
            items
                .filter { recurring -> recurring.isActive }
                .sortedWith(
                    compareBy<RecurringTransaction> { recurring -> recurring.dayOfMonth }
                        .thenBy { recurring -> recurring.createdAt },
                )
        }
    }

    override suspend fun getById(id: Long): RecurringTransaction? {
        return recurringTransactions.firstOrNull { recurring -> recurring.id == id }
    }

    override fun getByAccount(accountId: Long): Flow<List<RecurringTransaction>> {
        return recurringFlow.map { items ->
            items
                .filter { recurring -> recurring.accountId == accountId }
                .sortedWith(
                    compareBy<RecurringTransaction> { recurring -> recurring.dayOfMonth }
                        .thenBy { recurring -> recurring.createdAt },
                )
        }
    }

    override suspend fun insert(request: CreateRecurringTransactionRequest): RecurringTransaction {
        val now = DateUtils.nowMillis()
        val recurring = RecurringTransaction(
            id = nextId++,
            accountId = request.accountId,
            destinationAccountId = request.destinationAccountId,
            amount = request.amount,
            dayOfMonth = request.dayOfMonth,
            type = request.type,
            description = request.description,
            isActive = request.isActive,
            createdAt = now,
            updatedAt = now,
        )

        recurringTransactions += recurring
        emitAll()
        return recurring
    }

    override suspend fun update(id: Long, request: UpdateRecurringTransactionRequest): RecurringTransaction? {
        val index = recurringTransactions.indexOfFirst { recurring -> recurring.id == id }
        if (index < 0) {
            return null
        }

        val existing = recurringTransactions[index]
        val resolvedType = request.type ?: existing.type
        val updated = existing.copy(
            accountId = request.accountId ?: existing.accountId,
            destinationAccountId = if (resolvedType == RecurrenceType.TRANSFER) {
                request.destinationAccountId ?: existing.destinationAccountId
            } else {
                null
            },
            amount = request.amount ?: existing.amount,
            dayOfMonth = request.dayOfMonth ?: existing.dayOfMonth,
            type = resolvedType,
            description = request.description ?: existing.description,
            isActive = request.isActive ?: existing.isActive,
            updatedAt = DateUtils.nowMillis(),
        )

        recurringTransactions[index] = updated
        emitAll()
        return updated
    }

    override suspend fun toggleActive(id: Long, isActive: Boolean): RecurringTransaction? {
        val index = recurringTransactions.indexOfFirst { recurring -> recurring.id == id }
        if (index < 0) {
            return null
        }

        val updated = recurringTransactions[index].copy(
            isActive = isActive,
            updatedAt = DateUtils.nowMillis(),
        )
        recurringTransactions[index] = updated
        emitAll()
        return updated
    }

    override suspend fun delete(id: Long) {
        recurringTransactions.removeAll { recurring -> recurring.id == id }
        emitAll()
    }

    fun seedRecurring(vararg seededRecurring: RecurringTransaction) {
        recurringTransactions.clear()
        recurringTransactions.addAll(seededRecurring)
        nextId = (seededRecurring.maxOfOrNull { recurring -> recurring.id } ?: 0L) + 1L
        emitAll()
    }

    fun allRecurring(): List<RecurringTransaction> = recurringTransactions.toList()

    private fun emitAll() {
        recurringFlow.value = recurringTransactions.toList()
    }
}
