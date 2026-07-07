package com.petergabriel.budgetcalendar.features.creditcard.domain.usecase

import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType
import com.petergabriel.budgetcalendar.features.accounts.domain.repository.IAccountRepository
import com.petergabriel.budgetcalendar.features.creditcard.domain.model.CreditCardSummary
import com.petergabriel.budgetcalendar.features.creditcard.domain.repository.ICreditCardRepository
import com.petergabriel.budgetcalendar.features.transactions.domain.repository.ITransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlin.math.abs
import kotlin.math.max

@OptIn(ExperimentalCoroutinesApi::class)
class GetCreditCardSummariesUseCase(
    private val creditCardRepository: ICreditCardRepository,
    private val accountRepository: IAccountRepository,
    private val transactionRepository: ITransactionRepository,
    private val nowMillis: () -> Long = DateUtils::nowMillis,
) {
    operator fun invoke(): Flow<List<CreditCardSummary>> {
        return transactionRepository.transactionChangedTrigger
            .onStart { emit(Unit) }
            .flatMapLatest {
                creditCardRepository.getAllSettings()
                    .map { settings ->
                        val projectedBalances = creditCardRepository.getAllProjectedBalances().getOrElse { emptyMap() }
                        settings.mapNotNull { setting ->
                            val account = accountRepository.getAccountById(setting.accountId)
                            if (account == null || account.type != AccountType.CREDIT_CARD) {
                                null
                            } else {
                                val projected = projectedBalances[account.id] ?: 0L

                                val statementDue = statementDueFor(
                                    accountId = account.id,
                                    closingDay = setting.statementClosingDay,
                                    dueDay = setting.paymentDueDay,
                                )

                                CreditCardSummary(
                                    accountId = account.id,
                                    accountName = account.name,
                                    currentBalance = projected,
                                    reservedAmount = max(0L, -projected),
                                    statementBalance = setting.statementBalance,
                                    creditLimit = setting.creditLimit,
                                    availableCredit = setting.creditLimit?.minus(abs(projected)),
                                    dueDate = setting.dueDate,
                                    statementClosingDay = setting.statementClosingDay,
                                    paymentDueDay = setting.paymentDueDay,
                                    statementDueAmount = statementDue?.amount,
                                    statementDueDate = statementDue?.dueDate,
                                )
                            }
                        }.sortedBy { summary -> summary.accountName.lowercase() }
                    }
            }
    }

    private suspend fun statementDueFor(accountId: Long, closingDay: Int?, dueDay: Int?): StatementDue? {
        if (closingDay == null || dueDay == null) return null
        val lastClose = DateUtils.lastStatementClose(closingDay, nowMillis())
        val balanceAsOf = creditCardRepository.getBalanceAsOf(accountId, lastClose).getOrElse { return null }
        val paymentsSince = creditCardRepository.getPaymentsSince(accountId, lastClose).getOrElse { return null }
        return StatementDue(
            amount = max(0L, -balanceAsOf - paymentsSince),
            dueDate = DateUtils.paymentDueAfter(dueDay, lastClose),
        )
    }

    private data class StatementDue(
        val amount: Long,
        val dueDate: Long,
    )
}
