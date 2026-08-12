package com.petergabriel.budgetcalendar.features.creditcard.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.petergabriel.budgetcalendar.core.database.BudgetCalendarDatabase
import com.petergabriel.budgetcalendar.features.accounts.data.mapper.AccountMapper
import com.petergabriel.budgetcalendar.features.accounts.data.repository.AccountRepositoryImpl
import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType
import com.petergabriel.budgetcalendar.features.accounts.domain.model.CreateAccountRequest
import com.petergabriel.budgetcalendar.features.creditcard.data.mapper.CreditCardMapper
import com.petergabriel.budgetcalendar.features.transactions.data.mapper.TransactionMapper
import com.petergabriel.budgetcalendar.features.transactions.data.repository.TransactionRepositoryImpl
import com.petergabriel.budgetcalendar.features.transactions.domain.model.CreateTransactionRequest
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class CreditCardRepositoryImplTest {
    @Test
    fun getBalanceAsOfAndPaymentsSince_useDateBoundsAndIgnoreCancelledRows() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        BudgetCalendarDatabase.Schema.create(driver)
        val database = BudgetCalendarDatabase(driver)
        val transactionRepository = TransactionRepositoryImpl(database, TransactionMapper())
        val creditCardRepository = CreditCardRepositoryImpl(database, CreditCardMapper())
        val accountRepository = AccountRepositoryImpl(
            database,
            AccountMapper(),
            transactionRepository,
            creditCardRepository,
        )

        try {
            val card = accountRepository.createAccount(
                CreateAccountRequest(
                    name = "Card",
                    type = AccountType.CREDIT_CARD,
                    initialBalance = -10_000L,
                ),
            )
            val sinceMillis = 1_000_150L
            val asOfMillis = 1_000_300L

            transactionRepository.createTransaction(
                transaction(card.id, amount = 3_000L, signedAmount = -3_000L, date = 1_000_100L),
            )
            transactionRepository.createTransaction(
                transaction(card.id, amount = 1_000L, signedAmount = 1_000L, date = 1_000_200L),
            )
            transactionRepository.createTransaction(
                transaction(
                    card.id,
                    amount = 9_999L,
                    signedAmount = -9_999L,
                    date = 1_000_250L,
                    status = TransactionStatus.CANCELLED,
                ),
            )
            transactionRepository.createTransaction(
                transaction(card.id, amount = 4_000L, signedAmount = -4_000L, date = asOfMillis),
            )
            transactionRepository.createTransaction(
                transaction(card.id, amount = 2_000L, signedAmount = 2_000L, date = 1_000_400L),
            )

            assertEquals(-12_000L, creditCardRepository.getBalanceAsOf(card.id, asOfMillis).getOrThrow())
            assertEquals(3_000L, creditCardRepository.getPaymentsSince(card.id, sinceMillis).getOrThrow())
        } finally {
            driver.close()
        }
    }

    private fun transaction(
        accountId: Long,
        amount: Long,
        signedAmount: Long,
        date: Long,
        status: TransactionStatus = TransactionStatus.CONFIRMED,
    ): CreateTransactionRequest {
        return CreateTransactionRequest(
            accountId = accountId,
            amount = amount,
            date = date,
            type = if (signedAmount > 0) TransactionType.TRANSFER else TransactionType.EXPENSE,
            status = status,
            description = "test row",
            signedAmount = signedAmount,
        )
    }
}
