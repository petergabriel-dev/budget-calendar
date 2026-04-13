package com.petergabriel.budgetcalendar.features.accounts.domain.repository

import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account
import com.petergabriel.budgetcalendar.features.accounts.domain.model.CreateAccountRequest
import com.petergabriel.budgetcalendar.features.accounts.domain.model.UpdateAccountRequest
import kotlinx.coroutines.flow.Flow

interface IAccountRepository {
    fun getAllAccounts(): Flow<List<Account>>
    suspend fun getAccountById(id: Long): Account?
    fun getSpendingPoolAccounts(): Flow<List<Account>>
    suspend fun createAccount(request: CreateAccountRequest): Account
    suspend fun updateAccount(id: Long, request: UpdateAccountRequest): Account?
    suspend fun deleteAccount(id: Long)
    suspend fun getTotalSpendingPoolBalance(): Long
    suspend fun hasTransactionsForAccount(accountId: Long): Boolean
    suspend fun calculateNetWorth(): Long
}
