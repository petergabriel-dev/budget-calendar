package com.petergabriel.budgetcalendar.features.creditcard.domain.usecase

import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType
import com.petergabriel.budgetcalendar.features.accounts.domain.repository.IAccountRepository
import com.petergabriel.budgetcalendar.features.creditcard.domain.model.CreditCardSummary
import com.petergabriel.budgetcalendar.features.creditcard.domain.repository.ICreditCardRepository
import kotlin.math.abs
import kotlin.math.max

class GetCreditCardSummaryByIdUseCase(
    private val getCreditCardSettingsUseCase: GetCreditCardSettingsUseCase,
    private val accountRepository: IAccountRepository,
    private val creditCardRepository: ICreditCardRepository,
    private val nowMillis: () -> Long = DateUtils::nowMillis,
) {
    suspend operator fun invoke(accountId: Long): Result<CreditCardSummary> {
        val settings = getCreditCardSettingsUseCase(accountId).getOrElse { throwable ->
            return Result.failure(throwable)
        }

        val account = accountRepository.getAccountById(accountId)
            ?: return Result.failure(NoSuchElementException("Account with id=$accountId was not found"))
        if (account.type != AccountType.CREDIT_CARD) {
            return Result.failure(IllegalArgumentException("Account with id=$accountId is not a credit card"))
        }

        val projected = creditCardRepository.getProjectedBalance(accountId).getOrElse { throwable ->
            return Result.failure(throwable)
        }

        val statementDue = statementDueFor(accountId, settings.statementClosingDay, settings.paymentDueDay).getOrElse { throwable ->
            return Result.failure(throwable)
        }

        return Result.success(
            CreditCardSummary(
                accountId = account.id,
                accountName = account.name,
                currentBalance = projected,
                reservedAmount = max(0L, -projected),
                statementBalance = settings.statementBalance,
                creditLimit = settings.creditLimit,
                availableCredit = settings.creditLimit?.minus(abs(projected)),
                dueDate = settings.dueDate,
                statementClosingDay = settings.statementClosingDay,
                paymentDueDay = settings.paymentDueDay,
                statementDueAmount = statementDue?.amount,
                statementDueDate = statementDue?.dueDate,
            ),
        )
    }

    private suspend fun statementDueFor(accountId: Long, closingDay: Int?, dueDay: Int?): Result<StatementDue?> = runCatching {
        if (closingDay == null || dueDay == null) return@runCatching null
        val lastClose = DateUtils.lastStatementClose(closingDay, nowMillis())
        val balanceAsOf = creditCardRepository.getBalanceAsOf(accountId, lastClose).getOrThrow()
        val paymentsSince = creditCardRepository.getPaymentsSince(accountId, lastClose).getOrThrow()
        StatementDue(
            amount = max(0L, -balanceAsOf - paymentsSince),
            dueDate = DateUtils.paymentDueAfter(dueDay, lastClose),
        )
    }

    private data class StatementDue(
        val amount: Long,
        val dueDate: Long,
    )
}
