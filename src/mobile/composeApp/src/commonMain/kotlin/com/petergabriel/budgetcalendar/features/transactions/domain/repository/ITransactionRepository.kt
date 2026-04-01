package com.petergabriel.budgetcalendar.features.transactions.domain.repository

import com.petergabriel.budgetcalendar.features.transactions.domain.model.CreateTransactionRequest
import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface ITransactionRepository {
    val transactionChangedTrigger: SharedFlow<Unit>

    fun getTransactionsByAccount(accountId: Long): Flow<List<Transaction>>
    fun getTransactionsByDateRange(startDate: Long, endDate: Long, typeFilter: TransactionType? = null): Flow<List<Transaction>>
    fun getTransactionsByDate(date: Long): Flow<List<Transaction>>
    fun getPendingTransactions(): Flow<List<Transaction>>
    fun getOverdueTransactions(): Flow<List<Transaction>>
    fun getConfirmedTransactions(): Flow<List<Transaction>>
    fun getPendingAndOverdueTransactionsForSpendingPool(): Flow<List<Transaction>>
    fun getMonthProjectionTransactions(startMillis: Long, endMillis: Long): Flow<List<Transaction>>
    fun getPendingAndOverdueExpensesByAccount(accountId: Long): Flow<List<Transaction>>
    fun getConfirmedTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>

    suspend fun getTransactionById(id: Long): Transaction?
    suspend fun createTransaction(request: CreateTransactionRequest): Transaction
    suspend fun updateTransactionStatus(id: Long, status: TransactionStatus): Transaction?
    suspend fun updateLinkedTransactionId(id: Long, linkedTransactionId: Long?): Boolean
    suspend fun deleteTransaction(id: Long)
    suspend fun getTransactionByLinkedId(linkedTransactionId: Long): Transaction?
    suspend fun markOverdueTransactions(nowMillis: Long): Int
}
