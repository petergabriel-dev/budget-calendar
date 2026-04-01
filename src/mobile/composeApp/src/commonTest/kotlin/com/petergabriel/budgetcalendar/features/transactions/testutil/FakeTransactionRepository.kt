package com.petergabriel.budgetcalendar.features.transactions.testutil

import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.transactions.domain.model.CreateTransactionRequest
import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import com.petergabriel.budgetcalendar.features.transactions.domain.repository.ITransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeTransactionRepository : ITransactionRepository {
    private val transactions = mutableListOf<Transaction>()
    private val transactionsFlow = MutableStateFlow<List<Transaction>>(emptyList())
    private var nextId = 1L

    override fun getTransactionsByAccount(accountId: Long): Flow<List<Transaction>> {
        return transactionsFlow.map { allTransactions ->
            allTransactions
                .filter { transaction -> transaction.accountId == accountId }
                .sortedWith(compareByDescending<Transaction> { transaction -> transaction.date }
                    .thenByDescending { transaction -> transaction.createdAt })
        }
    }

    override fun getTransactionsByDateRange(
        startDate: Long,
        endDate: Long,
        typeFilter: TransactionType?,
    ): Flow<List<Transaction>> {
        return transactionsFlow.map { allTransactions ->
            val filtered = allTransactions
                .filter { transaction -> transaction.date in startDate..endDate }
                .sortedWith(compareBy<Transaction> { transaction -> transaction.date }
                    .thenByDescending { transaction -> transaction.createdAt })
            typeFilter?.let { filterType ->
                filtered.filter { transaction -> transaction.type == filterType }
            } ?: filtered
        }
    }

    override fun getTransactionsByDate(date: Long): Flow<List<Transaction>> {
        return transactionsFlow.map { allTransactions ->
            allTransactions
                .filter { transaction -> transaction.date == date }
                .sortedByDescending { transaction -> transaction.createdAt }
        }
    }

    override fun getPendingTransactions(): Flow<List<Transaction>> {
        return transactionsFlow.map { allTransactions ->
            allTransactions
                .filter { transaction -> transaction.status == TransactionStatus.PENDING && !transaction.isSandbox }
                .sortedBy { transaction -> transaction.date }
        }
    }

    override fun getOverdueTransactions(): Flow<List<Transaction>> {
        return transactionsFlow.map { allTransactions ->
            allTransactions
                .filter { transaction -> transaction.status == TransactionStatus.OVERDUE && !transaction.isSandbox }
                .sortedBy { transaction -> transaction.date }
        }
    }

    override fun getConfirmedTransactions(): Flow<List<Transaction>> {
        return transactionsFlow.map { allTransactions ->
            allTransactions
                .filter { transaction -> transaction.status == TransactionStatus.CONFIRMED && !transaction.isSandbox }
                .sortedByDescending { transaction -> transaction.date }
        }
    }

    override fun getPendingAndOverdueTransactionsForSpendingPool(): Flow<List<Transaction>> {
        return transactionsFlow.map { allTransactions ->
            allTransactions
                .filter { transaction ->
                    !transaction.isSandbox &&
                        (transaction.status == TransactionStatus.PENDING || transaction.status == TransactionStatus.OVERDUE)
                }
                .sortedBy { transaction -> transaction.date }
        }
    }

    override fun getMonthProjectionTransactions(startMillis: Long, endMillis: Long): Flow<List<Transaction>> {
        return transactionsFlow.map { allTransactions ->
            allTransactions
                .filter { transaction ->
                    !transaction.isSandbox &&
                        transaction.date in startMillis..endMillis &&
                        (transaction.status == TransactionStatus.PENDING || transaction.status == TransactionStatus.OVERDUE)
                }
                .sortedBy { transaction -> transaction.date }
        }
    }

    override fun getPendingAndOverdueExpensesByAccount(accountId: Long): Flow<List<Transaction>> {
        return transactionsFlow.map { allTransactions ->
            allTransactions
                .filter { transaction ->
                    transaction.accountId == accountId &&
                        transaction.type == TransactionType.EXPENSE &&
                        !transaction.isSandbox &&
                        (transaction.status == TransactionStatus.PENDING || transaction.status == TransactionStatus.OVERDUE)
                }
                .sortedBy { transaction -> transaction.date }
        }
    }

    override fun getConfirmedTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return transactionsFlow.map { allTransactions ->
            allTransactions
                .filter { transaction ->
                    !transaction.isSandbox &&
                        transaction.status == TransactionStatus.CONFIRMED &&
                        transaction.date in startDate..endDate
                }
                .sortedBy { transaction -> transaction.date }
        }
    }

    override suspend fun getTransactionById(id: Long): Transaction? {
        return transactions.firstOrNull { transaction -> transaction.id == id }
    }

    override suspend fun createTransaction(request: CreateTransactionRequest): Transaction {
        val now = DateUtils.nowMillis()
        val transaction = Transaction(
            id = nextId++,
            accountId = request.accountId,
            destinationAccountId = request.destinationAccountId,
            amount = request.amount,
            date = request.date,
            type = request.type,
            status = request.status,
            description = request.description,
            category = request.category,
            linkedTransactionId = request.linkedTransactionId,
            isSandbox = request.isSandbox,
            createdAt = now,
            updatedAt = now,
        )

        transactions += transaction
        emitTransactions()
        return transaction
    }

    override suspend fun updateTransactionStatus(id: Long, status: TransactionStatus): Transaction? {
        val index = transactions.indexOfFirst { transaction -> transaction.id == id }
        if (index < 0) {
            return null
        }

        val updated = transactions[index].copy(
            status = status,
            updatedAt = DateUtils.nowMillis(),
        )
        transactions[index] = updated
        emitTransactions()
        return updated
    }

    override suspend fun updateLinkedTransactionId(id: Long, linkedTransactionId: Long?): Boolean {
        val index = transactions.indexOfFirst { transaction -> transaction.id == id }
        if (index < 0) {
            return false
        }

        transactions[index] = transactions[index].copy(
            linkedTransactionId = linkedTransactionId,
            updatedAt = DateUtils.nowMillis(),
        )
        emitTransactions()
        return true
    }

    override suspend fun deleteTransaction(id: Long) {
        transactions.removeAll { transaction -> transaction.id == id }
        emitTransactions()
    }

    override suspend fun getTransactionByLinkedId(linkedTransactionId: Long): Transaction? {
        return transactions.firstOrNull { transaction -> transaction.linkedTransactionId == linkedTransactionId }
    }

    override suspend fun markOverdueTransactions(nowMillis: Long): Int {
        var overdueCount = 0

        for (index in transactions.indices) {
            val transaction = transactions[index]
            if (transaction.status == TransactionStatus.PENDING && nowMillis > transaction.date + DateUtils.MILLIS_IN_DAY) {
                transactions[index] = transaction.copy(
                    status = TransactionStatus.OVERDUE,
                    updatedAt = DateUtils.nowMillis(),
                )
                overdueCount += 1
            }
        }

        if (overdueCount > 0) {
            emitTransactions()
        }

        return overdueCount
    }

    fun seedTransactions(vararg seededTransactions: Transaction) {
        transactions.clear()
        transactions.addAll(seededTransactions)
        nextId = ((seededTransactions.maxOfOrNull { transaction -> transaction.id } ?: 0L) + 1L)
        emitTransactions()
    }

    fun allTransactions(): List<Transaction> = transactions.toList()

    private fun emitTransactions() {
        transactionsFlow.value = transactions.toList()
    }
}
