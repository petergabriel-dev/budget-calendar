package com.petergabriel.budgetcalendar.features.creditcard.domain.usecase

import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType
import com.petergabriel.budgetcalendar.features.accounts.domain.repository.IAccountRepository
import com.petergabriel.budgetcalendar.features.transactions.domain.model.CreateTransactionRequest
import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import com.petergabriel.budgetcalendar.features.transactions.domain.usecase.CreateTransactionUseCase

class MakeCreditCardPaymentUseCase(
    private val accountRepository: IAccountRepository,
    private val createTransactionUseCase: CreateTransactionUseCase,
) {
    suspend operator fun invoke(
        sourceAccountId: Long,
        ccAccountId: Long,
        amount: Long,
        date: Long = DateUtils.startOfDayMillis(DateUtils.nowMillis()),
    ): Result<Transaction> {
        if (amount <= 0L) {
            return Result.failure(IllegalArgumentException("Payment amount must be greater than 0"))
        }
        if (sourceAccountId == ccAccountId) {
            return Result.failure(IllegalArgumentException("Source and destination accounts must be different"))
        }

        val sourceAccount = accountRepository.getAccountById(sourceAccountId)
            ?: return Result.failure(NoSuchElementException("Account with id=$sourceAccountId was not found"))
        val destinationAccount = accountRepository.getAccountById(ccAccountId)
            ?: return Result.failure(NoSuchElementException("Account with id=$ccAccountId was not found"))
        if (destinationAccount.type != AccountType.CREDIT_CARD) {
            return Result.failure(IllegalArgumentException("Destination must be a credit card account"))
        }

        return createTransactionUseCase(
            CreateTransactionRequest(
                accountId = sourceAccount.id,
                destinationAccountId = destinationAccount.id,
                amount = amount,
                date = date,
                type = TransactionType.TRANSFER,
                description = "Credit card payment",
            ),
        )
    }
}
