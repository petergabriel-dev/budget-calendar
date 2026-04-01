package com.petergabriel.budgetcalendar.features.accounts.domain.repository

import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account
import com.petergabriel.budgetcalendar.features.accounts.domain.model.CreateAccountRequest
import com.petergabriel.budgetcalendar.features.accounts.domain.model.UpdateAccountRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface IAccountRepository {
    fun getAllAccounts(): Flow<List<Account>>
    suspend fun getAccountById(id: Long): Account?
    fun getSpendingPoolAccounts(): Flow<List<Account>>

    /**
     * Triggered after any mutation that may change account data.
     * Use this to re-query getSpendingPoolAccounts() after adjustBalance, createAccount, updateAccount, etc.
     */
    val balanceChangedTrigger: SharedFlow<Unit>
    suspend fun createAccount(request: CreateAccountRequest): Account
    suspend fun updateAccount(id: Long, request: UpdateAccountRequest): Account?
    suspend fun deleteAccount(id: Long)
    suspend fun getTotalSpendingPoolBalance(): Long
    suspend fun hasTransactionsForAccount(accountId: Long): Boolean
    suspend fun calculateNetWorth(): Long
    suspend fun adjustBalance(accountId: Long, delta: Long)
}
