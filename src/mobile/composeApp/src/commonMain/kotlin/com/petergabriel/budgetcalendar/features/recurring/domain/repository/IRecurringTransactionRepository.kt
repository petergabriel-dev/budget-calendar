package com.petergabriel.budgetcalendar.features.recurring.domain.repository

import com.petergabriel.budgetcalendar.features.recurring.domain.model.CreateRecurringTransactionRequest
import com.petergabriel.budgetcalendar.features.recurring.domain.model.RecurringTransaction
import com.petergabriel.budgetcalendar.features.recurring.domain.model.UpdateRecurringTransactionRequest
import kotlinx.coroutines.flow.Flow

interface IRecurringTransactionRepository {
    fun getAll(): Flow<List<RecurringTransaction>>
    fun getActive(): Flow<List<RecurringTransaction>>
    suspend fun getById(id: Long): RecurringTransaction?
    fun getByAccount(accountId: Long): Flow<List<RecurringTransaction>>
    suspend fun insert(request: CreateRecurringTransactionRequest): RecurringTransaction
    suspend fun update(id: Long, request: UpdateRecurringTransactionRequest): RecurringTransaction?
    suspend fun toggleActive(id: Long, isActive: Boolean): RecurringTransaction?
    suspend fun delete(id: Long)
}
