package com.petergabriel.budgetcalendar.features.creditcard.domain.usecase

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

                                CreditCardSummary(
                                    accountId = account.id,
                                    accountName = account.name,
                                    currentBalance = projected,
                                    reservedAmount = max(0L, -projected),
                                    statementBalance = setting.statementBalance,
                                    creditLimit = setting.creditLimit,
                                    availableCredit = setting.creditLimit?.minus(abs(projected)),
                                    dueDate = setting.dueDate,
                                )
                            }
                        }.sortedBy { summary -> summary.accountName.lowercase() }
                    }
            }
    }
}
