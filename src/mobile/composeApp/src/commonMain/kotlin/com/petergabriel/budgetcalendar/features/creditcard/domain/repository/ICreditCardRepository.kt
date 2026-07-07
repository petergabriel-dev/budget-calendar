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
        statementClosingDay: Int? = null,
        paymentDueDay: Int? = null,
    ): Result<CreditCardSettings>

    suspend fun update(
        accountId: Long,
        creditLimit: Long?,
        statementBalance: Long?,
        dueDate: Long?,
        statementClosingDay: Int? = null,
        paymentDueDay: Int? = null,
    ): Result<CreditCardSettings>

    suspend fun delete(accountId: Long): Result<Unit>
    suspend fun getProjectedBalance(accountId: Long): Result<Long>
    suspend fun getAllProjectedBalances(): Result<Map<Long, Long>>
}
