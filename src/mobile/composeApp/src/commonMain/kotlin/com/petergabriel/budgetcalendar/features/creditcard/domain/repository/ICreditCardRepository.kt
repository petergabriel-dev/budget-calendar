package com.petergabriel.budgetcalendar.features.creditcard.domain.repository

import com.petergabriel.budgetcalendar.features.creditcard.domain.model.CreditCardSettings
import kotlinx.coroutines.flow.Flow

interface ICreditCardRepository {
    fun getAllSettings(): Flow<List<CreditCardSettings>>
    suspend fun getSettingsByAccountId(accountId: Long): Result<CreditCardSettings>
    suspend fun insert(
        accountId: Long,
        creditLimit: Long?,
        statementBalance: Long?,
        dueDate: Long?,
    ): Result<CreditCardSettings>

    suspend fun update(
        accountId: Long,
        creditLimit: Long?,
        statementBalance: Long?,
        dueDate: Long?,
    ): Result<CreditCardSettings>

    suspend fun delete(accountId: Long): Result<Unit>
    suspend fun getReservedAmount(accountId: Long): Result<Long>
    suspend fun getAllReservedAmounts(): Result<Map<Long, Long>>
}
